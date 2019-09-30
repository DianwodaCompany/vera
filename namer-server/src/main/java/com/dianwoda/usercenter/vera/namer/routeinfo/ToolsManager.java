package com.dianwoda.usercenter.vera.namer.routeinfo;

import com.dianwoda.usercenter.vera.common.enums.ConsumeFromWhere;
import com.dianwoda.usercenter.vera.common.protocol.RequestCode;
import com.dianwoda.usercenter.vera.common.protocol.ResponseCode;
import com.dianwoda.usercenter.vera.common.protocol.body.ConsumerRunningInfo;
import com.dianwoda.usercenter.vera.common.protocol.header.*;
import com.dianwoda.usercenter.vera.common.protocol.hearbeat.PiperTaskData;
import com.dianwoda.usercenter.vera.namer.NamerFactory;
import com.dianwoda.usercenter.vera.namer.tools.PiperClient;
import com.dianwoda.usercenter.vera.remoting.protocol.RemotingCommand;
import io.netty.channel.Channel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * task 任务动作功能
 * @author seam
 */
public class ToolsManager {

  private PiperClient piperClient;
  private RouteInfoManager routeInfoManager;

  public ToolsManager() {
    this.piperClient = new PiperClient(NamerFactory.make());
    this.routeInfoManager = RouteInfoManager.getIntance();
  }

  public RemotingCommand redisListen(String location, String masterName, List<String> sentinels,
                                     String password, int operationType) {
    RemotingCommand response = null;
    Map<String, RouteInfoManager.PiperLiveInfo> piperLiveMap =
            this.routeInfoManager.getPiperLiveInfoMap();
    if (!piperLiveMap.containsKey(location)) {
      response = RemotingCommand.createResponseCommand(ListenRedisResponseHeader.class);
      response.setCode(ResponseCode.SYSTEM_ERROR);
      response.setRemark("不存在连接");

    } else {
      Channel channel = this.routeInfoManager.getPiperLiveInfoMap().get(location).getChannel();
      ListenRedisRequestHeader requestHeader = new ListenRedisRequestHeader();
      requestHeader.setMasterName(masterName);
      requestHeader.setSentinals(sentinels.stream().collect(Collectors.joining(",")));
      requestHeader.setPassword(password);
      requestHeader.setOperateType(operationType);
      RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.LISTEN_REDIS, requestHeader);

      try {
        response = this.piperClient.callPiper(channel, request);
      } catch (Exception e) {
        response = RemotingCommand.createResponseCommand(ListenRedisResponseHeader.class);
        response.setCode(ResponseCode.SYSTEM_ERROR);
        response.setRemark("redisListen error, detail:" + e.getMessage());
      }
    }

    return response;
  }

  public RemotingCommand syncPiper(String srcLocation, String syncPiperLocation, int operationType) {
    RemotingCommand response = null;
    Map<String, RouteInfoManager.PiperLiveInfo> piperLiveMap =
            this.routeInfoManager.getPiperLiveInfoMap();
    if (!piperLiveMap.containsKey(srcLocation)) {
      response = RemotingCommand.createResponseCommand(SyncPiperResponseHeader.class);
      response.setCode(ResponseCode.SYSTEM_ERROR);
      response.setRemark("不存在连接");

    } else {
      Channel channel = this.routeInfoManager.getPiperLiveInfoMap().get(srcLocation).getChannel();
      SyncPiperRequestHeader requestHeader = new SyncPiperRequestHeader();
      requestHeader.setSyncPiperLocation(syncPiperLocation);
      requestHeader.setOperateType(operationType);
      requestHeader.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET.getCode());
      RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.SYNC_PIPER, requestHeader);
      try {
        response = this.piperClient.callPiper(channel, request);
      } catch (Exception e) {
        response.setCode(ResponseCode.SYSTEM_ERROR);
        response.setRemark("syncPiper error, detail:" + e.getMessage());
      }
    }
    return response;
  }

  public RemotingCommand runningInfo(String location) {
    GetPiperRunningInfoRequestHeader requestHeader = new GetPiperRunningInfoRequestHeader();
    requestHeader.setJstackEnable(true);
    RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_PIPER_RUNNING_INFO, requestHeader);
    RemotingCommand response = null;
    try {
      Channel channel = this.routeInfoManager.getPiperLiveInfoMap().get(location).getChannel();
      response = this.piperClient.callPiper(channel, request);
    } catch (Exception e) {
      response.setCode(ResponseCode.SYSTEM_ERROR);
      response.setRemark("runningInfo error, detail:" + e.getMessage());
    }
    return response;
  }
}
