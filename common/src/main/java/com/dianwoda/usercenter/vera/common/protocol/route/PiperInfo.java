package com.dianwoda.usercenter.vera.common.protocol.route;

import java.util.HashMap;
import java.util.Map;

/**
 * @author seam
 */
public class PiperInfo {
  private String group;
  private Map<Integer, PiperData> piperDatas;

  public PiperInfo(String group, HashMap<Integer, PiperData> piperDatas) {
    this.group = group;
    this.piperDatas = piperDatas;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public Map<Integer, PiperData> getPiperDatas() {
    return piperDatas;
  }

  public void setPiperDatas(Map<Integer, PiperData> piperDatas) {
    this.piperDatas = piperDatas;
  }

  public PiperData getPiperData(String location) {
    if (location == null) return null;
    if (piperDatas != null) {
      for (Map.Entry<Integer, PiperData> entry : piperDatas.entrySet()) {
        if (entry.getValue().getLocation().equals(location)) {
          return entry.getValue();
        }
      }
    }
    return null;
  }
}
