package com.dianwoda.usercenter.vera.piper;

import com.dianwoda.usercenter.vera.common.PullSysFlag;
import com.dianwoda.usercenter.vera.common.protocol.header.PullMessageRequestHeader;
import com.dianwoda.usercenter.vera.piper.client.PiperClientInstance;
import com.dianwoda.usercenter.vera.piper.client.PullAPIWrapper;
import com.dianwoda.usercenter.vera.piper.client.PullCallback;
import com.dianwoda.usercenter.vera.piper.client.protocal.PullResult;
import com.dianwoda.usercenter.vera.piper.enums.CommunicationMode;
import com.dianwoda.usercenter.vera.piper.exception.PiperException;
import com.dianwoda.usercenter.vera.remoting.exception.RemotingException;

/**
 * @author seam
 */
public class PullAPIWrapper2 extends PullAPIWrapper {
  private PullResult pullResult;
  public PullAPIWrapper2(PiperClientInstance piperClientInstance) {
    super(piperClientInstance);
  }

  public void setPullResult(PullResult pullResult) {
    this.pullResult = pullResult;
  }

  public PullResult pullKernelImpl(String targetLocation, long offset, long commitOffset, boolean block, int msgNums,
                                   final long piperSuspendMaxTimeMillis, CommunicationMode mode, PullCallback pullCallback)
          throws PiperException, RemotingException,  InterruptedException {

    pullCallback.onSuccess(this.pullResult);
    return null;
  }

}
