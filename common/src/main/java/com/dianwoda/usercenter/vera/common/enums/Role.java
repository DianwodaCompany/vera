package com.dianwoda.usercenter.vera.common.enums;

/**
 * @author seam
 */
public enum Role {

  MASTER((byte)0),
  SLAVE((byte) 1);
  ;

  byte type;


  Role(byte type) {
    type = 0;
  }

  public byte getType() {
    return type;
  }
}
