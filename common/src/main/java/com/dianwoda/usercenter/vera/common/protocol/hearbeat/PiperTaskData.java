package com.dianwoda.usercenter.vera.common.protocol.hearbeat;

import com.dianwoda.usercenter.vera.common.protocol.route.TaskState;
import com.dianwoda.usercenter.vera.remoting.protocol.RemotingSerializable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author seam
 */
public class PiperTaskData extends RemotingSerializable {

  private String masterName;
  private List<String> sentinelList;
  private String password;
  private TaskState listenRedisState = TaskState.BLANK;
  private Map<String /* sync piper location */, TaskState> syncPiperStateMap = new ConcurrentHashMap<String, TaskState>();
  private long updateTime;

  public TaskState getListenRedisState() {
    return listenRedisState;
  }

  public void setListenRedisState(TaskState listenRedisState) {
    this.listenRedisState = listenRedisState;
  }

  public Map<String, TaskState> getSyncPiperStateMap() {
    return syncPiperStateMap;
  }

  public void setSyncPiperStateMap(Map<String, TaskState> syncPiperStateMap) {
    this.syncPiperStateMap = syncPiperStateMap;
  }

  public long getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(long updateTime) {
    this.updateTime = updateTime;
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
}
