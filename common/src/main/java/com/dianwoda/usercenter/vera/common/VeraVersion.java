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
package com.dianwoda.usercenter.vera.common;

public class VeraVersion {

  public static final int CURRENT_VERSION = Version.V0_0_2_SNAPSHOT.ordinal();

  public static String getVersionDesc(int value) {
    int length = Version.values().length;
    if (value >= length) {
      return Version.values()[length - 1].name();
    }

    return Version.values()[value].name();
  }

  public static Version value2Version(int value) {
    int length = Version.values().length;
    if (value >= length) {
      return Version.values()[length - 1];
    }

    return Version.values()[value];
  }

  public enum Version {
    V0_0_1_SNAPSHOT,
    V0_0_2_SNAPSHOT,

  }
}
