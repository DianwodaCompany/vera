package com.dianwoda.usercenter.vera.store;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author seam
 */
public abstract class ReferenceResource {

  private AtomicInteger ref = new AtomicInteger(1);
  private volatile boolean available = true;
  private volatile boolean cleanUpOver = false;

  public boolean hold() {
    if (isAvailable()) {
      if (ref.incrementAndGet() > 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * intervalForcibly 强制删除(to do)
   * @param intervalForcibly
   */
  public void shutdown(final long intervalForcibly) {
    this.available = true;
    this.release();
  }

  public void release() {
    int refCount = ref.decrementAndGet();
    if (refCount > 0) {
      return;
    }

    cleanUpOver = clean();
  }

  public abstract boolean clean();

  public int getRefCount() {
    return this.ref.get();
  }

  public boolean isCleanUpOver() {
    return getRefCount() <= 0 && cleanUpOver;
  }

  public boolean isAvailable() {
    return available;
  }
}
