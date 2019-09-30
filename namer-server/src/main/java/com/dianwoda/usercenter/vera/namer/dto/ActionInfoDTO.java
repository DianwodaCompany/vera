package com.dianwoda.usercenter.vera.namer.dto;

import com.dianwoda.usercenter.vera.common.protocol.route.PiperData;
import com.dianwoda.usercenter.vera.namer.enums.ActionStateEnum;
import com.dianwoda.usercenter.vera.namer.enums.ActionTaskEnum;
import com.dianwoda.usercenter.vera.namer.routeinfo.RouteInfoManager;

import java.util.stream.Collectors;

/**
 * @author seam
 */
public class ActionInfoDTO {

  private int id;
  private ActionTaskEnum actionTaskEnum;
  private PiperData srcPiperData;
  // redis or location ?
  private String operand;

  private ActionStateEnum actionStateEnum;
  private long createTime;
  private long updateTime;

  public ActionInfoDTO(int id , Action action) {
    this.id = action.getId();
    this.actionTaskEnum = action.getActionTaskEnum();
    RouteInfoManager routeInfoManager = RouteInfoManager.getIntance();
    this.srcPiperData = routeInfoManager.getPiperData(action.getSrcLocation());
    if (this.actionTaskEnum == ActionTaskEnum.LISTEN_REDIS_ACTION) {
      String temp = action.getSentinelList().stream().collect(Collectors.joining("|"));
      this.operand = action.getMasterName() + "-" + temp;
    } else {
      this.operand = action.getSyncPiperLocation();
    }

    this.actionStateEnum = action.getActionStateEnum();
    this.createTime = action.getCreateTime();
    this.updateTime = action.getUpdateTime();
  }


  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public ActionTaskEnum getActionTaskEnum() {
    return actionTaskEnum;
  }

  public void setActionTaskEnum(ActionTaskEnum actionTaskEnum) {
    this.actionTaskEnum = actionTaskEnum;
  }

  public PiperData getSrcPiperData() {
    return srcPiperData;
  }

  public void setSrcPiperData(PiperData srcPiperData) {
    this.srcPiperData = srcPiperData;
  }

  public String getOperand() {
    return operand;
  }

  public void setOperand(String operand) {
    this.operand = operand;
  }

  public ActionStateEnum getActionStateEnum() {
    return actionStateEnum;
  }

  public void setActionStateEnum(ActionStateEnum actionStateEnum) {
    this.actionStateEnum = actionStateEnum;
  }

  public long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(long createTime) {
    this.createTime = createTime;
  }

  public long getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(long updateTime) {
    this.updateTime = updateTime;
  }
}
