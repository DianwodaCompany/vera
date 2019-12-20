package com.dianwoda.usercenter.vera.piper.ha;

import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.ThreadFactoryImpl;
import com.dianwoda.usercenter.vera.store.SelectMappedBufferResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;

/**
 * @author seam
 */
public class HAConnection {
  protected static final Logger log = LoggerFactory.getLogger(HAConnection.class);
  private final HAServer haServer;
  private final SocketChannel sc;
  private Executor executor = Executors.newFixedThreadPool(2, new ThreadFactoryImpl("HAConnectionThread_"));
  private final ReadSocketHandler readSocketHandler;
  private final WriteSocketHandler writeSocketHandler;
  public final static int HAConnectionKeepingInterval = 1000 * 10;
  public final static int TransferDataSizeLimit = 1024 * 64;
  private volatile long slaveRequestOffset = -1;
  private long nextWriteOffset = -1;
  public HAConnection(final HAServer haServer, final SocketChannel sc) throws IOException {
    this.haServer = haServer;
    this.sc = sc;
    this.sc.configureBlocking(false);
    this.sc.socket().setSoLinger(false, -1);
    this.sc.socket().setTcpNoDelay(true);
    this.sc.socket().setReceiveBufferSize(1024 * 64);
    this.sc.socket().setSendBufferSize(1024 * 64);
    this.readSocketHandler = new ReadSocketHandler(this.sc);
    this.writeSocketHandler = new WriteSocketHandler(this.sc);
  }

  public void start() {
    this.executor.execute(this.readSocketHandler);
    this.executor.execute(this.writeSocketHandler);
  }

  public void destory() {
  }

  class WriteSocketHandler implements Runnable {
    private final SocketChannel sc;
    private final Selector selector;
    private boolean stop = false;
    private int headerSize = 8 + 4;
    private ByteBuffer headerBuffer = ByteBuffer.allocate(headerSize);
    private boolean lastWriteOver = true;
    private SelectMappedBufferResult transferResult;
    private long lastWriteTimestamp = 0;

    public WriteSocketHandler(SocketChannel sc) throws IOException {
      this.sc = sc;
      this.selector = Selector.open();
      this.sc.register(this.selector, SelectionKey.OP_WRITE);

    }

    @Override
    public void run() {
      log.info("WriteSocketHandler run starting");

      while (!isStop()) {
        try {
          selector.select(1000);

          if (slaveRequestOffset == -1) {
            Thread.sleep(200);
            continue;
          }
          if (nextWriteOffset == -1) {
            if (slaveRequestOffset == 0) {
              nextWriteOffset = HAConnection.this.haServer.getCommandStore().getMaxWriteOffset();
            } else {
              nextWriteOffset = slaveRequestOffset;
            }

            log.info("HAConnection start transfer data, nextWriteOffset:" + nextWriteOffset + ", slaveRequestOffset:" + slaveRequestOffset +
                      ", MaxWriteOffset:" + HAConnection.this.haServer.getCommandStore().getMaxWriteOffset());
          }
          if (!lastWriteOver) {
            this.lastWriteOver = this.transferData();
            if (!lastWriteOver) {
              continue;
            }
          }

          SelectMappedBufferResult selectMappedBufferResult = HAConnection.this.haServer.getCommandStore().getCommand(nextWriteOffset);
          if (selectMappedBufferResult != null) {
            int size = selectMappedBufferResult.getSize();
            if (size > TransferDataSizeLimit) {
              size = TransferDataSizeLimit;
              selectMappedBufferResult.getByteBuffer().limit(size);
            }
            headerBuffer.position(0);
            headerBuffer.putLong(nextWriteOffset);
            headerBuffer.putInt(size);
            headerBuffer.limit(headerSize);
            headerBuffer.flip();

            nextWriteOffset += size;
            this.transferResult = selectMappedBufferResult;
            log.info("HAConnection start transfer data, nextWriteOffset:" + nextWriteOffset + ", size:" + selectMappedBufferResult.getSize());
            this.lastWriteOver = this.transferData();

          } else {
            Thread.sleep(500);
          }

        } catch (Exception e) {
          log.error("WriteSocketHandler error", e);
          break;
        }

      }
      if (this.transferResult != null) {
        this.transferResult.release();
      }
      this.destory();
      HAConnection.log.info("close end");
    }

