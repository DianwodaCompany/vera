package com.dianwoda.usercenter.vera.piper.client;

import com.dianwoda.usercenter.vera.piper.client.protocal.PullResult;
import com.dianwoda.usercenter.vera.piper.enums.RequestExceptionReason;

/**
 * @author seam
 */
public interface PullCallback {

  void OnSuccess(final PullResult pullResult);

  void onException(final Throwable e, RequestExceptionReason reason);
}
