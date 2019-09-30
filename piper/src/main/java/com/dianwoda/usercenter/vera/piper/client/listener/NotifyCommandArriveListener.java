package com.dianwoda.usercenter.vera.piper.client.listener;

import com.dianwoda.usercenter.vera.piper.longpolling.PullRequestHoldService;
import com.dianwoda.usercenter.vera.store.listener.CommandArrivingListener;

/**
 * 新redis command写入时，及时通知之前offset request获取数据
 * @author seam
 */
public class NotifyCommandArriveListener implements CommandArrivingListener {
  private PullRequestHoldService holdService;

  public NotifyCommandArriveListener(PullRequestHoldService holdService) {
    this.holdService = holdService;
  }

  @Override
  public boolean commandArriveNotify(long writeOffset, int writeBytes) {
    return this.holdService.notifyMessageArriving(writeOffset, writeBytes);
  }
}
