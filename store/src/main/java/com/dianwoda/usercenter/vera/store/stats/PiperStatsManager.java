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
package com.dianwoda.usercenter.vera.store.stats;

import com.dianwoda.usercenter.vera.common.ThreadFactoryImpl;
import com.dianwoda.usercenter.vera.common.stats.MomentStatsItemSet;
import com.dianwoda.usercenter.vera.common.stats.StatsItem;
import com.dianwoda.usercenter.vera.common.stats.StatsItemSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class PiperStatsManager {
  private static final Logger log = LoggerFactory.getLogger(PiperStatsManager.class);

  public static final String LOCATION_GET_FALL_SIZE = "LOCATION_GET_FALL_SIZE";
  public static final String LOCATION_GET_FALL_TIME = "LOCATION_GET_FALL_TIME";
  public static final String LOCATION_PUT_NUMS = "LOCATION_PUT_NUMS";
  public static final String LOCATION_PUT_SIZE = "LOCATION_PUT_SIZE";
  public static final String LOCATION_RECONSUME_NUMS = "LOCATION_RECONSUME_NUMS";

  private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl(
          "PiperStatsThread_"));
  private final HashMap<String, StatsItemSet> statsTable = new HashMap<String, StatsItemSet>();
  private final String group;
  private final MomentStatsItemSet momentStatsItemSetFallSize = new MomentStatsItemSet(LOCATION_GET_FALL_SIZE, scheduledExecutorService, log);
  private final MomentStatsItemSet momentStatsItemSetFallTime = new MomentStatsItemSet(LOCATION_GET_FALL_TIME, scheduledExecutorService, log);

  public PiperStatsManager(String group) {
    this.group = group;
    this.statsTable.put(LOCATION_PUT_NUMS, new StatsItemSet(LOCATION_PUT_NUMS, scheduledExecutorService, log));
    this.statsTable.put(LOCATION_PUT_SIZE, new StatsItemSet(LOCATION_PUT_SIZE, scheduledExecutorService, log));
    this.statsTable.put(LOCATION_RECONSUME_NUMS, new StatsItemSet(LOCATION_RECONSUME_NUMS, scheduledExecutorService, log));
  }

  public MomentStatsItemSet getMomentStatsItemSetFallSize() {
    return momentStatsItemSetFallSize;
  }

  public MomentStatsItemSet getMomentStatsItemSetFallTime() {
    return momentStatsItemSetFallTime;
  }

  public void start() {
  }

  public void shutdown() {
    this.scheduledExecutorService.shutdown();
  }

  public StatsItem getStatsItem(final String statsName, final String statsKey) {
    try {
      return this.statsTable.get(statsName).getStatsItem(statsKey);
    } catch (Exception e) {
    }
    return null;
  }

  public void incPiperPutNums(final int incValue) {
    final String statsKey = String.format("%s", this.group);
    this.statsTable.get(LOCATION_PUT_NUMS).getAndCreateStatsItem(statsKey).getValue().addAndGet(incValue);
  }

  public void incPiperPutSize(final int incValue) {
    final String statsKey = String.format("%s", this.group);
    this.statsTable.get(LOCATION_PUT_SIZE).getAndCreateStatsItem(statsKey).getValue().addAndGet(incValue);
  }

  public void recordDiskFallBehindTime(final String location, final long fallBehind) {
    final String statsKey = String.format("%s", location);
    this.momentStatsItemSetFallTime.getAndCreateStatsItem(statsKey).getValue().set(fallBehind);
  }

  public void recordDiskFallBehindSize(final String location, final long fallBehind) {
    final String statsKey = String.format("%s", location);
    this.momentStatsItemSetFallSize.getAndCreateStatsItem(statsKey).getValue().set(fallBehind);
  }

}
