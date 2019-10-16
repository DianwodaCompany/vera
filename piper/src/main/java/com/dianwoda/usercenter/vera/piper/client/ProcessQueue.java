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
  private ReadWriteLock lockTreeMap = new ReentrantReadWriteLock();
  private ReentrantLock consumeLock = new ReentrantLock();
  private final TreeMap<Long, CommandExt> commandTreeMap = new TreeMap<Long, CommandExt>();
  private final TreeMap<Long, CommandExt> commandTempTreeMap = new TreeMap<>();
  private final String syncPiperLocation;

  public ProcessQueue(String syncPiperLocation) {
    this.syncPiperLocation = syncPiperLocation;
  }

  public boolean putCommand(final List<CommandExt> commands) {
    if (commands == null) {
      return false;
    }
    boolean dispatchConsume = false;
    try {
      this.lockTreeMap.writeLock().lockInterruptibly();
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
      return false;
    } finally {
      this.lockTreeMap.writeLock().unlock();
    }
    return dispatchConsume;
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
      this.lockTreeMap.readLock().lockInterruptibly();
      if (!this.commandTreeMap.isEmpty()) {
        return this.commandTreeMap.lastKey() - this.commandTreeMap.firstKey();
      }

    } catch (InterruptedException e) {
      log.error("getMaxSpan exception", e);
    } finally {
      this.lockTreeMap.readLock().unlock();
    }
    return 0;
  }

  public List<CommandExt> takeCommands(int num) throws InterruptedException {
    List<CommandExt> commands = new ArrayList<>();
    if (this.lockTreeMap.writeLock().tryLock(2000, TimeUnit.MILLISECONDS)) {
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
        this.lockTreeMap.writeLock().unlock();
      }
    }
    return commands;
  }

  public long commit() throws InterruptedException {

    if (this.lockTreeMap.writeLock().tryLock(2000, TimeUnit.MILLISECONDS)) {
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
        this.lockTreeMap.writeLock().unlock();
      }
    }
    return -1;
  }

  public void consumeAgain(List<CommandExt> commands) throws InterruptedException {
    if (this.lockTreeMap.writeLock().tryLock(2000, TimeUnit.MILLISECONDS)){
      try {
        if (!commands.isEmpty()) {
          for (CommandExt command : commands) {
            this.commandTempTreeMap.remove(command.getStoreOffset());
            this.commandTreeMap.put(command.getStoreOffset(), command);
          }
        }
      } finally {
        this.lockTreeMap.writeLock().unlock();
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
      this.lockTreeMap.writeLock().lockInterruptibly();
      this.commandTempTreeMap.clear();
      this.commandTreeMap.clear();
      this.msgCount.set(0);

    } catch (InterruptedException e) {
      log.error("clear error", e);
    } finally {
      this.lockTreeMap.writeLock().unlock();
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
}
