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
 * $Id: RegisterBrokerRequestHeader.java 1835 2013-05-16 02:00:50Z vintagewang@apache.org $
 */
package com.dianwoda.usercenter.vera.common.protocol.header;

import com.dianwoda.usercenter.vera.common.protocol.hearbeat.PiperTaskData;
import com.dianwoda.usercenter.vera.remoting.CommandCustomHeader;
import com.dianwoda.usercenter.vera.remoting.annotation.CFNotNull;
import com.dianwoda.usercenter.vera.remoting.annotation.CFNullable;
import com.dianwoda.usercenter.vera.remoting.exception.RemotingCommandException;

public class RegisterPiperRequestHeader implements CommandCustomHeader {

  @CFNotNull
  private String location;
  @CFNullable
  private String haServerAddr;
  @CFNotNull
  private Integer piperId;

  @CFNotNull
  private String group;

  @Override
  public void checkFields() throws RemotingCommandException {
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getHaServerAddr() {
    return haServerAddr;
  }

  public void setHaServerAddr(String haServerAddr) {
    this.haServerAddr = haServerAddr;
  }

  public Integer getPiperId() {
    return piperId;
  }

  public void setPiperId(Integer piperId) {
    this.piperId = piperId;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

}
