package com.dianwoda.usercenter.vera.piper.client;

import com.dianwoda.usercenter.vera.common.PullSysFlag;
import com.dianwoda.usercenter.vera.common.message.CommandDecoder;
import com.dianwoda.usercenter.vera.common.message.CommandExt;
import com.dianwoda.usercenter.vera.common.protocol.header.PullMessageRequestHeader;
import com.dianwoda.usercenter.vera.piper.client.protocal.PullResult;
import com.dianwoda.usercenter.vera.piper.client.protocal.PullResultExt;
import com.dianwoda.usercenter.vera.piper.enums.CommunicationMode;
import com.dianwoda.usercenter.vera.piper.enums.PullStatus;
import com.dianwoda.usercenter.vera.piper.exception.PiperException;
import com.dianwoda.usercenter.vera.remoting.exception.RemotingException;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * 拉取动作的api封装
 * @author seam
 */
public class PullAPIWrapper {
  private final PiperClientInstance piperClientInstance;

  public PullAPIWrapper(PiperClientInstance piperClientInstance) {
    this.piperClientInstance = piperClientInstance;
  }

  public PullResult pullKernelImpl(String targetLocation, long offset, long commitOffset, boolean block, int msgNums,
                                   final long piperSuspendMaxTimeMillis, CommunicationMode mode, PullCallback pullCallback)
      throws PiperException, RemotingException,  InterruptedException{
    PullMessageRequestHeader requestHeader = new PullMessageRequestHeader();
    requestHeader.setLocation(targetLocation);
    requestHeader.setReadOffset(offset);
    requestHeader.setCommitOffset(commitOffset);
    requestHeader.setMaxMsgNums(msgNums);
    if (block) {
      requestHeader.setSysFlag(PullSysFlag.buildPullSysFlag(true));
    }
    requestHeader.setSuspendTimeoutMillis(piperSuspendMaxTimeMillis);

    PullResult pullResult = this.piperClientInstance.getPiperClientAPIImpl().pullCommand(targetLocation, requestHeader,
            piperSuspendMaxTimeMillis, mode, pullCallback);
    return pullResult;
  }

  public PullResult processPullResult(PullResult pullResult) {
    PullResultExt pullResultExt = (PullResultExt)pullResult;
    if (pullResult.getPullStatus() == PullStatus.FOUND && pullResultExt.getCommandBinary() != null) {
      ByteBuffer byteBuffer = ByteBuffer.wrap(pullResultExt.getCommandBinary());
      List<CommandExt> commandExtList = CommandDecoder.decodes(byteBuffer);

      pullResultExt.setCmdFoundList(commandExtList);
    }
    pullResultExt.setCommandBinary(null);
    return pullResultExt;
  }
}
