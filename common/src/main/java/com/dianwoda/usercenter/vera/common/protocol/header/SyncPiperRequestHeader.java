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

import com.dianwoda.usercenter.vera.common.enums.ConsumeFromWhere;
import com.dianwoda.usercenter.vera.remoting.CommandCustomHeader;
import com.dianwoda.usercenter.vera.remoting.annotation.CFNotNull;
import com.dianwoda.usercenter.vera.remoting.exception.RemotingCommandException;

public class SyncPiperRequestHeader implements CommandCustomHeader {
  @CFNotNull
  private String syncPiperLocation;

  @CFNotNull
  private Integer operateType;    // 0 初始化，1 运行, 2 停止

  @CFNotNull
  private String syncPiperGroup;

  @CFNotNull
  private int consumeFromWhere = ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET.getCode();

  public Integer getOperateType() {
    return operateType;
  }

  public void setOperateType(Integer operateType) {
    this.operateType = operateType;
  }

  @Override
  public void checkFields() throws RemotingCommandException {

  }

  public String getSyncPiperLocation() {
    return syncPiperLocation;
  }

  public void setSyncPiperLocation(String syncPiperLocation) {
    this.syncPiperLocation = syncPiperLocation;
  }


  public int getConsumeFromWhere() {
    return consumeFromWhere;
  }

  public void setConsumeFromWhere(int consumeFromWhere) {
    this.consumeFromWhere = consumeFromWhere;
  }

  public String getSyncPiperGroup() {
    return syncPiperGroup;
  }

  public void setSyncPiperGroup(String syncPiperGroup) {
    this.syncPiperGroup = syncPiperGroup;
  }
}
