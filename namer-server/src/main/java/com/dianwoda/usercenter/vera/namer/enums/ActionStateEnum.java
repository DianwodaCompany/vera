package com.dianwoda.usercenter.vera.namer.enums;

/**
 * @author seam
 */
public enum ActionStateEnum {
  DEFAULT((byte)-1, "默认"),
  AGREE((byte)1, "同意"),
  REJECT((byte)4, "拒绝"),
  FINISH((byte)6, "完成"),

  ;

  byte state;
  String mean;

  ActionStateEnum(byte state, String mean) {
    this.state = state;
    this.mean = mean;
  }

  public byte getState() {
    return state;
  }

  public boolean isFinalState() {
    return state == REJECT.getState() ||
            state == FINISH.getState();
  }

  public boolean isAgree() {
    return state == AGREE.getState();
  }

  public boolean isReject() {
    return state == REJECT.getState();
  }
}
