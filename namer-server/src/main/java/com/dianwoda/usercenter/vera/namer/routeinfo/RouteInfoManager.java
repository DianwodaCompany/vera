package com.dianwoda.usercenter.vera.namer.routeinfo;

import com.dianwoda.usercenter.vera.common.MixAll;
import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.protocol.body.RegisterPiperResult;
import com.dianwoda.usercenter.vera.common.protocol.ResponseCode;
import com.dianwoda.usercenter.vera.common.protocol.hearbeat.PiperTaskData;
import com.dianwoda.usercenter.vera.common.protocol.route.PiperData;
import com.dianwoda.usercenter.vera.common.protocol.route.PiperInfo;
import com.dianwoda.usercenter.vera.common.protocol.route.TaskState;
import com.dianwoda.usercenter.vera.namer.dto.InterResponse;
import com.dianwoda.usercenter.vera.remoting.common.RemotingUtil;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * piper数据管理功能
 * @author seam
 */
public class RouteInfoManager {

  protected static final Logger log = LoggerFactory.getLogger(RouteInfoManager.class);
  private Map<String /* group */, PiperInfo> piperDataMap = new ConcurrentHashMap<>();
  private Map<String /* location */, PiperLiveInfo> piperLiveInfoMap = new ConcurrentHashMap<>();
  private Map<String /* location */, PiperTaskData> piperTaskDataMap = new ConcurrentHashMap<>();
  private final static long PIPER_CHANNEL_EXPIRED_TIME = 1000 * 30 * 1;
  private static RouteInfoManager routeInfoManagerInstance = new RouteInfoManager();

  private RouteInfoManager() {}
  public static RouteInfoManager getIntance() {
   return  routeInfoManagerInstance;
  }

  // 注册piper
  public RegisterPiperResult registerPiper(final String group, final String location, final int piperId,
                                           final String haServerLocation, final Channel channel) {
    RegisterPiperResult result = new RegisterPiperResult();
    PiperInfo piperInfo = this.piperDataMap.get(group);
    if (piperInfo == null) {
      piperInfo = new PiperInfo(group, new HashMap<Integer, PiperData>());
      this.piperDataMap.put(group, piperInfo);
    }
    Map<Integer, PiperData> piperDatas = piperInfo.getPiperDatas();
    piperDatas.put(piperId, new PiperData(location, group, piperId));

    PiperLiveInfo prevPiperLiveInfo = this.piperLiveInfoMap.put(location,
            new PiperLiveInfo(SystemClock.now(), channel, haServerLocation));
    if (null == prevPiperLiveInfo) {
      log.info("new piper registerd, {}, HAServer:{}", location, haServerLocation);
    }
    if (MixAll.MASTER_ID != piperId) {
      PiperData masterPiperData = piperInfo.getPiperDatas().get(MixAll.MASTER_ID);
      if (masterPiperData != null) {
        PiperLiveInfo piperLiveInfo = this.piperLiveInfoMap.get(masterPiperData.getLocation());
        if (piperLiveInfo != null) {
          result.setMasterAddr(masterPiperData.getLocation());
          result.setHaServerAddr(piperLiveInfo.getHaServerLocation());
        }
      }
    }
    return result;
  }

  // 注销piper
  public void unregisterPiper(final String group, final String location, final int piperId) {
//    this.piperLiveInfoMap.remove(location);
//    this.cleanPiperExtraInfo(location);
  }

  public void receiveHearbeatData(String srcLocation, PiperTaskData piperTaskData) {
    this.piperTaskDataMap.put(srcLocation, piperTaskData);
  }


