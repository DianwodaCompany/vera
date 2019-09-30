package com.dianwoda.usercenter.vera.common.message;

import com.dianwoda.usercenter.vera.common.protocol.route.PiperInfo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author seam
 */
public class GetPiperDataFromNamerResult implements Serializable{
  private Map<String, PiperInfo> piperMap =  new HashMap<String, PiperInfo>();


  public Map<String, PiperInfo> getPiperMap() {
    return piperMap;
  }

  public void addPiperEntry(String key , PiperInfo value) {
    this.piperMap.put(key, value);
  }
}
