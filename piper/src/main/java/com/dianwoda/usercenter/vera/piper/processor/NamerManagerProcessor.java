package com.dianwoda.usercenter.vera.piper.processor;

import com.dianwoda.usercenter.vera.common.UtilAll;
import com.dianwoda.usercenter.vera.common.VeraVersion;
import com.dianwoda.usercenter.vera.common.protocol.RequestCode;
import com.dianwoda.usercenter.vera.common.protocol.ResponseCode;
import com.dianwoda.usercenter.vera.common.protocol.body.ConsumerRunningInfo;
import com.dianwoda.usercenter.vera.common.protocol.header.*;
import com.dianwoda.usercenter.vera.common.protocol.hearbeat.PiperTaskData;
import com.dianwoda.usercenter.vera.common.protocol.route.TaskState;
import com.dianwoda.usercenter.vera.piper.client.PiperClientInstance;
import com.dianwoda.usercenter.vera.remoting.common.RemotingHelper;
import com.dianwoda.usercenter.vera.remoting.exception.RemotingCommandException;
import com.dianwoda.usercenter.vera.remoting.netty.NettyRequestProcessor;
import com.dianwoda.usercenter.vera.remoting.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 来自namer的消息消费实现
 * @author seam
 */
public class NamerManagerProcessor implements NettyRequestProcessor {
  protected static final Logger log = LoggerFactory.getLogger(NamerManagerProcessor.class);
  private PiperClientInstance piperClientInstance;
  public NamerManagerProcessor(PiperClientInstance piperClientInstance) {
    this.piperClientInstance = piperClientInstance;
  }

