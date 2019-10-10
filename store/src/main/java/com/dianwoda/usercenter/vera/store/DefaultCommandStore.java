package com.dianwoda.usercenter.vera.store;


import com.dianwoda.usercenter.vera.common.CountDownLatch2;
import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.ThreadFactoryImpl;
import com.dianwoda.usercenter.vera.common.UtilAll;
import com.dianwoda.usercenter.vera.common.message.CommandDecoder;
import com.dianwoda.usercenter.vera.common.message.GetCommandStatus;
import com.dianwoda.usercenter.vera.common.protocol.Common;
import com.dianwoda.usercenter.vera.store.config.PiperPathConfigHelper;
import com.dianwoda.usercenter.vera.store.listener.CommandArrivingListener;
import com.dianwoda.usercenter.vera.store.stats.PiperStatsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * command 存储操作
 * @author seam
 */
public class DefaultCommandStore implements CommandStore {
  protected static final Logger log = LoggerFactory.getLogger(DefaultCommandStore.class);
  public static int MAX_SIZE = 1024 * 1024 * 4;
  public static int blockFileSize = 1024 * 1024 * 20; // 20M
  // Resource reclaim interval
  public static int cleanResourceInterval = 10000;
  public static int reserveResourceInterval = 1000 * 60 * 60 * 24 * 3; // 3天
  private String deleteWhen = "04";   // 4 am begin delete file
  private int diskMaxUsedSpaceRatio = 75;
  private int diskSpaceWarningLevelRatio = 50;

