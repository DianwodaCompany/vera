package com.dianwoda.usercenter.vera.namer.dto;

/**
 * @author seam
 */
public class InterResponse {
  int code;
  String data;
  String remark;

  public InterResponse() {
  }
  public void setData(String data) {this.data = data;}
  public void setCode(int code) {
    this.code = code;
  }
  public void setRemark(String remark) {
    this.remark = remark;
  }

  public static InterResponse createInterResponse(int code) {
    InterResponse interResponse = new InterResponse();
    interResponse.setCode(code);
    return interResponse;
  }

  public static InterResponse createInterResponse(int code, String remark) {
    InterResponse interResponse = new InterResponse();
    interResponse.setCode(code);
    interResponse.setRemark(remark);
    return interResponse;
  }

  public int getCode() {
    return code;
  }

  public String getRemark() {
    return remark;
  }
  public String getData() {
    return data;
  }
}
