package com.dianwoda.usercenter.vera.piper.data;

import com.dianwoda.usercenter.vera.common.protocol.route.PiperData;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * active piper data
 * @author seam
 */
public class ActivePiperData {
  private Map<String, PiperData> piperDataMap = new ConcurrentHashMap<>();
  private long updateTime;

  public boolean containPiperData(PiperData piperData) {
    return piperDataMap.containsKey(piperData.getLocation());
  }

  public boolean containPiperData(String location) {
    return piperDataMap.containsKey(location);
  }

  public void addPiperData(PiperData piperData) {
    piperDataMap.put(piperData.getLocation(), piperData);
  }

  public void removePiperData(PiperData piperData) {
    piperDataMap.remove(piperData.getLocation());
  }

  public void addPiperDatas(List<PiperData> list) {
    if (list != null) {
      for (PiperData piperData : list) {
        addPiperData(piperData);
      }
    }
  }

  public void removePiperDatas(List<PiperData> list) {
    if (list != null) {
      for (PiperData piperData : list) {
        removePiperData(piperData);
      }
    }
  }

  public void setUpdateTime(long updateTime) {
    this.updateTime = updateTime;
  }

  public Map<String, PiperData> getPiperDataMap() {
    return piperDataMap;
  }

  public void setPiperDataMap(Map<String, PiperData> piperDataMap) {
    this.piperDataMap = piperDataMap;
  }

  public Collection<PiperData> values() {
    return piperDataMap.values();
  }
}
