package com.dianwoda.usercenter.vera.namer.dto;

import com.dianwoda.usercenter.vera.common.protocol.hearbeat.PiperTaskData;
import com.dianwoda.usercenter.vera.common.protocol.route.PiperData;
import com.dianwoda.usercenter.vera.common.protocol.route.TaskState;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author seam
 */
public class PiperInfoDTO {
  private long updateTime;
  private String location;
  private String group;
  private String role;
  private String hostName;

  // listen redis
  private String masterName;
  private String sentinels;
  private String password;
  private TaskState listenRedisTaskState;
  private long listenRedisUpdateTime;

  // sync piper
  private List<TaskState> syncPiperTaskStateList;
  private List<String> syncPiperLocationList;
  private long syncPiperUpdateTime;

  // to do
  private String copyMasterLocation;


  public PiperInfoDTO(PiperData piperData, PiperTaskData piperTaskData, long updateTime) {
    this.location = piperData.getLocation();
    this.group = piperData.getGroup();
    this.updateTime = updateTime;
    this.role = piperData.getPiperId() == 0 ? "Master" : "Slave";
    this.hostName = piperData.getHostName();

    // listen redis
    this.masterName = piperTaskData != null ? piperTaskData.getMasterName() : null;
    this.sentinels = (piperTaskData != null && piperTaskData.getSentinelList() != null) ?
            piperTaskData.getSentinelList().stream().collect(Collectors.joining(",")) : null;
    this.password = piperTaskData != null ? piperTaskData.getPassword() : null;
    this.listenRedisTaskState = piperTaskData.getListenRedisState();
    this.listenRedisUpdateTime = piperTaskData.getUpdateTime();

    // sync piper
    this.syncPiperTaskStateList = piperTaskData.getSyncPiperStateMap().values().stream().collect(Collectors.toList());
    this.syncPiperLocationList = piperTaskData.getSyncPiperStateMap().keySet().stream().collect(Collectors.toList());
    this.syncPiperUpdateTime = piperTaskData.getUpdateTime();
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public long getUpdateTime() {
    return updateTime;
  }

  public List<String> getSyncPiperLocationList() {
    return syncPiperLocationList;
  }

  public void setUpdateTime(long updateTime) {
    this.updateTime = updateTime;
  }

  public TaskState getListenRedisTaskState() {
    return listenRedisTaskState;
  }

  public void setListenRedisTaskState(TaskState listenRedisTaskState) {
    this.listenRedisTaskState = listenRedisTaskState;
  }

  public long getListenRedisUpdateTime() {
    return listenRedisUpdateTime;
  }

  public void setListenRedisUpdateTime(long listenRedisUpdateTime) {
    this.listenRedisUpdateTime = listenRedisUpdateTime;
  }

  public List<TaskState> getSyncPiperTaskStateList() {
    return syncPiperTaskStateList;
  }

  public void setSyncPiperTaskStateList(List<TaskState> syncPiperTaskStateList) {
    this.syncPiperTaskStateList = syncPiperTaskStateList;
  }

  public void setSyncPiperLocationList(List<String> syncPiperLocationList) {
    this.syncPiperLocationList = syncPiperLocationList;
  }

  public long getSyncPiperUpdateTime() {
    return syncPiperUpdateTime;
  }

  public void setSyncPiperUpdateTime(long syncPiperUpdateTime) {
    this.syncPiperUpdateTime = syncPiperUpdateTime;
  }

  public String getCopyMasterLocation() {
    return copyMasterLocation;
  }

  public void setCopyMasterLocation(String copyMasterLocation) {
    this.copyMasterLocation = copyMasterLocation;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getMasterName() {
    return masterName;
  }

  public void setMasterName(String masterName) {
    this.masterName = masterName;
  }

  public String getSentinels() {
    return sentinels;
  }

  public void setSentinels(String sentinels) {
    this.sentinels = sentinels;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getHostName() {
    return hostName;
  }
}
