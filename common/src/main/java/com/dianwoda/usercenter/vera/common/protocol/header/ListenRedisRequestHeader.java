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

import java.util.ArrayList;
import java.util.List;

public class ListenRedisRequestHeader implements CommandCustomHeader {

  @CFNotNull
  private String masterName;

  @CFNotNull
  private String sentinals;

  @CFNullable
  private String password;

  @CFNotNull
  private Integer operateType;    // 0 初始化，1 运行, 2 停止

  public Integer getOperateType() {
    return operateType;
  }

  public void setOperateType(Integer operateType) {
    this.operateType = operateType;
  }

  @Override
  public void checkFields() throws RemotingCommandException {

  }

  public String getMasterName() {
    return masterName;
  }

  public void setMasterName(String masterName) {
    this.masterName = masterName;
  }

  public String getSentinals() {
    return sentinals;
  }

  public void setSentinals(String sentinals) {
    this.sentinals = sentinals;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }


}
