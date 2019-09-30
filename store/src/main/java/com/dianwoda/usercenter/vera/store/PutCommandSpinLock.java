package com.dianwoda.usercenter.vera.store;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author seam
 */
public class PutCommandSpinLock implements PutCommandLock {
  // false -> can lock, true -> in lock
  private AtomicBoolean spinLock = new AtomicBoolean(false);

  @Override
  public void lock() {
    while (spinLock.compareAndSet(false, true)) {

    }
  }

  @Override
  public void unlock() {
    spinLock.set(false);
  }
}
