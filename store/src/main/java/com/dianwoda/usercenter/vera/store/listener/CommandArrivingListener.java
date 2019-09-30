package com.dianwoda.usercenter.vera.store.listener;

/**
 * 收到新command提醒
 * @author seam
 */
public interface CommandArrivingListener {

  public boolean commandArriveNotify(long offset, int writeBytes);
}
