package com.dianwoda.usercenter.vera.common.enums;

/**
 * @author seam
 */
public enum ConsumeFromWhere {

  CONSUME_FROM_LAST_OFFSET(0),  // 最新的offset开始消费

  CONSUME_FROM_LOCAL_OFFSET(1), // 从本地之前保存的offset开始消费（bug: 可能会引起sync piper重复消费消息）
  ;

  int code;
  ConsumeFromWhere(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public static ConsumeFromWhere getConsumeFromWhere(int code) {
    if (code == CONSUME_FROM_LAST_OFFSET.getCode()) {
      return CONSUME_FROM_LAST_OFFSET;
    } else if (code == CONSUME_FROM_LOCAL_OFFSET.getCode()) {
      return CONSUME_FROM_LOCAL_OFFSET;
    }
    return CONSUME_FROM_LAST_OFFSET;
  }
}
