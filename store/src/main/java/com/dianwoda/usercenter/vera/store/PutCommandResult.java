package com.dianwoda.usercenter.vera.store;

/**
 * @author seam
 */
public class PutCommandResult {

  private PutCommandStatus putCommandStatus;
  private AppendCommandResult appendCommandResult;

  public PutCommandResult(PutCommandStatus putCommandStatus, AppendCommandResult appendCommandResult) {
    this.putCommandStatus = putCommandStatus;
    this.appendCommandResult = appendCommandResult;
  }

  public PutCommandStatus getPutCommandStatus() {
    return putCommandStatus;
  }

  public void setPutCommandStatus(PutCommandStatus putCommandStatus) {
    this.putCommandStatus = putCommandStatus;
  }

  public AppendCommandResult getAppendCommandResult() {
    return appendCommandResult;
  }

  public void setAppendCommandResult(AppendCommandResult appendCommandResult) {
    this.appendCommandResult = appendCommandResult;
  }

  @Override
  public String toString() {
    return "PutMessageResult [putMessageStatus=" + putCommandStatus + ", appendMessageResult="
            + appendCommandResult + "]";
  }
}