    private boolean transferData() throws Exception {
      int writeZeroTimes = 0;
      while (this.headerBuffer.hasRemaining()) {
        int num = this.sc.write(this.headerBuffer);
        if (num > 0) {
          this.lastWriteTimestamp = SystemClock.now();

        } else if (num == 0) {
          if (++writeZeroTimes >= 3) {
            break;
          }
        } else {
          throw new Exception("ha master write header data error");
        }
      }

      if (this.transferResult == null) {
        return false;
      }

      writeZeroTimes = 0;
      ByteBuffer bodyBuffer = this.transferResult.getByteBuffer();
      while (bodyBuffer.hasRemaining()) {
        int num = this.sc.write(bodyBuffer);
        if (num > 0) {
          this.lastWriteTimestamp = SystemClock.now();
        } else if (num == 0) {
          if (++writeZeroTimes >= 3) {
            break;
          }
        } else {
          throw new Exception("ha master write body data error");
        }
      }

      if (writeZeroTimes >= 3) {
        return false;
      }
      if (!bodyBuffer.hasRemaining()) {
        this.transferResult.release();
        this.transferResult = null;
        return true;
      }
      return false;
    }


    public void destory() {
      this.setStop(true);
      readSocketHandler.setStop(true);

      haServer.removeConnection(HAConnection.this);
      SelectionKey sk = this.sc.keyFor(this.selector);
      if (sk != null) {
        sk.cancel();
      }

      try {
        this.selector.close();
        this.sc.close();
      } catch (IOException e) {
        HAConnection.log.error("close error", e);
      }
    }
    public boolean isStop() {
      return stop;
    }

    public void setStop(boolean stop) {
      this.stop = stop;
    }
  }


  class ReadSocketHandler implements Runnable {
    private static final int READ_MAX_BUFFER_SIZE = 1024 * 1024;
    private final SocketChannel sc;
    private final Selector selector;
    private boolean stop;
    private final ByteBuffer byteBufferRead = ByteBuffer.allocate(READ_MAX_BUFFER_SIZE);
    private int readPos = 0;
    private volatile long lastTimestamp = SystemClock.now();
    private int slaveOffsetPrintCount = 0;

    public ReadSocketHandler(final SocketChannel sc) throws IOException {
      this.sc = sc;
      this.selector = Selector.open();
      this.sc.register(this.selector, SelectionKey.OP_READ);
      this.stop = false;
    }


    public void start() throws IOException {
      HAConnection.this.executor.execute(this);

    }

    public void setStop(boolean isStop) {
      this.stop = true;
    }

    public boolean isStop() {
      return stop;
    }

    public void destory() {
      this.setStop(true);
      writeSocketHandler.setStop(true);

      haServer.removeConnection(HAConnection.this);
      SelectionKey sk = this.sc.keyFor(this.selector);
      if (sk != null) {
        sk.cancel();
      }

      try {
        this.selector.close();
        this.sc.close();
      } catch (IOException e) {
        HAConnection.log.error("close error", e);
      }

      HAConnection.log.info("close end");
    }

    @Override
    public void run() {
      log.info("ReadSocketHandler run starting");
      while (!isStop()) {

        try {
          selector.select(1000);
          boolean success = this.readDataProcess();
          if (!success) {
            HAConnection.log.error("readDataProcess error");
            break;
          }

          if (SystemClock.now() - this.lastTimestamp > HAConnection.HAConnectionKeepingInterval) {
            break;
          }

        } catch (Exception e) {
          log.error("ReadStockHandler run error", e);
          break;
        }
      }

      this.destory();
    }

    private boolean readDataProcess() {
      if (!this.byteBufferRead.hasRemaining()) {
        this.byteBufferRead.flip();
        this.readPos = 0;
      }

      int readZeroCount = 0;

      while (this.byteBufferRead.hasRemaining()) {

        try {
          int num = this.sc.read(byteBufferRead);
          if (num > 0) {
            this.lastTimestamp = SystemClock.now();
            readZeroCount = 0;
            if (byteBufferRead.position() - this.readPos >= 8) {
              int pos = this.byteBufferRead.position() - (this.byteBufferRead.position() % 8);
              long readOffset = this.byteBufferRead.getLong(pos-8);
              this.readPos = pos;
              if (HAConnection.this.slaveRequestOffset != readOffset) {
                log.info("HAConnection.this.slaveRequestOffset:" + HAConnection.this.slaveRequestOffset + ", readOffset:" + readOffset);
              }
              HAConnection.this.slaveRequestOffset = readOffset;
              if (++slaveOffsetPrintCount == 10000) {
                log.info("HAConnection.this.slaveRequestOffset:" + HAConnection.this.slaveRequestOffset);
                slaveOffsetPrintCount = 0;
              }
              break;
            }
          } else if (num == 0) {
            if (++readZeroCount >= 5) {
              break;
            }
          } else {
            log.warn("readDataProcess error happend, maybe connection closed!");
            return false;
          }
        } catch (IOException e) {
          log.error("readDataProcess IOException", e);
          return false;
        }

      }
      return true;
    }
  }
}
