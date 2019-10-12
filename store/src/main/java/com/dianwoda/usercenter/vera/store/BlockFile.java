package com.dianwoda.usercenter.vera.store;

import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.UtilAll;
import com.dianwoda.usercenter.vera.common.protocol.Common;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * command 存储文件
 * @author seam
 */
public class BlockFile extends ReferenceResource {
  protected static final Logger log = LoggerFactory.getLogger(BlockFile.class);
  public static final int OS_PAGE_SIZE = 1024 * 4;

  private int fileSize;
  private FileChannel fileChannel;
  private final AtomicInteger wrotePosition = new AtomicInteger(0);
  private final AtomicInteger flushedPosition = new AtomicInteger(0);
  private long fileFromOffset;
  private File file;
  private MappedByteBuffer mappedByteBuffer;
  private String fileName;
  protected volatile boolean cleanupOver = false;
  private volatile long storeTimestamp = 0;

  public BlockFile(final String fileName, final int fileSize) throws IOException {
    init(fileName, fileSize);
  }

  public static void ensureDirOK(final String dirName) {
    if (dirName != null) {
      File f = new File(dirName);
      if (!f.exists()) {
        boolean result = f.mkdirs();
        log.info(dirName + " mkdir " + (result ? "OK" : "Failed"));
      }
    }
  }

