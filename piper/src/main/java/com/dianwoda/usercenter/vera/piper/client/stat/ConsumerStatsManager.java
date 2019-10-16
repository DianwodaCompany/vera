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

package com.dianwoda.usercenter.vera.piper.client.stat;

import com.dianwoda.usercenter.vera.common.protocol.body.ConsumeStatus;
import com.dianwoda.usercenter.vera.common.stats.StatsItemSet;
import com.dianwoda.usercenter.vera.common.stats.StatsSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;

/**
 * 作为consumer一方的统计
 */
public class ConsumerStatsManager {
  private static final Logger log = LoggerFactory.getLogger(ConsumerStatsManager.class);

  private static final String LOCATION_CONSUME_OK_TPS = "CONSUME_OK_TPS";
  private static final String LOCATION_CONSUME_FAILED_TPS = "CONSUME_FAILED_TPS";
  private static final String LOCATION_CONSUME_FINAL_FAILED_TPS = "CONSUME_FINAL_FAILED_TPS";
  private static final String LOCATION_CONSUME_RT = "CONSUME_RT";
  private static final String LOCATION_PULL_TPS = "PULL_TPS";
  private static final String LOCATION_PULL_RT = "PULL_RT";
  private static final String LOCATION_CONSUME_LIFE_CIRCLE_RT = "LOCATION_CONSUME_LIFE_CIRCLE_RT";

  private final StatsItemSet locationConsumeOKTPS;
  private final StatsItemSet locationConsumeRT;
  private final StatsItemSet locationConsumeFailedTPS;
  private final StatsItemSet locationConsumeFinalFailedTPS;
  private final StatsItemSet locationPullTPS;
  private final StatsItemSet locationPullRT;
  private final StatsItemSet locationConsumeLifeCircleRT;

  public ConsumerStatsManager(final ScheduledExecutorService scheduledExecutorService) {
    this.locationConsumeOKTPS =
            new StatsItemSet(LOCATION_CONSUME_OK_TPS, scheduledExecutorService, log);

    this.locationConsumeRT =
            new StatsItemSet(LOCATION_CONSUME_RT, scheduledExecutorService, log);

    this.locationConsumeFailedTPS =
            new StatsItemSet(LOCATION_CONSUME_FAILED_TPS, scheduledExecutorService, log);

    this.locationConsumeFinalFailedTPS =
            new StatsItemSet(LOCATION_CONSUME_FINAL_FAILED_TPS, scheduledExecutorService, log);

    this.locationPullTPS = new StatsItemSet(LOCATION_PULL_TPS, scheduledExecutorService, log);

    this.locationPullRT = new StatsItemSet(LOCATION_PULL_RT, scheduledExecutorService, log);
    this.locationConsumeLifeCircleRT = new StatsItemSet(LOCATION_CONSUME_LIFE_CIRCLE_RT, scheduledExecutorService, log);
  }

  public void start() {
  }

  public void shutdown() {
  }

  public void incPullRT(final String location, final long rt) {
    this.locationPullRT.addValue(location, (int) rt, 1);
  }

  public void incPullTPS(final String location, final long msgs) {
    this.locationPullTPS.addValue(location, (int) msgs, 1);
  }

  public void incConsumeRT(final String location, final long rt) {
    this.locationConsumeRT.addValue(location, (int) rt, 1);
  }

  public void incConsumeOKTPS(final String location, final long msgs) {
    this.locationConsumeOKTPS.addValue(location, (int) msgs, 1);
  }

  public void incConsumeFailedTPS(final String location, final long msgs) {
    this.locationConsumeFailedTPS.addValue(location, (int) msgs, 1);
  }

  public void incConsumeFinalFailedTPS(final String location, final long msgs) {
    this.locationConsumeFinalFailedTPS.addValue(location, (int) msgs, 1);
  }

  public void incConsumeLifeCircleRT(final String location, final long rt) {
    this.locationConsumeLifeCircleRT.addValue(location, (int) rt, 1);
  }

  public ConsumeStatus consumeStatus(final String location) {
    ConsumeStatus cs = new ConsumeStatus();
    StatsSnapshot ss = this.getPullRT(location);
    if (ss != null) {
      cs.setPullRT(ss.getAvgpt());
    }

    StatsSnapshot ss1 = this.getPullTPS(location);
    if (ss1 != null) {
      cs.setPullTPS(ss1.getAvgpt());
    }

    StatsSnapshot ss2 = this.getConsumeRT(location);
    if (ss2 != null) {
      cs.setConsumeRT(ss2.getAvgpt());
    }

    StatsSnapshot ss3 = this.getConsumeOKTPS(location);
    if (ss3 != null) {
      cs.setConsumeOKTPS(ss3.getTps());
    }

    StatsSnapshot ss4 = this.getConsumeFailedTPS(location);
    if (ss4 != null) {
      cs.setConsumeFailedTPS(ss4.getTps());
    }

    StatsSnapshot ss5 = this.locationConsumeFailedTPS.getStatsDataInHour(location);
    if (ss5 != null) {
      cs.setConsumeFailedMsgs(ss5.getSum());
    }

    StatsSnapshot ss6 = this.getLocationConsumeLifeCircleRT(location);
    if (ss6 != null) {
      cs.setConsumeLifeCircleRT(ss6.getSum());
    }
    return cs;
  }

  private StatsSnapshot getPullRT(final String location) {
    return this.locationPullRT.getStatsDataInMinute(location);
  }

  private StatsSnapshot getPullTPS(final String location) {
    return this.locationPullTPS.getStatsDataInMinute(location);
  }

  private StatsSnapshot getConsumeRT(final String location) {
    StatsSnapshot statsData = this.locationConsumeRT.getStatsDataInMinute(location);
    if (0 == statsData.getSum()) {
      statsData = this.locationConsumeRT.getStatsDataInHour(location);
    }
    return statsData;
  }

  private StatsSnapshot getConsumeOKTPS(final String location) {
    return this.locationConsumeOKTPS.getStatsDataInMinute(location);
  }

  private StatsSnapshot getConsumeFailedTPS(final String location) {
    return this.locationConsumeFailedTPS.getStatsDataInMinute(location);
  }

  public StatsSnapshot getLocationConsumeLifeCircleRT(final String location) {
    return locationConsumeLifeCircleRT.getStatsDataInMinute(location);
  }
}