  /**
   * 清理无效piper
   */
  public void scanNotActivePiper() {
    Iterator<Map.Entry<String, PiperLiveInfo>> it = this.piperLiveInfoMap.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, PiperLiveInfo> next = it.next();
      long last = next.getValue().getLastUpdateTimestamp();
      if ((last + PIPER_CHANNEL_EXPIRED_TIME) < SystemClock.now()) {
        RemotingUtil.closeChannel(next.getValue().getChannel());
        it.remove();
        // clean other extra info
        this.cleanPiperExtraInfo(next.getKey());
        log.warn("The piper channel expired, {} {}ms", next.getKey(), PIPER_CHANNEL_EXPIRED_TIME);
      }
    }
  }

  /**
   * 清理无效piper
   * @param location
   */
  private void cleanPiperExtraInfo(String location) {

    // clean piperdatamap
    Iterator<Map.Entry<String, PiperInfo>> iterator = this.piperDataMap.entrySet().iterator();
    while (iterator.hasNext()) {
      PiperInfo piperInfo = iterator.next().getValue();
      Iterator<Map.Entry<Integer, PiperData>> it = piperInfo.getPiperDatas().entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<Integer, PiperData> entry = it.next();
        int piperId = entry.getKey();
        PiperData piperData = entry.getValue();
        if (location.equals(piperData.getLocation())) {
          it.remove();
          log.info("remove piperAddr[{}, {}] from piperDataMap, because channel destroyed",
                  piperId, location);
          break;
        }
      }
      if (piperInfo.getPiperDatas().isEmpty()) {
        log.info("remove group[{}] from piperDataMap, because channel destroyed",
                piperInfo.getGroup());
      }
    }
    // clean piperTaskDataMap
    this.piperTaskDataMap.remove(location);
  }

  public void onChannelDestroy(String location, Channel channel) {
    String locationFind = null;
    if (channel != null) {
      Iterator<Map.Entry<String, PiperLiveInfo>> iter = this.piperLiveInfoMap.entrySet().iterator();
      while (iter.hasNext()) {
        Map.Entry<String, PiperLiveInfo> entry = iter.next();
        if (entry.getValue().getChannel() == channel) {
          locationFind = entry.getKey();
          break;
        }
      }

      if (null == locationFind) {
        locationFind = location;
      } else {
        log.info("the piper's channel destroyed, {}, clean it's data structure at once", locationFind);
      }

      if (locationFind != null && locationFind.length() > 0) {
        this.cleanPiperExtraInfo(locationFind);
      }
    }
  }

  public Map<String, PiperLiveInfo> getPiperLiveInfoMap() {
    return piperLiveInfoMap;
  }

  public Map<String, PiperInfo> getPiperDataMap() {
    return piperDataMap;
  }

  public PiperData getPiperData(String location) {
    PiperData result = null;
    for (Map.Entry<String, PiperInfo> entry : this.piperDataMap.entrySet()) {
      result = entry.getValue().getPiperData(location);
      if (result != null) {
        break;
      }
    }
    return result;
  }

  public PiperTaskData getPiperTaskData(String location) {
    return this.piperTaskDataMap.get(location);
  }

  public boolean existListenRedisTask(String location, String masterName,
                                      List<String> sentinelList, String password) {
    PiperTaskData piperTaskData = getPiperTaskData(location);
    if (piperTaskData != null && masterName.equals(piperTaskData.getMasterName()) &&
            sentinelList.equals(piperTaskData.getSentinelList()) &&
            (password != null ? password.equals(password) : true) &&
            piperTaskData.getListenRedisState() != TaskState.TASK_LISTEN_REDIS_ABORT) {
      return true;
    }
    return false;
  }

  public boolean existListenRedisTask(String location) {
    PiperTaskData piperTaskData = getPiperTaskData(location);
    if (piperTaskData.getMasterName() != null) {
      return true;
    }
    return false;
  }

  public class PiperLiveInfo {
    private long lastUpdateTimestamp;
    private Channel channel;
    private String haServerLocation;

    public PiperLiveInfo(long lastUpdateTimestamp, Channel channel, String haServerLocation) {
      this.lastUpdateTimestamp = lastUpdateTimestamp;
      this.channel = channel;
      this.haServerLocation = haServerLocation;
    }

    public Channel getChannel() {
      return channel;
    }

    public String getHaServerLocation() {
      return haServerLocation;
    }

    public long getLastUpdateTimestamp() {
      return lastUpdateTimestamp;
    }
  }


}
