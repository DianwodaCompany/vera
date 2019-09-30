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
package com.dianwoda.usercenter.vera.piper.service;

import com.dianwoda.usercenter.vera.common.ThreadFactoryImpl;
import com.dianwoda.usercenter.vera.piper.PiperController;
import com.dianwoda.usercenter.vera.remoting.ChannelEventListener;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientHousekeepingService implements ChannelEventListener {
  private static final Logger log = LoggerFactory.getLogger(ClientHousekeepingService.class);
  private final PiperController piperController;

  private ScheduledExecutorService scheduledExecutorService = Executors
          .newSingleThreadScheduledExecutor(new ThreadFactoryImpl("ClientHousekeepingScheduledThread"));

  public ClientHousekeepingService(final PiperController piperController) {
    this.piperController = piperController;
  }

  public void start() {
    this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        try {
          ClientHousekeepingService.this.scanExceptionChannel();
        } catch (Throwable e) {
          log.error("Error occurred when scan not active client channels.", e);
        }
      }
    }, 1000 * 10, 1000 * 10, TimeUnit.MILLISECONDS);
  }

  private void scanExceptionChannel() {
    // nothing
  }

  public void shutdown() {
    this.scheduledExecutorService.shutdown();
  }

  @Override
  public void onChannelConnect(String remoteAddr, Channel channel) {
    // nothing

  }

  @Override
  public void onChannelClose(String remoteAddr, Channel channel) {
    // nothing

  }

  @Override
  public void onChannelException(String remoteAddr, Channel channel) {
    // nothing
  }

  @Override
  public void onChannelIdle(String remoteAddr, Channel channel) {
    // nothing

  }
}
