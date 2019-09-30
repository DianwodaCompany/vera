package com.dianwoda.usercenter.vera.common.protocol.body;

/**
 * @author seam
 */
public class RegisterPiperResult {
  private String haServerAddr;
  private String masterAddr;


  public String getHaServerAddr() {
    return haServerAddr;
  }

  public void setHaServerAddr(String haServerAddr) {
    this.haServerAddr = haServerAddr;
  }

  public String getMasterAddr() {
    return masterAddr;
  }

  public void setMasterAddr(String masterAddr) {
    this.masterAddr = masterAddr;
  }
}
