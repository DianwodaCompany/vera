package com.dianwoda.usercenter.vera.common.protocol.body;

import com.dianwoda.usercenter.vera.common.UtilAll;
import com.dianwoda.usercenter.vera.remoting.protocol.RemotingSerializable;

import java.util.Properties;
import java.util.TreeMap;

/**
 * @author seam
 */
public class ConsumerRunningInfo extends RemotingSerializable {
  public static final String PROP_CLIENT_VERSION = "PROP_CLIENT_VERSION";
  public static final String PROP_NAMERSERVER_ADDR = "PROP_NAMERSERVER_ADDR";

  public static final String PROP_THREADPOOL_CORE_SIZE = "PROP_THREADPOOL_CORE_SIZE";
  public static final String PROP_CONSUMER_START_TIMESTAMP = "PROP_CONSUMER_START_TIMESTAMP";
  public static final String LOCATION = "LOCATION";
  private Properties properties = new Properties();
  private TreeMap<String/* location */, ConsumeStatus> statusTable = new TreeMap<String, ConsumeStatus>();

  private String jstack;


  public Properties getProperties() {
    return properties;
  }

  public void setProperties(Properties properties) {
    this.properties = properties;
  }

  public TreeMap<String, ConsumeStatus> getStatusTable() {
    return statusTable;
  }

  public void setStatusTable(TreeMap<String, ConsumeStatus> statusTable) {
    this.statusTable = statusTable;
  }

  public String getJstack() {
    return jstack;
  }

  public void setJstack(String jstack) {
    this.jstack = jstack;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(PROP_CLIENT_VERSION).append(":" + properties.getProperty(PROP_CLIENT_VERSION));
    sb.append(" | ").append(PROP_NAMERSERVER_ADDR).append(":" + properties.getProperty(PROP_NAMERSERVER_ADDR));
    sb.append(" | ").append(PROP_THREADPOOL_CORE_SIZE).append(":" + properties.getProperty(PROP_THREADPOOL_CORE_SIZE));
    sb.append(" | ").append(PROP_CONSUMER_START_TIMESTAMP).append(":" + UtilAll.timeMillisToHumanString2(
            Long.parseLong(properties.getProperty(PROP_CONSUMER_START_TIMESTAMP))));
    statusTable.forEach((key, value) -> {
      sb.append(" | ").append(LOCATION).append(":" + key).append(", ConsumeStatus: " + value);
    });
    sb.append(" | ").append("jstack:").append(jstack);
    return sb.toString();
  }
}