  @Override
  public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request)
          throws RemotingCommandException {
    log.info(String.format("receive request, %d %s %s",
            request.getCode(),
            RemotingHelper.parseChannelRemoteAddr(ctx.channel()), request));

    switch (request.getCode()) {
      case RequestCode.LISTEN_REDIS:
        return this.listenRedis(ctx, request);

      case RequestCode.SYNC_PIPER:
        return this.syncPiper(ctx, request);

      case RequestCode.GET_PIPER_TASK_INFO:
        return this.getPiperTaskInfo(ctx, request);

      case RequestCode.GET_PIPER_RUNNING_INFO:
        return this.getPiperRunningInfo(ctx, request);
      default:
        break;
    }
    return null;
  }

  public RemotingCommand getPiperRunningInfo(ChannelHandlerContext ctx, RemotingCommand request)
          throws RemotingCommandException {
    final RemotingCommand response = RemotingCommand.createResponseCommand(null);
    final GetPiperRunningInfoRequestHeader requestHeader = (GetPiperRunningInfoRequestHeader) request.decodeCommandCustomHeader(GetPiperRunningInfoRequestHeader.class);

    ConsumerRunningInfo runningInfo = this.piperClientInstance.getPullConsumerImpl().piperRunningInfo();
    if (runningInfo != null) {
      List<String> list = this.piperClientInstance.getPiperClientAPIImpl().getRemotingClient().getNamerLocatoinList();
      String namerServers = list == null ? null : list.stream().collect(Collectors.joining(";"));
      if (requestHeader.isJstackEnable()) {
        Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
        String jstack = UtilAll.jstack(map);
        runningInfo.setJstack(jstack);
      }
      runningInfo.getProperties().setProperty(ConsumerRunningInfo.PROP_NAMERSERVER_ADDR, namerServers);
      runningInfo.getProperties().setProperty(ConsumerRunningInfo.PROP_CLIENT_VERSION, VeraVersion.getVersionDesc(VeraVersion.CURRENT_VERSION));
    }
    if (runningInfo != null) {
      response.setCode(ResponseCode.SUCCESS);
      response.setBody(runningInfo.encode());
    } else {
      response.setCode(ResponseCode.SYSTEM_ERROR);
      response.setRemark(String.format("getPiperRunningInfo error"));
    }
    return response;
  }


  public RemotingCommand getPiperTaskInfo(ChannelHandlerContext ctx, RemotingCommand request) {
    final RemotingCommand response = RemotingCommand.createResponseCommand(PiperTaskInfoResponseHeader.class);
    response.setOpaque(request.getOpaque());
    PiperTaskData taskData = this.piperClientInstance.getPiperClientInterImpl().getPiperTaskInfo();
    response.setBody(taskData.encode());
    return response;
  }

  public RemotingCommand listenRedis(ChannelHandlerContext ctx, RemotingCommand request)
          throws RemotingCommandException {

    final RemotingCommand response = RemotingCommand.createResponseCommand(ListenRedisResponseHeader.class);
    final ListenRedisRequestHeader requestHeader = (ListenRedisRequestHeader) request.decodeCommandCustomHeader(ListenRedisRequestHeader.class);
    response.setOpaque(request.getOpaque());

    boolean result = false;
    boolean stateCheckResult = false;
    switch (requestHeader.getOperateType()) {
      case 0:   // 初始化
        stateCheckResult = checkListenRedisOperation(requestHeader);
        result = stateCheckResult && listenRedisInitial(requestHeader);
        break;

      case 1:   // 运行
        stateCheckResult = checkListenRedisOperation(requestHeader);
        result = stateCheckResult && listenRedisRunning(requestHeader);
        break;

      case 2:   // 停止
        stateCheckResult = checkListenRedisOperation(requestHeader);
        result = stateCheckResult && listenRedisStop(requestHeader);
        break;
    }
    if (!stateCheckResult) {
      response.setRemark("redis任务状态不一致, 当前状态: " + this.listenRedisState());
    }
    response.setCode(result ? ResponseCode.SUCCESS : ResponseCode.SYSTEM_ERROR);
    return response;
  }


  public RemotingCommand syncPiper(ChannelHandlerContext ctx, RemotingCommand request)
          throws RemotingCommandException {
    final RemotingCommand response = RemotingCommand.createResponseCommand(SyncPiperResponseHeader.class);
    final SyncPiperRequestHeader requestHeader = (SyncPiperRequestHeader) request.decodeCommandCustomHeader(SyncPiperRequestHeader.class);
    response.setOpaque(request.getOpaque());
    boolean result = false;
    boolean stateCheckResult = false;
    switch (requestHeader.getOperateType()) {
      case 0:     // 初始化
        stateCheckResult = checkSyncPiperOperation(requestHeader);
        result = stateCheckResult && syncPiperInitial(requestHeader);
        break;
      case 1:     // 运行
        stateCheckResult = checkSyncPiperOperation(requestHeader);
        result = stateCheckResult && syncPiperRunning(requestHeader);
        break;
      case 2:     // 停止
        stateCheckResult = checkSyncPiperOperation(requestHeader);
        result = stateCheckResult && syncPiperStop(requestHeader);
        break;
      default:
        return null;
    }
    if (!stateCheckResult) {
      response.setRemark("syncPiper任务状态不一致, 当前状态: " + this.syncPiperState(requestHeader.getSyncPiperLocation()));
    }
    response.setCode(result ? ResponseCode.SUCCESS : ResponseCode.SYSTEM_ERROR);
    return response;
  }

  private TaskState listenRedisState() {
    return this.piperClientInstance.
            getPiperClientInterImpl().getListenRedisState();
  }

  private TaskState syncPiperState(String syncPiperLocation) {
    return this.piperClientInstance.
            getPiperClientInterImpl().getSyncPiperState(syncPiperLocation);
  }

  private synchronized boolean listenRedisInitial(ListenRedisRequestHeader requestHeader) {
    return this.piperClientInstance.
            getPiperClientInterImpl().redisReplicatorInitial(requestHeader);
  }
  private boolean checkListenRedisOperation(ListenRedisRequestHeader requestHeader) {
    return this.piperClientInstance.
            getPiperClientInterImpl().checkListenRedisOperation(requestHeader.getOperateType());
  }

  private synchronized boolean listenRedisRunning(ListenRedisRequestHeader requestHeader) {
    return this.piperClientInstance.
            getPiperClientInterImpl().redisReplicatorRun();
  }

  private synchronized boolean listenRedisStop(ListenRedisRequestHeader requestHeader) {
    return this.piperClientInstance.
            getPiperClientInterImpl().redisReplicatorStop();
  }


  private boolean checkSyncPiperOperation(SyncPiperRequestHeader requestHeader) {
    return this.piperClientInstance.
            getPiperClientInterImpl().checkSyncPiperOperation(requestHeader.getSyncPiperLocation(), requestHeader.getOperateType());
  }

  private synchronized boolean syncPiperInitial(SyncPiperRequestHeader requestHeader) {
    return this.piperClientInstance.
            getPiperClientInterImpl().syncPiperInitial(requestHeader.getSyncPiperLocation());
  }

  private synchronized boolean syncPiperRunning(SyncPiperRequestHeader requestHeader) {
    return this.piperClientInstance.
            getPiperClientInterImpl().syncPiperRunning(requestHeader);
  }

  private synchronized boolean syncPiperStop(SyncPiperRequestHeader requestHeader) {
    return this.piperClientInstance.
            getPiperClientInterImpl().syncPiperStop(requestHeader.getSyncPiperLocation());
  }


  @Override
  public boolean rejectRequest() {
    return false;
  }
}
