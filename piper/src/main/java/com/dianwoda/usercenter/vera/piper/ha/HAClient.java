package com.dianwoda.usercenter.vera.piper.ha;

import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.ThreadFactoryImpl;
import com.dianwoda.usercenter.vera.common.enums.Role;
import com.dianwoda.usercenter.vera.common.protocol.route.PiperData;
import com.dianwoda.usercenter.vera.piper.PiperController;
import com.dianwoda.usercenter.vera.piper.config.PiperConfig;
import com.dianwoda.usercenter.vera.remoting.common.RemotingUtil;
import com.google.common.net.HostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author seam
 */
public class HAClient implements Runnable {
  protected static final Logger log = LoggerFactory.getLogger(HAClient.class);
  private ExecutorService executorService = null;
  private final PiperController piperController;
  private final PiperConfig piperConfig;
  private boolean stoped = false;
  private long reportSlaveOffset = -1;
  private long reportTimeStamp = SystemClock.now();
  private long lastWriteTimeStamp = -1;
  private static final int READ_MAX_BUFFER_SIZE = 1024 * 1024 * 4;
  private ByteBuffer offsetWriteBuffer = ByteBuffer.allocate(8);
  private ByteBuffer dataReadBuffer = ByteBuffer.allocate(READ_MAX_BUFFER_SIZE);
  private ByteBuffer dataReadBufferBackup = ByteBuffer.allocate(READ_MAX_BUFFER_SIZE);
  private int dispatchPosition = 0;
  private volatile SocketChannel socketChannel = null;
  private Selector selector = null;
  private volatile HostAndPort masterPiperDataAddr = null;


  public HAClient(final PiperController piperController, final PiperConfig piperConfig) {
    this.piperController = piperController;
    this.piperConfig = piperConfig;
    this.executorService = Executors.newSingleThreadExecutor(new ThreadFactoryImpl("HAClientThread_"));
    try {
      this.selector = Selector.open();
    } catch (IOException e) {
      log.error("HA Client create selector error", e);
    }
  }

  public void start() {
    this.executorService.submit(this);
  }

  @Override
  public void run() {
    log.info("HAClient run starting");
    while (!isStoped()) {
      try {
        if (!connectMaster()) {
          Thread.sleep(500);
          continue;
        }

        if (this.satisfyReport()) {
          if (!this.reportOffset()) {
            this.closeConnectMaster();
            log.info("HAClient close ConnectMaster");
            continue;
          }
        }
        selector.select(1000);

        boolean ok = processReadEvent();
        if (!ok) {
          this.closeConnectMaster();
          log.info("HAClient close ConnectMaster");
        }

        long interval = SystemClock.now() - this.lastWriteTimeStamp;
        if (interval > 1000 * 120) {
          log.warn("HAClient, housekeeping, found this connection[" + this.masterPiperDataAddr.toString()
                  + "] expired, " + interval);
          this.closeConnectMaster();
          log.warn("HAClient, master not response some time, so close connection");
        }

      } catch (Exception e) {
        log.error("HAClient run error", e);
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e1) {
          e1.printStackTrace();
        }
      }
    }

