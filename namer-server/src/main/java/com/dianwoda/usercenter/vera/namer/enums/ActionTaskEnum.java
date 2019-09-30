package com.dianwoda.usercenter.vera.namer.enums;

/**
 * @author seam
 */
public enum ActionTaskEnum {

  LISTEN_REDIS_ACTION((byte)1, "侦听redis动作"),
  SYNC_PIPER_ACTION((byte)2, "同步Piper"),
  COPY_MASTER_PIPER_ACTION((byte)4, "复制MasterPiper"),
  ;

  int code;
  String mean;

  ActionTaskEnum(byte code, String mean) {
    this.code = code;
    this.mean = mean;
  }

  public int getCode() {
    return code;
  }

  @Override
  public String toString() {
    return this.name() + "(" + this.mean + ")";
  }
}
