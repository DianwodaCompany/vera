/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * $Id: PullMessageRequestHeader.java 1835 2013-05-16 02:00:50Z vintagewang@apache.org $
 */
package com.dianwoda.usercenter.vera.common.protocol.header;

import com.dianwoda.usercenter.vera.remoting.CommandCustomHeader;
import com.dianwoda.usercenter.vera.remoting.annotation.CFNotNull;
import com.dianwoda.usercenter.vera.remoting.annotation.CFNullable;
import com.dianwoda.usercenter.vera.remoting.exception.RemotingCommandException;

public class PullMessageRequestHeader implements CommandCustomHeader {
  @CFNotNull
  private String location;
  @CFNotNull
  private Long readOffset;
  @CFNotNull
  private Integer maxMsgNums;
  @CFNotNull
  private Integer sysFlag;
  @CFNotNull
  private Long commitOffset;
  @CFNotNull
  private Long suspendTimeoutMillis;

  @Override
  public void checkFields() throws RemotingCommandException {
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }


  public Long getReadOffset() {
    return readOffset;
  }

  public void setReadOffset(Long readOffset) {
    this.readOffset = readOffset;
  }

  public Integer getMaxMsgNums() {
    return maxMsgNums;
  }

  public void setMaxMsgNums(Integer maxMsgNums) {
    this.maxMsgNums = maxMsgNums;
  }

  public Integer getSysFlag() {
    return sysFlag;
  }

  public void setSysFlag(Integer sysFlag) {
    this.sysFlag = sysFlag;
  }

  public Long getCommitOffset() {
    return commitOffset;
  }

  public void setCommitOffset(Long commitOffset) {
    this.commitOffset = commitOffset;
  }

  public Long getSuspendTimeoutMillis() {
    return suspendTimeoutMillis;
  }

  public void setSuspendTimeoutMillis(Long suspendTimeoutMillis) {
    this.suspendTimeoutMillis = suspendTimeoutMillis;
  }

}
