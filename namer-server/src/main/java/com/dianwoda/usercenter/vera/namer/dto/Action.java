package com.dianwoda.usercenter.vera.namer.dto;

import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.protocol.route.PiperData;
import com.dianwoda.usercenter.vera.namer.enums.ActionTaskEnum;
import com.dianwoda.usercenter.vera.namer.enums.ActionStateEnum;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author seam
 */
public class Action {
  private static AtomicInteger actionIncr = new AtomicInteger(0);
  private int id;
  private ActionTaskEnum actionTaskEnum;

  private String masterName;
  private List<String> sentinelList;
  private String password;
  private String syncPiperLocation;
  private long createTime;
  private long updateTime;
  private String srcLocation;
  private String group;
  private int piperId;
  private ActionStateEnum actionStateEnum;

  public Action() {
  }

  public static Action buildListenRedisAction(PiperData srcPiperData, String masterName,
                                              List<String> sentinels, String password) {
    Integer id = Action.actionIncr.getAndIncrement();
    Action action = new Action();
    action.id = id;
    action.actionTaskEnum = ActionTaskEnum.LISTEN_REDIS_ACTION;
    action.masterName = masterName;
    action.sentinelList = sentinels;
    action.password = password;
    action.createTime = SystemClock.now();
    action.updateTime = action.createTime;
    action.srcLocation = srcPiperData.getLocation();
    action.actionStateEnum = ActionStateEnum.DEFAULT;
    action.group = srcPiperData.getGroup();
    action.piperId = srcPiperData.getPiperId();
    return action;
  }

  public static Action buildSyncPiperAction(PiperData srcPiperData, String syncPiperLocation) {
    Integer id = Action.actionIncr.getAndIncrement();
    Action action = new Action();
    action.id = id;
    action.actionTaskEnum = ActionTaskEnum.SYNC_PIPER_ACTION;
    action.syncPiperLocation = syncPiperLocation;
    action.createTime = SystemClock.now();
    action.updateTime = action.createTime;
    action.srcLocation = srcPiperData.getLocation();
    action.actionStateEnum = ActionStateEnum.DEFAULT;
    action.group = srcPiperData.getGroup();
    action.piperId = srcPiperData.getPiperId();
    return action;
  }

  public ActionTaskEnum getActionTaskEnum() {
    return actionTaskEnum;
  }


  public String getSyncPiperLocation() {
    return syncPiperLocation;
  }

  public long getCreateTime() {
    return createTime;
  }

  public String getSrcLocation() {
    return srcLocation;
  }

  public ActionStateEnum getActionStateEnum() {
    return actionStateEnum;
  }

  public void setActionStateEnum(ActionStateEnum actionStateEnum) {
    this.actionStateEnum = actionStateEnum;
  }

  public void setCreateTime(long createTime) {
    this.createTime = createTime;
  }

  public void setActionTaskEnum(ActionTaskEnum actionTaskEnum) {
    this.actionTaskEnum = actionTaskEnum;
  }


  public void setSyncPiperLocation(String syncPiperLocation) {
    this.syncPiperLocation = syncPiperLocation;
  }

  public void setSrcLocation(String srcLocation) {
    this.srcLocation = srcLocation;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getMasterName() {
    return masterName;
  }

  public void setMasterName(String masterName) {
    this.masterName = masterName;
  }

  public List<String> getSentinelList() {
    return sentinelList;
  }

  public void setSentinelList(List<String> sentinelList) {
    this.sentinelList = sentinelList;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public int getPiperId() {
    return piperId;
  }

  public void setPiperId(int piperId) {
    this.piperId = piperId;
  }

  public long getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(long updateTime) {
    this.updateTime = updateTime;
  }
}
