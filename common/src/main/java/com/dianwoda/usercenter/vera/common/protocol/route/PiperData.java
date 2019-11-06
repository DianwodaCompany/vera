package com.dianwoda.usercenter.vera.common.protocol.route;

import com.dianwoda.usercenter.vera.common.enums.Role;
import com.google.common.net.HostAndPort;

/**
 * @author seam
 */
public class PiperData {

  private String location;
  private String group;
  private int piperId;
  private String hostName;

  public PiperData() {

  }

  public PiperData(String location, String group, int piperId, String hostName) {
    this.location = location;
    this.group = group;
    this.piperId = piperId;
    this.hostName = hostName;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public int getPiperId() {
    return piperId;
  }

  public Role getRole() {
    return getPiperId() == 0 ? Role.MASTER : Role.SLAVE;
  }

  public void setPiperId(int piperId) {
    this.piperId = piperId;
  }

  @Override
  public int hashCode() {
    return location.hashCode() + group.hashCode() + piperId;
  }

  public String getHostName() {
    return hostName;
  }

  @Override
  public boolean equals(Object obj) {
    PiperData piperData = (PiperData)obj;
    if (this.location.equals(piperData.location) &&
            this.group.equals(piperData.group) &&
            this.piperId == piperData.piperId) {
      return true;
    }
    return false;
  }

  public HostAndPort getMasterHostAndPort() {
    HostAndPort my = HostAndPort.fromString(location);
    return HostAndPort.fromParts(my.getHostText(), my.getPort() + 2);
  }

  @Override
  public String toString() {
    return "location:" + location + ", piperId:" + piperId + ", group:" + group + ", hostname:" + hostName;
  }
}