  private PutCommandLock putCommandLock;
  private BlockFileQueue blockFileQueue;
  private final AppendCommandCallBack callBack;
  private StoreCheckpoint storeCheckpoint;
  private String storePath;
  private FlushRealTimeService flushRealTimeService;
  private ClearStoreFileService clearStoreFileService;
  private AtomicLong logicOffsetMax = new AtomicLong(0);  // logic offset
  private PiperStatsManager piperStatsManager;
  private CommandArrivingListener commandArrivingListener;
  private final ScheduledExecutorService scheduledExecutorService =
          Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("StoreScheduledThread_"));

  public DefaultCommandStore(String storePath, PiperStatsManager piperStatsManager,
                             CommandArrivingListener commandArrivingListener) {
    putCommandLock = new PutCommandSpinLock();
    this.blockFileQueue = new BlockFileQueue(storePath, blockFileSize);
    this.storePath = storePath;
    this.flushRealTimeService = new FlushRealTimeService();
    this.callBack = new DefaultAppendCommandCallBack(this);
    this.piperStatsManager = piperStatsManager;
    this.commandArrivingListener = commandArrivingListener;
  }

  @Override
  public boolean load() {
    boolean result = true;
    try {
      boolean lastExitOK = !this.isTempFileExist();
      log.info("last shutdown {}", lastExitOK ? "normally" : "abnormally");

      result = result && this.blockFileQueue.load();
      if (result) {
        this.storeCheckpoint = new StoreCheckpoint(PiperPathConfigHelper.getStoreCheckpoint(this.storePath));

        if (lastExitOK) {
          recoverNormally();
        } else {
          recoverAbnormally();
        }
      }
    } catch (Exception e) {
      log.error("load exceptoin", e);
      result = false;
    }
    return result;
  }

  /**
   * 正常退出恢复
   */
  private void recoverNormally() {
    final List<BlockFile> blockFiles = this.blockFileQueue.getBlockFiles();
    if (!blockFiles.isEmpty()) {
      int index = blockFiles.size() - 3;
      if (index < 0) {
        index = 0;
      }
      BlockFile blockFile = blockFiles.get(index);
      ByteBuffer byteBuffer = blockFile.sliceByteBuffer();
      long processOffset = blockFile.getFileFromOffset();
      long blockFileOffset = 0;
      log.info("recover physics file, " + blockFile.getFileName());
      while (true) {
        byteBuffer.position((int)blockFileOffset);
        DispatchRequest request = checkMessageAndReturnSize(byteBuffer, true, true);
        if (request.isSuccess() && request.getTotalSize() > 0) {
          blockFileOffset += request.getTotalSize();
          this.logicOffsetMax.set(Math.max(this.logicOffsetMax.get(), request.getLogicOffset()));

        } else if (request.isSuccess() && request.getTotalSize() == 0) {
          index++;
          if (index >= blockFiles.size()) {
            log.info("recover last 3 physics file over, last block file " + blockFile.getFileName());
            break;
          } else {
            blockFile = blockFiles.get(index);
            byteBuffer = blockFile.sliceByteBuffer();
            processOffset = blockFile.getFileFromOffset();
            blockFileOffset = 0;
            log.info("recover next physics file, " + blockFile.getFileName());
          }
        } else if (!request.isSuccess()){ // Intermediate file read error
          log.info("recover physics file end, " + blockFile.getFileName());
          break;
        }
      }
      processOffset += blockFileOffset;
      this.blockFileQueue.setFlushedWhere(processOffset);
      this.blockFileQueue.truncateDirtyFiles(processOffset);
      log.info("recover last blockFile:" + (blockFile == null ? null : blockFile.getFileName()) + " processoffset:" + processOffset);
    }
  }

  /**
   * 异常退出恢复
   */
  private void recoverAbnormally() {
    final List<BlockFile> blockFileList = this.blockFileQueue.getBlockFiles();
    if (!blockFileList.isEmpty()) {
      int index = blockFileList.size() - 1;
      BlockFile blockFile = null;
      for (; index >=0; index--) {
        blockFile = blockFileList.get(index);
        if (isBlockFileMatchedRecover(blockFile)) {
          log.info("recover from this block file " + blockFile.getFileName());
          break;
        }
      }
      if (index < 0) {
        index = 0;
        blockFile = blockFileList.get(index);
      }
      ByteBuffer byteBuffer = blockFile.sliceByteBuffer();
      long processOffset = blockFile.getFileFromOffset();
      long blockFileOffset = 0;
      while (true) {
        DispatchRequest dispatchRequest = this.checkMessageAndReturnSize(byteBuffer, true, true);
        int size = dispatchRequest.getTotalSize();
        if (size > 0) {
          blockFileOffset += size;
        } else if (size < 0) {
          log.info("recover block file end, " + blockFile.getFileName());
          break;
        } else if (size == 0) {
          index++;
          if (index >= blockFileList.size()) {
            log.info("recover block file over, last block file " + blockFile.getFileName());
            break;
          } else {
            blockFile = blockFileList.get(index);
            byteBuffer = blockFile.sliceByteBuffer();
            processOffset = blockFile.getFileFromOffset();
            blockFileOffset = 0;
            log.info("recover next block file, " + blockFile.getFileName());
          }
        }
      }

      processOffset += blockFileOffset;
      this.blockFileQueue.setFlushedWhere(processOffset);
      this.blockFileQueue.truncateDirtyFiles(processOffset);
    } else {
      this.blockFileQueue.setFlushedWhere(0);
    }
  }

  private boolean isBlockFileMatchedRecover(BlockFile blockFile) {
    ByteBuffer byteBuffer = blockFile.sliceByteBuffer();
    int magicCode = byteBuffer.getInt(CommandDecoder.MAGIC_CODE_POSITION);
    if (magicCode != Common.MESSAGE_MAGIC_CODE) {
      return false;
    }

    long storeTimestamp = byteBuffer.getLong(CommandDecoder.STORE_TIMESTAMP_POSITION);
    if (storeTimestamp <= 0) {
      return false;
    }

    if (this.storeCheckpoint.getPhysicMsgTimestamp() == 0 ||
            this.storeCheckpoint.getPhysicMsgTimestamp() > storeTimestamp) {
      return true;
    }
    return false;
  }

  private DispatchRequest checkMessageAndReturnSize(ByteBuffer byteBuffer, final boolean checkCRC,
                                                       final boolean readBody) {
    int pos = byteBuffer.position();
    try {
      // 1 total size
      int totalSize = byteBuffer.getInt();
      if (totalSize < 0) {
        return new DispatchRequest(0, false);
      }
      // 2 magic code
      int magicCode = byteBuffer.getInt();
      switch (magicCode) {
        case Common.MESSAGE_MAGIC_CODE:
          break;
        case Common.BLANK_MAGIC_CODE:
          return new DispatchRequest(0, true);
        default:
          log.warn("found a illegal magic code 0x" + Integer.toHexString(magicCode) + " with pos:" + pos);
          return new DispatchRequest(0, false);
      }
      // 3 crc
      int crc = byteBuffer.getInt();
      // 4 store timestamp
      long storeTimestamp = byteBuffer.getLong();
      // 5 store offset
      long storeOffset = byteBuffer.getLong();
      // 6 logic offset
      long logicOffset = byteBuffer.getLong();
      DispatchRequest dispatchRequest = new DispatchRequest(totalSize,  true);
      dispatchRequest.setLogicOffset(logicOffset);
      // 7 data
      if (readBody) {
        // 7 data length
        int dataLen = byteBuffer.getInt();
        byte[] data = new byte[dataLen];
        byteBuffer.get(data, 0, dataLen);
        if (checkCRC) {
          int crc2 = UtilAll.crc32(data, 0, dataLen);
          if (crc != crc2) {
            log.error("CRC check failed. bodyCRC={}, currentCRC={}", crc, crc2);
            return new DispatchRequest(0, false);
          }
        }
        return dispatchRequest;
      }
      return dispatchRequest;
    } catch (Exception e) {
      log.error("CRC check failed. pos=" + pos, e);
      return new DispatchRequest(-1, false);

    }
  }

  @Override
  public PutCommandResult appendCommand(byte[] data) {
    BlockFile blockFile = this.blockFileQueue.getLastBlockFile();
    AppendCommandResult result = null;
    putCommandLock.lock();

    try {
      if (blockFile == null || blockFile.isFull()) {
        blockFile = this.blockFileQueue.getLastBlockFile(0, true);
      }
      if (blockFile == null) {
        log.error("create block file error, data: {}", data);
        return new PutCommandResult(PutCommandStatus.CREATE_BLOCKFILE_FAILED, null);
      }
      result = blockFile.appendCommand(data, this.callBack);
      switch (result.getStatus()) {
        case PUT_OK:
          this.commandArrivingListener.commandArriveNotify(result.getWroteOffset(), result.getWroteBytes());
          this.piperStatsManager.incPiperPutNums(1);
          this.piperStatsManager.incPiperPutSize(result.getWroteBytes());
          break;
        case END_OF_FILE:
          blockFile = this.blockFileQueue.getLastBlockFile(0, true);
          if (blockFile == null) {
            log.error("create block file2 error, data:{}", data);
            return new PutCommandResult(PutCommandStatus.CREATE_BLOCKFILE_FAILED, result);
          }
          result = blockFile.appendCommand(data, this.callBack);
          break;
        case UNKNOWN_ERROR:
          return new PutCommandResult(PutCommandStatus.UNKNOWN_ERROR, result);
        default:
          return new PutCommandResult(PutCommandStatus.UNKNOWN_ERROR, result);
      }

    } finally {
      putCommandLock.unlock();
    }

    PutCommandResult putCommandResult = new PutCommandResult(PutCommandStatus.PUT_OK, result);
    flushRealTimeService.wakeup();
    return putCommandResult;
  }

  @Deprecated
  public GetCommandResult getCommandOld(String targetLocation, long offset, int msgNums) {
    GetCommandStatus status = GetCommandStatus.NO_MESSAGE_IN_BLOCK;
    GetCommandResult result = new GetCommandResult();
    long nextBeginOffset = offset;

    final long maxOffsetPy = blockFileQueue.getMaxOffset();
    final long minOffsetPy = blockFileQueue.getMinOffset();
    if (maxOffsetPy == 0) {
      status = GetCommandStatus.NO_MESSAGE_IN_BLOCK;
      nextBeginOffset = 0;
    } else if (offset < minOffsetPy) {
      status = GetCommandStatus.OFFSET_TOO_SMALL;
      nextBeginOffset = minOffsetPy;
    } else if (offset == maxOffsetPy) {
      status = GetCommandStatus.OFFSET_OVERFLOW_ONE;
      nextBeginOffset = offset;
    } else if (offset > maxOffsetPy) {
      status = GetCommandStatus.OFFSET_OVERFLOW_BADLY;
      if (0 == minOffsetPy) {
        nextBeginOffset = minOffsetPy;
      } else {
        nextBeginOffset = maxOffsetPy;
      }
    } else {
      //
      long newOffset = offset;
      for(int i=0; i<msgNums; i++) {
        BlockFile blockFile = blockFileQueue.findBlockFileByOffset(newOffset);
        SelectMappedBufferResult selectMappedBufferResult = blockFile == null ? null : blockFile.queryCommand(newOffset);
        if(selectMappedBufferResult != null) {
          try {
            status = GetCommandStatus.FOUND;
            int totalSize = selectMappedBufferResult.getSize();
            selectMappedBufferResult.getByteBuffer().limit(totalSize);
            result.addCommand(selectMappedBufferResult);
            newOffset += selectMappedBufferResult.getSize();
            nextBeginOffset = newOffset;
            log.info("oldOffset:" + offset + " nextBeginOffset:" + nextBeginOffset + " status:" + status.name());
          } finally {
            selectMappedBufferResult.release();
          }
        } else {
          if (result.getBufferTotalSize() == 0) {
            status = GetCommandStatus.OFFSET_FOUND_NULL;
            nextBeginOffset = rollNextFile(newOffset);
          }
          break;
        }
      }
      // maxOffsetPy maybe less than nextBeginOffset, need fix
      long fallBehind = maxOffsetPy - nextBeginOffset;
      this.piperStatsManager.recordDiskFallBehindSize(targetLocation, fallBehind);

    }
    result.setNextBeginOffset(nextBeginOffset);
    result.setMinOffset(minOffsetPy);
    result.setMaxOffset(maxOffsetPy);
    result.setStatus(status);
    return result;
  }

  @Override
  public GetCommandResult getCommand(String targetLocation, long offset, int msgNums) {
    GetCommandStatus status = GetCommandStatus.NO_MESSAGE_IN_BLOCK;
    GetCommandResult result = new GetCommandResult();
    long nextBeginOffset = offset;

    final long maxOffsetPy = blockFileQueue.getMaxOffset();
    final long minOffsetPy = blockFileQueue.getMinOffset();
    if (maxOffsetPy == 0) {
      status = GetCommandStatus.NO_MESSAGE_IN_BLOCK;
      nextBeginOffset = 0;
    } else if (offset < minOffsetPy) {
      status = GetCommandStatus.OFFSET_TOO_SMALL;
      nextBeginOffset = minOffsetPy;
    } else if (offset == maxOffsetPy) {
      status = GetCommandStatus.OFFSET_OVERFLOW_ONE;
      nextBeginOffset = offset;
    } else if (offset > maxOffsetPy) {
      status = GetCommandStatus.OFFSET_OVERFLOW_BADLY;
      if (0 == minOffsetPy) {
        nextBeginOffset = minOffsetPy;
      } else {
        nextBeginOffset = maxOffsetPy;
      }
    } else {
      //
      BlockFile blockFile = blockFileQueue.findBlockFileByOffset(nextBeginOffset);
      SelectMappedBufferResult selectMappedBufferResult = blockFile.queryCommand(nextBeginOffset, msgNums);
      if(selectMappedBufferResult != null) {
        try {
          status = GetCommandStatus.FOUND;
          int totalSize = selectMappedBufferResult.getSize();
          selectMappedBufferResult.getByteBuffer().limit(totalSize);
          result.addCommand(selectMappedBufferResult);
          nextBeginOffset += totalSize;
          log.info("oldOffset:" + offset + " nextBeginOffset:" + nextBeginOffset + " status:" + status.name());
        } finally {
          selectMappedBufferResult.release();
        }
      } else {
        // 加载下一个block file
        if (result.getBufferTotalSize() == 0) {
          status = GetCommandStatus.OFFSET_FOUND_NULL;
          nextBeginOffset = rollNextFile(nextBeginOffset);
        }
      }
      // maxOffsetPy maybe less than nextBeginOffset, need fix
      long fallBehind = maxOffsetPy - nextBeginOffset;
      this.piperStatsManager.recordDiskFallBehindSize(targetLocation, fallBehind);
    }
    result.setNextBeginOffset(nextBeginOffset);
    result.setMinOffset(minOffsetPy);
    result.setMaxOffset(maxOffsetPy);
    result.setStatus(status);
    return result;
  }

  public long rollNextFile(final long offset) {
    return blockFileSize + offset - offset % blockFileSize;
  }

  // bytebuffer position start 0
  private byte[] transfer(ByteBuffer byteBuffer) {
    int limit = byteBuffer.limit();
    if (limit <= 0) {
      return null;
    }
    ByteBuffer byteBufferNew = ByteBuffer.allocate(limit);
    for (int i=0; i<limit; i++) {
      byteBufferNew.put(byteBuffer.get(i));
    }
    return byteBufferNew.array();
  }

  @Override
  public long getMaxWriteOffset() {
    return this.blockFileQueue.getMaxOffset();
  }

  private boolean isTempFileExist() {
    String fileName = PiperPathConfigHelper.getAbortFile(this.storePath);
    File file = new File(fileName);
    return file.exists();
  }

  private void createTempFile() throws IOException {
    String fileName = PiperPathConfigHelper.getAbortFile(this.storePath);
    File file = new File(fileName);
    BlockFile.ensureDirOK(file.getParent());
    boolean result = file.createNewFile();
    log.info(fileName + (result ? " create OK" : " already exists!"));
  }

  private void deleteFile(final String fileName) {
    File file = new File(fileName);
    boolean result = file.delete();
    log.info(fileName + (result ? " delete OK" : " delete Failed"));
  }

  @Override
  public void start() {
    try {
      this.createTempFile();
    } catch (IOException e) {
      log.error("create file error", e);
    }

    this.addScheduleTask();
  }

  private void addScheduleTask() {

    new ThreadFactoryImpl("flushBlockFileThread_").
            newThread(this.flushRealTimeService).start();

    this.scheduledExecutorService.scheduleAtFixedRate(() -> {
      DefaultCommandStore.this.cleanFiles();
    }, 1000 * 60, DefaultCommandStore.cleanResourceInterval, TimeUnit.MILLISECONDS);
  }

  @Override
  public void stop() {
    this.flushRealTimeService.setStop(true);
    this.deleteFile(PiperPathConfigHelper.getAbortFile(this.storePath));
    this.storeCheckpoint.shutdown();
  }

  public void cleanFiles() {
    clearStoreFileService.run();
  }


  class FlushRealTimeService implements Runnable {
    private boolean isStop = false;
    private int FLUSH_SPAN = 1000 * 10;
    private int FORCE_FLUSH_SPAN = 1000 * 30;
    private CountDownLatch2 waitPoint = new CountDownLatch2(1);
    private AtomicBoolean hasNotified = new AtomicBoolean(false);

    public void setStop(boolean stop) {
      this.isStop = stop;
    }

    public boolean isStop() {
      return isStop;
    }

    private boolean waitForRunning(int interval) {
      if (hasNotified.compareAndSet(true, false)) {
        return true;
      }
      waitPoint.reset();
      try {
        waitPoint.await(interval, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return false;
    }

    public void wakeup() {
      if (hasNotified.compareAndSet(false, true)) {
        waitPoint.countDown();
      }
    }

    @Override
    public void run() {
      long startFlushTime = SystemClock.now();
      int interval = 1000 * 5;
      while (!this.isStop()) {
        boolean canFlush = false;
        boolean forceFlush = false;
        if (SystemClock.now() - startFlushTime >= FLUSH_SPAN) {
          canFlush = true;
        }
        if (SystemClock.now() - startFlushTime >= FORCE_FLUSH_SPAN) {
          forceFlush = true;
        }
        if (forceFlush || (canFlush && !waitForRunning(interval))) {
          blockFileQueue.flush(0);
          startFlushTime = SystemClock.now();

          long storeTimestamp = DefaultCommandStore.this.blockFileQueue.getStoreTimestamp();
          if (storeTimestamp > 0) {
            DefaultCommandStore.this.storeCheckpoint.setPhysicMsgTimestamp(storeTimestamp);
          }
        }
      }
    }
  }

  class ClearStoreFileService {

    public void run() {
      int reserveTime = DefaultCommandStore.reserveResourceInterval;
      int deleteFilesInterval = 1000 * 6;
      long intervalForcibly = 1000 * 120;
      boolean isTimeMeet = this.isTimeMeet();
      boolean isSpaceMeet = this.isSpaceMeet();

      if (isTimeMeet || isSpaceMeet) {

        log.info(String.format("begin to delete before %d hours file. timeup: %s spacefull: %s",
                reserveTime / 1000 / 600, isTimeMeet, isSpaceMeet));

        int deleteCount = DefaultCommandStore.this.blockFileQueue.deleteExpiredFileByTime(reserveTime,
                deleteFilesInterval, intervalForcibly);
        log.info("ClearStoreFileService run clean file deleteCount:" + deleteCount);
      }

    }

    private boolean isTimeMeet() {
      String when = DefaultCommandStore.this.deleteWhen;
      if (UtilAll.isItTimeToDo(when)) {
        DefaultCommandStore.log.info("it's time to reclaim disk space, " + when);
        return true;
      }
      return false;
    }

    private boolean isSpaceMeet() {
      double maxRatio = DefaultCommandStore.this.diskMaxUsedSpaceRatio / 100.0;
      String storePathPhysic = DefaultCommandStore.this.storePath;

      double usedRatio = UtilAll.getDiskPartitionSpaceUsedPercent(storePathPhysic);
      if (usedRatio >= maxRatio) {
        return true;

      } else if (usedRatio >= diskSpaceWarningLevelRatio) {
        DefaultCommandStore.log.error("disk space beyond warning level!");
      }
      return false;
    }


  }

  @Override
  public AtomicLong getLogicOffsetMax() {
    return this.logicOffsetMax;
  }
}