  private void init(final String fileName, final int fileSize) throws IOException {
    this.fileName = fileName;
    this.fileSize = fileSize;

    this.file = new File(fileName);
    this.fileFromOffset = Long.parseLong(this.file.getName());

    boolean ok = false;

    ensureDirOK(this.file.getParent());
    try {
      this.fileChannel = new RandomAccessFile(this.file, "rw").getChannel();
      this.mappedByteBuffer = this.fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, this.fileSize);
      ok = true;
    } catch (FileNotFoundException e) {
      log.error("create block file channel " + this.fileName + " Failed. ", e);
      throw e;
    } finally {
      if (!ok && this.fileChannel != null) {
        this.fileChannel.close();
      }
    }
  }

  public AppendCommandResult appendCommand(final byte[] data, AppendCommandCallBack cb) {
    assert data != null;
    // construct command inter
    CommandInter commandInter = new CommandInter();

    // 1 store timestamp
    commandInter.setStoreTimestamp(SystemClock.now());
    // 2 crc
    ByteBuffer buffer = ByteBuffer.allocate(data.length);
    buffer.put(data);
    commandInter.setCrc(UtilAll.crc32(buffer.array()));
    // 3 data
    commandInter.setData(data);

    return appendCommandInner(commandInter, cb);
  }

  public AppendCommandResult appendCommandInner(final CommandInter commandInter, AppendCommandCallBack cb) {
    int currentPos = this.wrotePosition.get();

    try {
      ByteBuffer byteBuffer = this.mappedByteBuffer.slice();
      byteBuffer.position(currentPos);
      AppendCommandResult result = null;
      result = cb.doAppend(this.getFileFromOffset(), byteBuffer, this.fileSize - currentPos, commandInter);
      this.wrotePosition.addAndGet(result.getWroteBytes());
      this.storeTimestamp = result.getStoreTimestamp();
      return result;
    } catch (Throwable e) {
      log.error("Error occurred when append message to blockFile.", e);
      return new AppendCommandResult(AppendCommandStatus.UNKNOWN_ERROR);
    }
  }

  public SelectMappedBufferResult queryCommand(long offset) {
    int readPosition = getReadPosition();
    if (offset >= getFileFromOffset() + readPosition) {
      return null;
    } else if (offset > getFileFromOffset() + this.fileSize) {
      return null;
    }
    int totalSize = 0;
    long newOffset = offset % this.fileSize;
    try {

      ByteBuffer byteBuffer = this.mappedByteBuffer.slice();
      byteBuffer.position((int) newOffset);
      ByteBuffer transferBuffer = byteBuffer.slice();
      // 1 total size
      totalSize = byteBuffer.getInt();
      // 2 magic code
      int magicCode = byteBuffer.getInt();
      if (magicCode != Common.MESSAGE_MAGIC_CODE) {
        return null;
      }
      // 3 crc
      int crc = byteBuffer.getInt();
      // 4 store timestamp
      long storeTimestamp = byteBuffer.getLong();
      // 5 logic offset
      long logicOffset = byteBuffer.getLong();
      // 6 store offset
      long storeOffset = byteBuffer.getLong();
      // 7 data for test
      // 7 data length
      int dataLength = byteBuffer.getInt();
      ByteBuffer dataBuffer = ByteBuffer.allocate(dataLength);
      for (int i = 0; i < dataLength; i++) {
        dataBuffer.put(byteBuffer.get(i));
      }
      // debug
      String data = new String(dataBuffer.array());
      log.info("file name:" + this.getFileName() + " Content:" +
              data.substring(0, Math.min(data.length(), 100)) + " offset:" + offset +
              " datalen:" + dataLength + " totalSize:" + totalSize + " canreadposition:" + readPosition);
      // data for test end
      if (this.hold()) {
        return new SelectMappedBufferResult(transferBuffer, offset, totalSize, this);
      } else {
        log.warn("matched, but hold failed, request pos: " + offset + ", fileFromOffset: "
                + this.fileFromOffset);
      }

    } catch (Exception e){
      log.error(String.format("Error occurred when Read offset in blockfile, name:%s, offset:%d, size:%d, canreadpoint:%d",
              this.fileName, offset, totalSize, readPosition), e);
    }
    return null;
  }

  public SelectMappedBufferResult queryCommand(long offset, int num) {

    int relativeOffset = (int) (offset % this.fileSize);
    ByteBuffer operBuffer = this.mappedByteBuffer.slice();
    operBuffer.position(relativeOffset);
    ByteBuffer dataBuffer = operBuffer.slice();

    int sumSize = 0;
    do {
      int readPosition = getReadPosition();
      if (relativeOffset >= readPosition) {
        break;
      } else if (relativeOffset > this.fileSize) {
        break;
      }

      // 1 total size
      int totalSize = operBuffer.getInt();
      // 2 magic code
      int magicCode = operBuffer.getInt();
      if (magicCode != Common.MESSAGE_MAGIC_CODE) {
        break;
      }
      // 3 crc
      int crc = operBuffer.getInt();
      // 4 store timestamp
      long storeTimestamp = operBuffer.getLong();
      // 5 logic offset
      long logicOffset = operBuffer.getLong();
      // 6 store offset
      long storeOffset = operBuffer.getLong();
      // 7 data for test
      // 7 data length
      int dataLength = operBuffer.getInt();
      ByteBuffer tempBuffer = ByteBuffer.allocate(dataLength);
      for (int i = 0; i < dataLength; i++) {
        tempBuffer.put(operBuffer.get(i));
      }
      // 8 data
      operBuffer.position(operBuffer.position() + dataLength);
      // debug
      String data = new String(tempBuffer.array());

      log.info("file name:" + this.getFileName() + " Content:" +
              data.substring(0, Math.min(data.length(), 100)) + " offset:" + offset +
              " nowOffset:" + (this.getFileFromOffset() + relativeOffset) + " datalen:" + dataLength + " totalSize:" + totalSize +
              " sumSize:" + sumSize + " FileFromOffset:" + getFileFromOffset() + " readposition:" + readPosition);

      relativeOffset += totalSize;
      sumSize += totalSize;
    } while (--num > 0);

    if (sumSize > 0) {
      if (this.hold()) {
        return new SelectMappedBufferResult(dataBuffer, offset, sumSize, this);
      } else {
        log.warn("matched, but hold failed, request pos: " + offset + ", fileFromOffset: "
                + this.fileFromOffset);
      }
    }
    return null;
  }

  public boolean isFull() {
    return this.wrotePosition.get() >= this.fileSize;
  }

  private boolean isAbleToFlush(int flushLeastPages) {
    long wrotePoint = this.wrotePosition.get();
    long flushPoint = this.flushedPosition.get();

    if (isFull()) {
      return false;
    }

    if (flushLeastPages > 0) {
      return flushLeastPages >= (wrotePoint - flushPoint) / OS_PAGE_SIZE;
    }

    return wrotePoint > flushPoint;
  }

  public int flush(final int flushLeastPages) {
    if (isAbleToFlush(flushLeastPages)) {
      if (this.hold()) {
        int value = this.wrotePosition.get();
        try {
          this.mappedByteBuffer.force();
        } catch (Exception e) {
          log.error("Error occurred when force data to disk.", e);
        }
        int oldFlushPosition = this.flushedPosition.get();
        this.flushedPosition.set(value);
        log.info("block file :" + this.getFileName() + " flush successly, old flush position:" + oldFlushPosition +
                " new flush position:" + this.flushedPosition.get());
        this.release();
      } else {
        log.warn("in flush, hold failed, flush offset = " + this.flushedPosition.get());
        this.flushedPosition.set(getReadPosition());
      }
    }

    return getFlushPosition();
  }

  private int getFlushPosition() {
    return this.flushedPosition.get();
  }

  public void setWrotePosition(int pos) {
    this.wrotePosition.set(pos);
  }

  public void setFlushedPosition(int pos) {
    this.flushedPosition.set(pos);
  }

  public long getFileFromOffset() {
    return this.fileFromOffset;
  }

  public int getReadPosition() {
    return this.wrotePosition.get();
  }

  @Override
  public boolean clean() {
    if (this.isAvailable()) {
      log.error("this file[REF:" + this.getRefCount() + "] " + this.fileName
              + " have not shutdown, stop unmapping.");
      return false;
    }

    if (this.isCleanUpOver()) {
      log.error("this file[REF:" + this.getRefCount() + "] " + this.fileName
              + " have cleanup, do not do it again.");
      return true;
    }

    clean(this.mappedByteBuffer);
    log.info("unmap file[REF:" + this.getRefCount() + "] " + this.fileName + " OK");
    return true;
  }

  public static void clean(final ByteBuffer buffer) {
    if (buffer == null || !buffer.isDirect() || buffer.capacity() == 0) {
      return;
    }
    invoke(invoke(viewed(buffer), "cleaner"), "clean");
  }

  private static Object invoke(final Object target, final String methodName, final Class<?>... args) {
    return AccessController.doPrivileged(new PrivilegedAction<Object>() {
      @Override
      public Object run() {
        try {
          Method method = method(target, methodName, args);
          method.setAccessible(true);
          return method.invoke(target);
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
      }
    });
  }
  private static Method method(Object target, String methodName, Class<?>[] args)
          throws NoSuchMethodException {
    try {
      return target.getClass().getMethod(methodName, args);
    } catch (NoSuchMethodException e) {
      return target.getClass().getDeclaredMethod(methodName, args);
    }
  }

  private static ByteBuffer viewed(ByteBuffer buffer) {
    String methodName = "viewedBuffer";

    Method[] methods = buffer.getClass().getMethods();
    for (int i = 0; i < methods.length; i++) {
      if (methods[i].getName().equals("attachment")) {
        methodName = "attachment";
        break;
      }
    }

    ByteBuffer viewedBuffer = (ByteBuffer) invoke(buffer, methodName);
    if (viewedBuffer == null) {
      return buffer;
    } else {
      return viewed(viewedBuffer);
    }
  }

  public long getStoreTimestamp() {
    return storeTimestamp;
  }

  public ByteBuffer sliceByteBuffer() {
    return this.mappedByteBuffer.slice();
  }

  public String getFileName() {
    return fileName;
  }

  public boolean destroy(final long intervalForcibly) {

    this.shutdown(intervalForcibly);

    if (this.isCleanUpOver()) {
      try {
        this.fileChannel.close();
        log.info("close file channel " + this.fileName + " OK");

        long beginTime = System.currentTimeMillis();
        boolean result = this.file.delete();
        log.info("delete file[" + this.fileName
                + (result ? " OK, " : " Failed, ") + "W:" + this.getWrotePosition() + " M:"
                + this.getFlushedPosition() + ", "
                + UtilAll.computeEclipseTimeMilliseconds(beginTime));
      } catch (Exception e) {
        log.warn("close file channel " + this.fileName + " Failed. ", e);
      }

      return true;
    } else {
      log.warn("destroy mapped file " + this.fileName
              + " Failed. cleanupOver: " + this.cleanupOver);
    }
    return true;
  }

  public AtomicInteger getWrotePosition() {
    return wrotePosition;
  }

  public AtomicInteger getFlushedPosition() {
    return flushedPosition;
  }

  public File getFile() {
    return file;
  }
}
