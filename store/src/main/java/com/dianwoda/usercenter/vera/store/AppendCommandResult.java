package com.dianwoda.usercenter.vera.store;

/**
 * @author seam
 */
public class AppendCommandResult {
  // Return code
  private AppendCommandStatus status;

  // Where to start writing
  private long wroteOffset;

  // private Write bytes
  private int wroteBytes;

  // message storage timestamp
  private long storeTimestamp;

  public AppendCommandResult(AppendCommandStatus status) {
    this.status = status;
  }

  public AppendCommandResult(AppendCommandStatus status,
                             long wroteOffset, int wroteBytes, long storeTimestamp) {
    this.status = status;
    this.wroteOffset = wroteOffset;
    this.wroteBytes = wroteBytes;
    this.storeTimestamp = storeTimestamp;
  }

  public AppendCommandStatus getStatus() {
    return status;
  }

  public long getWroteOffset() {
    return wroteOffset;
  }

  public int getWroteBytes() {
    return wroteBytes;
  }

  public long getStoreTimestamp() {
    return storeTimestamp;
  }
}
