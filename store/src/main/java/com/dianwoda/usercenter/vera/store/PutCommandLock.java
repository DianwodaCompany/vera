package com.dianwoda.usercenter.vera.store;

/**
 * @author seam
 */
public interface PutCommandLock {
  void lock();
  void unlock();
}
