package com.dianwoda.usercenter.vera.common.protocol.route;

/**
 * @author seam
 */
public enum TaskState {
  BLANK((byte)-1, "默认阶段"),
  TASK_LISTEN_REDIS_INITIAL((byte)1, "Redis侦听初始化"),
  TASK_SYNC_PIPER_INITIAL((byte)2, "复制Piper初始化"),

  TASK_LISTEN_REDIS_RUNNING((byte)4, "Redis侦听启动"),
  TASK_SYNC_PIPER_RUNNING((byte)6, "复制Piper启动"),

  TASK_LISTEN_REDIS_ABORT((byte)8, "Redis侦听停止"),

  TASK_SYNC_PIPER_ABORT((byte)10, "复制Piper停止"),

  ;

  private byte type;
  private String mean;

  TaskState(byte type, String mean) {
    this.type = type;
    this.mean = mean;
  }

  public byte getType() {
    return this.type;
  }

  public String getMean() {
    return mean;
  }
}
