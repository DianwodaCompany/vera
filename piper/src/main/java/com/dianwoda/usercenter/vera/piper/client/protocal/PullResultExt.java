package com.dianwoda.usercenter.vera.piper.client.protocal;

import com.dianwoda.usercenter.vera.common.message.CommandExt;
import com.dianwoda.usercenter.vera.piper.enums.PullStatus;

import java.util.List;

/**
 * @author seam
 */
public class PullResultExt extends PullResult {

  private byte[] commandBinary;

  public PullResultExt(PullStatus pullStatus, long nextBeginOffset, long minOffset, long maxOffset,
                       List<CommandExt> msgFoundList, final byte[] commandBinary) {
    super(pullStatus, nextBeginOffset, minOffset, maxOffset, msgFoundList);
    this.commandBinary = commandBinary;
  }

  public byte[] getCommandBinary() {
    return commandBinary;
  }

  public void setCommandBinary(byte[] commandBinary) {
    this.commandBinary = commandBinary;
  }
}
