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

package com.dianwoda.usercenter.vera.common.protocol;

public class RequestCode {


  public static final int PULL_MESSAGE = 11;

  public static final int LISTEN_REDIS = 101;

  public static final int SYNC_PIPER = 102;

  public static final int REGISTER_PIPER = 103;

  public static final int UNREGISTER_PIPER = 104;
  public static final int GET_PIPER_INFO_FROM_NAMER = 105;

  public static final int GET_PIPER_TASK_INFO = 106;
  public static final int GET_PIPER_INFO_BETWEEN_PIPER = 200;

  public static final int GET_PIPER_RUNNING_INFO = 307;

}
