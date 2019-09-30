package com.dianwoda.usercenter.vera.store;

/**
 * @author seam
 */
public class DispatchRequest {

  private final boolean success;
  private final int totalSize;
  private long logicOffset;
  public DispatchRequest(int totalSize, boolean success) {
    this.totalSize = totalSize;
    this.success = success;
  }

  public boolean isSuccess() {
    return success;
  }

  public int getTotalSize() {
    return totalSize;
  }

  public long getLogicOffset() {
    return logicOffset;
  }

  public void setLogicOffset(long logicOffset) {
    this.logicOffset = logicOffset;
  }
}
