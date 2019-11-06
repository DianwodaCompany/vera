package com.dianwoda.usercenter.vera.piper.client;

import com.dianwoda.usercenter.vera.common.PullSysFlag;
import com.dianwoda.usercenter.vera.common.enums.Role;
import com.dianwoda.usercenter.vera.common.message.CommandDecoder;
import com.dianwoda.usercenter.vera.common.message.CommandExt;
import com.dianwoda.usercenter.vera.common.protocol.header.PullMessageRequestHeader;
import com.dianwoda.usercenter.vera.common.protocol.route.PiperData;
import com.dianwoda.usercenter.vera.piper.client.protocal.PullResult;
import com.dianwoda.usercenter.vera.piper.client.protocal.PullResultExt;
import com.dianwoda.usercenter.vera.piper.enums.CommunicationMode;
import com.dianwoda.usercenter.vera.piper.enums.PullStatus;
import com.dianwoda.usercenter.vera.piper.exception.PiperException;
import com.dianwoda.usercenter.vera.remoting.exception.RemotingException;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 拉取动作的api封装
 * @author seam
 */
public class PullAPIWrapper {
  private final PiperClientInstance piperClientInstance;
  private Random random = new Random(System.currentTimeMillis());

  public PullAPIWrapper(PiperClientInstance piperClientInstance) {
    this.piperClientInstance = piperClientInstance;
  }

  public PullResult pullKernelImpl(String targetLocation, String targetGroup,
                                   long offset, long commitOffset, boolean block, int msgNums,
                                   final long piperSuspendMaxTimeMillis, CommunicationMode mode, PullCallback pullCallback)
      throws PiperException, RemotingException,  InterruptedException{

    String correctLocation = this.routeSyncLocation(targetLocation, targetGroup);
    PullMessageRequestHeader requestHeader = new PullMessageRequestHeader();
    requestHeader.setLocation(correctLocation);
    requestHeader.setReadOffset(offset);
    requestHeader.setCommitOffset(commitOffset);
    requestHeader.setMaxMsgNums(msgNums);
    if (block) {
      requestHeader.setSysFlag(PullSysFlag.buildPullSysFlag(true));
    }
    requestHeader.setSuspendTimeoutMillis(piperSuspendMaxTimeMillis);

    PullResult pullResult = this.piperClientInstance.getPiperClientAPIImpl().pullCommand(correctLocation, requestHeader,
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


  private String routeSyncLocation(String targetLocation, String targetGroup) {
    Map<String /* group */, List<PiperData> /* piperdata list */> activePiperBaseGroup = this.piperClientInstance.getPiperController().getActivePiperBaseGroup();
    if (activePiperBaseGroup.containsKey(targetGroup)) {
      List<PiperData> piperDataList = activePiperBaseGroup.get(targetGroup);
      for (PiperData piperData : piperDataList) {
        if (piperData.getRole().ordinal() == Role.MASTER.ordinal()) {
          return piperData.getLocation();
        } else if (piperData.getLocation().equals(targetLocation)) {
          return targetLocation;
        }
      }
      return piperDataList.get(randomNum() % piperDataList.size()).getLocation();
    } else {
      return targetLocation;
    }
  }

  public int randomNum() {
    int value = random.nextInt();
    if (value < 0) {
      value = Math.abs(value);
      if (value < 0) {
        value = 0;
      }
    }
    return value;
  }
}