    try {
      this.destory();
    } catch (Exception e) {
      log.error("HAClient run error", e);
    }
    log.info("HAClient service end");
  }

  private boolean processReadEvent()  {
    int readZeroTimes = 0;
    while (this.dataReadBuffer.hasRemaining()) {

      try {
        int num = this.socketChannel.read(this.dataReadBuffer);
        if (num > 0) {
          this.lastWriteTimeStamp = SystemClock.now();
          readZeroTimes = 0;
          boolean result = this.processRequest();
          if (!result) {
            log.error("HAClient, dispatchReadRequest error");
            return false;
          }

        } else if (num == 0) {
          if (++readZeroTimes >= 3) {
            break;
          }
        } else {
          log.error("HAClient read event socket < 0");
          return false;
        }

      } catch (Exception e) {
        log.error("HAClient, dispatchReadRequest error");
      }

    }
    return true;
  }

  private boolean processRequest() {

    final int headerSize = 8 + 4;
    int readPosition = dataReadBuffer.position();

    while (true) {

      int diff = dataReadBuffer.position() - dispatchPosition;
      if (diff >= headerSize) {

        long phyOffset = dataReadBuffer.getLong(dispatchPosition);
        int dataSize = dataReadBuffer.getInt(dispatchPosition + 8);
        long slavePhyOffset = this.piperController.getCommandStore().getMaxWriteOffset();
        log.info("phyOffset:" + phyOffset + " dataSize:" + dataSize + " slavePhyOffset:" + slavePhyOffset + " diff:" + diff);

        if (slavePhyOffset != 0) {
          if (slavePhyOffset != phyOffset) {
            log.error("master pushed offset not equal the max phy offset in slave, SLAVE:" + slavePhyOffset +
                        " MASTER:" + phyOffset);
            return false;
          }
        }

        if (dataSize > 0 && diff >= dataSize + headerSize) {
          byte[] data = new byte[dataSize];
          this.dataReadBuffer.position(dispatchPosition + headerSize);
          this.dataReadBuffer.get(data);
          this.piperController.getCommandStore().appendCommandToBlockFile(phyOffset, data);
          this.dataReadBuffer.position(readPosition);
          this.dispatchPosition += dataSize + headerSize;

          continue;
        }
      }

      if (!dataReadBuffer.hasRemaining()) {
        reallocateByteBuffer();
      }
      break;
    }
    return true;
  }

  private void reallocateByteBuffer() {
    int left = READ_MAX_BUFFER_SIZE - this.dispatchPosition;
    if (left > 0) {
      this.dataReadBuffer.position(this.dispatchPosition);
      this.dataReadBufferBackup.put(this.dataReadBuffer);
    }
    ByteBuffer temp = this.dataReadBuffer;
    this.dataReadBuffer = this.dataReadBufferBackup;
    this.dataReadBufferBackup = temp;
    this.dataReadBufferBackup.position(0);
    this.dataReadBufferBackup.limit(READ_MAX_BUFFER_SIZE);

    this.dispatchPosition = 0;
  }

  private boolean satisfyReport() {
    if (this.reportTimeStamp == -1) {
      return true;
    }
    if (SystemClock.now() - this.reportTimeStamp >= 1000) {
      return true;
    }
    return false;
  }

  private boolean reportOffset() throws IOException {
    this.offsetWriteBuffer.position(0);
    this.offsetWriteBuffer.limit(8);
    this.offsetWriteBuffer.putLong(this.reportSlaveOffset);
    this.offsetWriteBuffer.flip();

    int writeZeroTimes = 0;
    while (this.offsetWriteBuffer.hasRemaining()) {
      int nums = this.socketChannel.write(this.offsetWriteBuffer);
      if (nums > 0) {
        continue;
      } else if (nums == 0) {
        if (++writeZeroTimes >= 3) {
          return false;
        }
      } else {
        return false;
      }
    }
    return true;
  }

  private boolean destory() throws IOException {
    this.setStoped(true);
    this.executorService.shutdownNow();
    if (this.socketChannel != null) {
      this.socketChannel.close();
    }
    return true;
  }

  private boolean connectMaster() throws ClosedChannelException {
    if (socketChannel == null) {
      if (this.masterPiperDataAddr == null) {
        this.masterPiperDataAddr = getMaster(false);
      }
      if (this.masterPiperDataAddr == null) {
        return false;
      }
      if (masterPiperDataAddr.getHostText().equals(this.piperConfig.getHost())  &&
              masterPiperDataAddr.getPort() == this.piperConfig.masterSyncPort()) {
        return false;
      }
      String masterAddr = masterPiperDataAddr.getHostText() + ":" + masterPiperDataAddr.getPort();

      if (masterAddr != null) {
        SocketAddress socketAddress = RemotingUtil.string2SocketAddress(masterAddr);
        this.socketChannel = RemotingUtil.connect(socketAddress);
        this.socketChannel.register(this.selector, SelectionKey.OP_READ);
      }

      this.reportSlaveOffset = this.piperController.getCommandStore().getMaxWriteOffset();
    }
    return this.socketChannel != null;
  }

  private boolean closeConnectMaster() throws IOException {
    if (this.socketChannel != null) {
      this.socketChannel.close();
      this.socketChannel = null;
    }
    this.lastWriteTimeStamp = SystemClock.now();
    return true;
  }

  private HostAndPort getMaster(boolean force) {
    if (masterPiperDataAddr == null || force) {
      Map<String /* group */, List<PiperData>> piperDataMap = this.piperController.getActivePiperBaseGroup();
      String group = this.piperConfig.group();
      List<PiperData> piperDatas = piperDataMap.get(group);
      PiperData masterPiperData = null;

      if (piperDatas != null) {
        for (PiperData piperData : piperDatas) {
          if (piperData.getRole().ordinal() == Role.MASTER.ordinal()) {
            masterPiperData = piperData;
            break;
          }
        }
      }

      if (masterPiperData != null) {
        masterPiperDataAddr = masterPiperData.getMasterHostAndPort();
      }
    }
    return masterPiperDataAddr;
  }

  public void setStoped(boolean stoped) {
    this.stoped = stoped;
  }

  public boolean isStoped() {
    return stoped;
  }

}
