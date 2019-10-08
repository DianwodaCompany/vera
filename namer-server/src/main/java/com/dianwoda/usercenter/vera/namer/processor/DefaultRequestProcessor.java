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
package com.dianwoda.usercenter.vera.namer.processor;

import com.dianwoda.usercenter.vera.common.protocol.body.RegisterPiperResult;
import com.dianwoda.usercenter.vera.common.protocol.RequestCode;
import com.dianwoda.usercenter.vera.common.protocol.ResponseCode;
import com.dianwoda.usercenter.vera.common.protocol.header.*;
import com.dianwoda.usercenter.vera.common.protocol.hearbeat.PiperTaskData;
import com.dianwoda.usercenter.vera.common.protocol.route.PiperAllData;
import com.dianwoda.usercenter.vera.common.protocol.route.PiperData;
import com.dianwoda.usercenter.vera.common.protocol.route.PiperInfo;
import com.dianwoda.usercenter.vera.namer.NamerController;
import com.dianwoda.usercenter.vera.namer.routeinfo.RouteInfoManager;
import com.dianwoda.usercenter.vera.remoting.common.RemotingHelper;
import com.dianwoda.usercenter.vera.remoting.exception.RemotingCommandException;
import com.dianwoda.usercenter.vera.remoting.netty.NettyRequestProcessor;
import com.dianwoda.usercenter.vera.remoting.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DefaultRequestProcessor implements NettyRequestProcessor {
  private static final Logger log = LoggerFactory.getLogger(DefaultRequestProcessor.class);

  protected final NamerController namerController;

  public DefaultRequestProcessor(NamerController namerController) {
    this.namerController = namerController;
  }

  @Override
  public RemotingCommand processRequest(ChannelHandlerContext ctx,
                                        RemotingCommand request) throws RemotingCommandException {
    log.info(String.format("receive request, %d %s %s",
            request.getCode(),
            RemotingHelper.parseChannelRemoteAddr(ctx.channel()),
            request));

    switch (request.getCode()) {
      case RequestCode.REGISTER_PIPER:
        return this.registerPiper(ctx, request);
      case RequestCode.GET_PIPER_INFO_FROM_NAMER:
        return this.getPiperInfoFromNamer(ctx, request);
      case RequestCode.UNREGISTER_PIPER:
        return this.unregisterPiper(ctx, request);
      default:
        break;
    }
    return null;
  }

  @Override
  public boolean rejectRequest() {
    return false;
  }


  public RemotingCommand getPiperInfoFromNamer(ChannelHandlerContext ctx, RemotingCommand request)
    throws RemotingCommandException {
    final RemotingCommand response = RemotingCommand.createResponseCommand(GetPipersFromNamerResponseHeader.class);
    final GetPipersFromNamerRequestHeader requestHeader = (GetPipersFromNamerRequestHeader) request.decodeCommandCustomHeader(GetPipersFromNamerRequestHeader.class);

    // check valid src location
    String srcLocation = requestHeader.getSrcLocation();
    Map<String, RouteInfoManager.PiperLiveInfo> piperLiveInfoMap = this.namerController.getRouteInfoManager().getPiperLiveInfoMap();
    if (!piperLiveInfoMap.containsKey(srcLocation)) {
      response.setCode(ResponseCode.SYSTEM_ERROR);
      response.setRemark("请求不合法");
    } else {

      Map<String, PiperInfo> piperDataMap = this.namerController.getRouteInfoManager().getPiperDataMap();
      PiperAllData piperAllData = new PiperAllData();
      for (Map.Entry<String, PiperInfo> entry : piperDataMap.entrySet()) {
        PiperInfo value = entry.getValue();
        Map<Integer, PiperData> integerPiperDataMap = value.getPiperDatas();
        if (integerPiperDataMap != null && !integerPiperDataMap.isEmpty()) {
          for (PiperData piperData : integerPiperDataMap.values()) {
            piperAllData.addPiperData(piperData);
          }
        }
      }
      response.setCode(ResponseCode.SUCCESS);
      response.setBody(piperAllData.encode());
    }
    return response;
  }

  public RemotingCommand registerPiper(ChannelHandlerContext ctx, RemotingCommand request)
          throws RemotingCommandException {
    final RemotingCommand response = RemotingCommand.createResponseCommand(RegisterPiperResponseHeader.class);
    final RegisterPiperResponseHeader responseHeader = (RegisterPiperResponseHeader) response.readCustomHeader();

    final RegisterPiperRequestHeader requestHeader = (RegisterPiperRequestHeader) request.decodeCommandCustomHeader(RegisterPiperRequestHeader.class);
    RegisterPiperResult result = this.namerController.getRouteInfoManager().registerPiper(
            requestHeader.getGroup(),
            requestHeader.getLocation(),
            requestHeader.getPiperId(),
            requestHeader.getHostName(),
            requestHeader.getHaServerAddr(),
            ctx.channel());
    if (request.getBody() != null) {
      PiperTaskData piperTaskData = PiperTaskData.decode(request.getBody(), PiperTaskData.class);
      this.namerController.getRouteInfoManager().receiveHearbeatData(requestHeader.getLocation(), piperTaskData);
    }

    responseHeader.setHaServerAddr(result.getHaServerAddr());
    response.setCode(ResponseCode.SUCCESS);
    response.setRemark(null);
    return response;
  }

  public RemotingCommand unregisterPiper(ChannelHandlerContext ctx, RemotingCommand request) throws RemotingCommandException {
    final RemotingCommand response = RemotingCommand.createResponseCommand(null);
    final UnRegisterPiperRequestHeader requestHeader = (UnRegisterPiperRequestHeader) request.
            decodeCommandCustomHeader(UnRegisterPiperRequestHeader.class);
    this.namerController.getRouteInfoManager().unregisterPiper(requestHeader.getGroup(),
            requestHeader.getLocation(), requestHeader.getPiperId());

    response.setCode(ResponseCode.SUCCESS);
    return response;
  }
}
