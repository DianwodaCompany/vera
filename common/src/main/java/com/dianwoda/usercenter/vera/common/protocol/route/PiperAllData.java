package com.dianwoda.usercenter.vera.common.protocol.route;

import com.dianwoda.usercenter.vera.remoting.protocol.RemotingSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author seam
 */
public class PiperAllData extends RemotingSerializable {

  private List<PiperData> piperDataList = new ArrayList<>();


  public List<PiperData> getPiperDataList() {
    return piperDataList;
  }


  public void addPiperData(PiperData piperData) {
    this.piperDataList.add(piperData);
  }
}
