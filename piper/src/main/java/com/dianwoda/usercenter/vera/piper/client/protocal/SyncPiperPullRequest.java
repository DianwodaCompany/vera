package com.dianwoda.usercenter.vera.piper.client.protocal;

import com.dianwoda.usercenter.vera.piper.client.ProcessQueue;

/**
 * @author seam
 */
public class SyncPiperPullRequest {
  private String targetLocation;
  private String targetGroup;
  private long nextOffset;
  private long commitOffset;
  private ProcessQueue processQueue;

  public String getTargetLocation() {
    return targetLocation;
  }

  public void setTargetLocation(String targetLocation) {
    this.targetLocation = targetLocation;
  }

  public long getNextOffset() {
    return nextOffset;
  }

  public void setNextOffset(long nextOffset) {
    this.nextOffset = nextOffset;
  }

  public ProcessQueue getProcessQueue() {
    return processQueue;
  }

  @Override
  public String toString() {
    return "PullRequest [targetLocation=" + targetLocation + ", nextOffset=" + this.nextOffset + "]";
  }

  public void setProcessQueue(ProcessQueue processQueue) {
    this.processQueue = processQueue;
  }

  public long getCommitOffset() {
    return commitOffset;
  }

  public void setCommitOffset(long commitOffset) {
    this.commitOffset = commitOffset;
  }

  public String getTargetGroup() {
    return targetGroup;
  }

  public void setTargetGroup(String targetGroup) {
    this.targetGroup = targetGroup;
  }
}
