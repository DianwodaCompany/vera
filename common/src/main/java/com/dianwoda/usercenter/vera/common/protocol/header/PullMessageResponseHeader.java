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
 * $Id: PullMessageResponseHeader.java 1835 2013-05-16 02:00:50Z vintagewang@apache.org $
 */
package com.dianwoda.usercenter.vera.common.protocol.header;

import com.dianwoda.usercenter.vera.remoting.CommandCustomHeader;
import com.dianwoda.usercenter.vera.remoting.annotation.CFNotNull;
import com.dianwoda.usercenter.vera.remoting.exception.RemotingCommandException;

public class PullMessageResponseHeader implements CommandCustomHeader {

  @CFNotNull
  private Long nextBeginOffset;
  @CFNotNull
  private Long minOffset;
  @CFNotNull
  private Long maxOffset;

  @Override
  public void checkFields() throws RemotingCommandException {
  }

  public Long getNextBeginOffset() {
    return nextBeginOffset;
  }

  public void setNextBeginOffset(Long nextBeginOffset) {
    this.nextBeginOffset = nextBeginOffset;
  }

  public Long getMinOffset() {
    return minOffset;
  }

  public void setMinOffset(Long minOffset) {
    this.minOffset = minOffset;
  }

  public Long getMaxOffset() {
    return maxOffset;
  }

  public void setMaxOffset(Long maxOffset) {
    this.maxOffset = maxOffset;
  }

  @Override
  public String toString() {
    return "PullMessageResponseHeader ( nextBeginOffset:" + nextBeginOffset + " minOffset:" + minOffset + " maxOffset:" + maxOffset + " )";
  }
}
