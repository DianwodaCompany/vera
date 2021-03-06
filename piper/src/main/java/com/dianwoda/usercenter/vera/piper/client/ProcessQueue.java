package com.dianwoda.usercenter.vera.piper.client;

import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.message.CommandExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 命令处理队列，可回滚
 * @author seam
 */
public class ProcessQueue {
  protected static final Logger log = LoggerFactory.getLogger(ProcessQueue.class);
  private volatile boolean dropped = false;
  private volatile long lastPullTimestamp = SystemClock.now();
  private volatile boolean isConsuming = false;
  private AtomicInteger msgCount = new AtomicInteger(0);
  private ReadWriteLock treeMapLock = new ReentrantReadWriteLock();
  private ReentrantLock consumeLock = new ReentrantLock();
  private final TreeMap<Long, CommandExt> commandTreeMap = new TreeMap<Long, CommandExt>();
  private final TreeMap<Long, CommandExt> commandTempTreeMap = new TreeMap<>();
  private final String syncPiperLocation;

  public ProcessQueue(String syncPiperLocation) {
    this.syncPiperLocation = syncPiperLocation;
  }

  public ProcessQueueStatus putCommand(final List<CommandExt> commands) {
    if (commands == null) {
      return new ProcessQueueStatus(-1, false);
    }
    boolean dispatchConsume = false;
    int oldMsgCount = msgCount.get();
    try {
      this.treeMapLock.writeLock().lockInterruptibly();
      int validMsgCount = 0;
      for (CommandExt commandExt : commands) {
        CommandExt old = commandTreeMap.put(commandExt.getStoreOffset(), commandExt);
        if (old == null) {
          validMsgCount++;
        }
      }
      if (validMsgCount > 0 && !this.isConsuming) {
        this.isConsuming = true;
        dispatchConsume = true;
      }
      msgCount.addAndGet(validMsgCount);

    } catch (Exception e) {
      log.error("pullCommand exception", e);
      return new ProcessQueueStatus(-1, false);
    } finally {
      this.treeMapLock.writeLock().unlock();
    }
    return new ProcessQueueStatus(oldMsgCount, dispatchConsume);
  }

  public boolean isDropped() {
    return dropped;
  }

  private void setDropped(boolean dropped) {
    this.dropped = dropped;
  }

  public boolean close() {
    this.setDropped(true);
    return true;
  }

  public long getLastPullTimestamp() {
    return lastPullTimestamp;
  }

  public void setLastPullTimestamp(long lastPullTimestamp) {
    this.lastPullTimestamp = lastPullTimestamp;
  }

  public AtomicInteger getMsgCount() {
    return msgCount;
  }

  public TreeMap<Long, CommandExt> getCommandTreeMap() {
    return commandTreeMap;
  }

  public long getMaxSpan() {
    try {
      this.treeMapLock.readLock().lockInterruptibly();
      if (!this.commandTreeMap.isEmpty()) {
        return this.commandTreeMap.lastKey() - this.commandTreeMap.firstKey();
      }

    } catch (InterruptedException e) {
      log.error("getMaxSpan exception", e);
    } finally {
      this.treeMapLock.readLock().unlock();
    }
    return 0;
  }

  public List<CommandExt> takeCommands(int num) throws InterruptedException {
    List<CommandExt> commands = new ArrayList<>();
    if (this.treeMapLock.writeLock().tryLock(10000, TimeUnit.MILLISECONDS)) {
      try {
        for (int i = 0; i < num; i++) {
          Map.Entry<Long, CommandExt> entry = this.commandTreeMap.pollFirstEntry();
          if (entry != null) {
            this.commandTempTreeMap.put(entry.getKey(), entry.getValue());
            commands.add(entry.getValue());
          } else {
            break;
          }
        }
        if (commands.isEmpty()) {
          this.isConsuming = false;
        }

      } finally {
        this.treeMapLock.writeLock().unlock();
      }
    } else {
      log.error("try get lock of takeCommands error!");
    }
    return commands;
  }

  public long commit() throws InterruptedException {

    if (this.treeMapLock.writeLock().tryLock(20000, TimeUnit.MILLISECONDS)) {
      try {
        Map.Entry<Long, CommandExt> entry = this.commandTempTreeMap.lastEntry();
        this.msgCount.addAndGet(this.commandTempTreeMap.size() * -1);
        this.commandTempTreeMap.clear();

        if (entry != null) {
          Long lastOffset = entry.getKey();
          CommandExt lastValue = entry.getValue();
          return lastOffset + lastValue.getTotalSize();
        }

      } finally {
        this.treeMapLock.writeLock().unlock();
      }
    } else {
      log.error("commit elasp overtime, error!");
    }
    return -1;
  }

  public int getTempTreeSize() {
    try {
      if (this.treeMapLock.readLock().tryLock(1000, TimeUnit.MILLISECONDS)) {
        return this.commandTempTreeMap.size();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.treeMapLock.readLock().unlock();
    }
    return -1;
  }

  public int getTreeSize() {
    try {
      if (this.treeMapLock.readLock().tryLock(1000, TimeUnit.MILLISECONDS)) {
        return this.commandTreeMap.size();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.treeMapLock.readLock().unlock();
    }
    return -1;
  }

  public void consumeAgain(List<CommandExt> commands) throws InterruptedException {
    if (this.treeMapLock.writeLock().tryLock(2000, TimeUnit.MILLISECONDS)){
      try {
        if (!commands.isEmpty()) {
          for (CommandExt command : commands) {
            this.commandTempTreeMap.remove(command.getStoreOffset());
            this.commandTreeMap.put(command.getStoreOffset(), command);
          }
        }
      } finally {
        this.treeMapLock.writeLock().unlock();
      }
    }
  }

  public ReentrantLock getConsumeLock() {
    return consumeLock;
  }

  public String getSyncPiperLocation() {
    return syncPiperLocation;
  }

  public void clear() {
    try {
      this.treeMapLock.writeLock().lockInterruptibly();
      this.commandTempTreeMap.clear();
      this.commandTreeMap.clear();
      this.msgCount.set(0);

    } catch (InterruptedException e) {
      log.error("clear error", e);
    } finally {
      this.treeMapLock.writeLock().unlock();
    }
  }

  public boolean isConsuming() {
    return isConsuming;
  }

  public void reset() {
    this.clear();
    this.dropped = false;
    this.isConsuming = false;
  }

  static class ProcessQueueStatus {
    private boolean dispatchConsume;
    private int noProccessMsgCount;

    public ProcessQueueStatus(int noProccessMsgCount, boolean dispatchConsume) {
      this.dispatchConsume = dispatchConsume;
      this.noProccessMsgCount = noProccessMsgCount;
    }

    public boolean isDispatchConsume() {
      return dispatchConsume;
    }

    public int isNoProccessMsgCount() {
      return noProccessMsgCount;
    }

    @Override
    public String toString() {
      return "dispatchConsume:" + dispatchConsume + ", noProccessMsgCount:" + noProccessMsgCount;
    }
  }
}
