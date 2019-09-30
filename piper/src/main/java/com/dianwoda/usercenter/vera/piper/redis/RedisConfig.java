package com.dianwoda.usercenter.vera.piper.redis;

import com.dianwoda.usercenter.vera.common.protocol.header.ListenRedisRequestHeader;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * @program: vera
 * @description:
 * @author: zhouqi1
 * @create: 2018-10-18 20:55
 **/
@Data
@ToString
@NoArgsConstructor
public class RedisConfig {

  private String masterName;
  private List<String> sentinalList = new ArrayList<>();
  private String password;
  private String timeout;
  private String maxIdle;
  private String maxTotal;
  private String maxWaitMillis;
  private String testOnBorrow;

  public RedisConfig(ListenRedisRequestHeader requestHeader) {
    this.masterName = requestHeader.getMasterName();
    this.sentinalList = Lists.newArrayList(requestHeader.getSentinals().split(","));
    this.password = requestHeader.getPassword();
    this.timeout = "100000";
    this.maxIdle = "10";
    this.maxTotal = "100";
    this.maxWaitMillis = "30000";
    this.testOnBorrow = null;
  }

  public String getMasterName() {
    return masterName;
  }

  public void setMasterName(String masterName) {
    this.masterName = masterName;
  }

  public List<String> getSentinalList() {
    return sentinalList;
  }

  public void setSentinalList(List<String> sentinalList) {
    this.sentinalList = sentinalList;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getTimeout() {
    return timeout;
  }

  public void setTimeout(String timeout) {
    this.timeout = timeout;
  }

  public String getMaxIdle() {
    return maxIdle;
  }

  public void setMaxIdle(String maxIdle) {
    this.maxIdle = maxIdle;
  }

  public String getMaxTotal() {
    return maxTotal;
  }

  public void setMaxTotal(String maxTotal) {
    this.maxTotal = maxTotal;
  }

  public String getMaxWaitMillis() {
    return maxWaitMillis;
  }

  public void setMaxWaitMillis(String maxWaitMillis) {
    this.maxWaitMillis = maxWaitMillis;
  }

  public String getTestOnBorrow() {
    return testOnBorrow;
  }

  public void setTestOnBorrow(String testOnBorrow) {
    this.testOnBorrow = testOnBorrow;
  }

  @Override
  public int hashCode() {
    return this.masterName.hashCode() + this.sentinalList.hashCode() + this.password.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    RedisConfig config = (RedisConfig)obj;
    return this.masterName.equals(config.getMasterName()) && this.sentinalList.equals(config.sentinalList)
            && (this.password == null ? true : this.password.equals(config.getPassword()));
  }
}
