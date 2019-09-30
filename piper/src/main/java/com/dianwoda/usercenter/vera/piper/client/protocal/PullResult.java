package com.dianwoda.usercenter.vera.piper.client.protocal;

import com.dianwoda.usercenter.vera.common.message.CommandExt;
import com.dianwoda.usercenter.vera.piper.enums.PullStatus;

import java.util.List;

/**
 * @author seam
 */
public class PullResult {
  private final PullStatus pullStatus;
  private final long nextBeginOffset;
  private final long minOffset;
  private final long maxOffset;
  private List<CommandExt> cmdFoundList;

  public PullResult(PullStatus pullStatus, long nextBeginOffset, long minOffset, long maxOffset,
                    List<CommandExt> cmdFoundList) {
    this.pullStatus = pullStatus;
    this.nextBeginOffset = nextBeginOffset;
    this.minOffset = minOffset;
    this.maxOffset = maxOffset;
    this.cmdFoundList = cmdFoundList;
  }

  public PullStatus getPullStatus() {
    return pullStatus;
  }

  public long getNextBeginOffset() {
    return nextBeginOffset;
  }

  public long getMinOffset() {
    return minOffset;
  }

  public long getMaxOffset() {
    return maxOffset;
  }

  public List<CommandExt> getCmdFoundList() {
    return cmdFoundList;
  }

  public void setCmdFoundList(List<CommandExt> cmdFoundList) {
    this.cmdFoundList = cmdFoundList;
  }

  @Override
  public String toString() {
    return "PullStatus:" + pullStatus.name() + " nextBeginOffset:" + nextBeginOffset + " minOffset:" + minOffset +
            " maxOffset:" + maxOffset;
  }
}
