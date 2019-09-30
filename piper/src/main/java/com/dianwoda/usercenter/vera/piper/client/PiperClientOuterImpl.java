package com.dianwoda.usercenter.vera.piper.client;

import com.dianwoda.usercenter.vera.common.protocol.RequestCode;
import com.dianwoda.usercenter.vera.common.protocol.ResponseCode;
import com.dianwoda.usercenter.vera.common.protocol.body.RegisterPiperResult;
import com.dianwoda.usercenter.vera.common.protocol.header.*;
import com.dianwoda.usercenter.vera.common.protocol.hearbeat.PiperTaskData;
import com.dianwoda.usercenter.vera.common.protocol.route.PiperAllData;
import com.dianwoda.usercenter.vera.piper.DefaultRPCHookImpl;
import com.dianwoda.usercenter.vera.piper.client.protocal.PullResult;
import com.dianwoda.usercenter.vera.piper.client.protocal.PullResultExt;
import com.dianwoda.usercenter.vera.piper.enums.CommunicationMode;
import com.dianwoda.usercenter.vera.piper.enums.PullStatus;
import com.dianwoda.usercenter.vera.piper.enums.RequestExceptionReason;
import com.dianwoda.usercenter.vera.piper.exception.PiperException;
import com.dianwoda.usercenter.vera.piper.processor.NamerManagerProcessor;
import com.dianwoda.usercenter.vera.remoting.RPCHook;
import com.dianwoda.usercenter.vera.remoting.RemotingClient;
import com.dianwoda.usercenter.vera.remoting.exception.*;
import com.dianwoda.usercenter.vera.remoting.netty.NettyClientConfig;
import com.dianwoda.usercenter.vera.remoting.netty.NettyRemotingClient;
import com.dianwoda.usercenter.vera.remoting.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * piper做为客户端对外实现
 * @author seam
 */
public class PiperClientOuterImpl {
  protected static final Logger log = LoggerFactory.getLogger(PiperClientOuterImpl.class);
  private final PiperClientInstance piperClientInstance;
  private final RemotingClient remotingClient;
  private final NamerManagerProcessor namerManagerProcessor;
  private final RPCHook rpcHook;
  private final ExecutorService namerManagerExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
    @Override
    public Thread newThread(Runnable r) {
      return new Thread(r, "NamerManagerProcessorThread");
    }
  });

  public PiperClientOuterImpl(final PiperClientInstance piperClientInstance, final NettyClientConfig nettyClientConfig,
                              final NamerManagerProcessor namerManagerProcessor) {
    this.piperClientInstance = piperClientInstance;
    this.remotingClient = new NettyRemotingClient(nettyClientConfig, null);
    this.rpcHook = new DefaultRPCHookImpl();
    this.remotingClient.registerRPCHook(this.rpcHook);
    this.namerManagerProcessor = namerManagerProcessor;
    /**
     * from namer
     */
    this.remotingClient.registerProcessor(RequestCode.SYNC_PIPER, this.namerManagerProcessor, this.namerManagerExecutor);
    this.remotingClient.registerProcessor(RequestCode.LISTEN_REDIS, this.namerManagerProcessor, this.namerManagerExecutor);
    this.remotingClient.registerProcessor(RequestCode.GET_PIPER_TASK_INFO, this.namerManagerProcessor, this.namerManagerExecutor);
    this.remotingClient.registerProcessor(RequestCode.GET_PIPER_RUNNING_INFO, this.namerManagerProcessor, this.namerManagerExecutor);
  }

  public void start() {
    this.rpcHook.start();
    this.remotingClient.start();
  }

  public void stop() {
    this.rpcHook.stop();
    this.remotingClient.shutdown();
  }

  public void updateNamerLocation(final String addrs) {
    List<String> list = new ArrayList<String>();
    String[] arrdArray = addrs.split(",");
    for (String addr : arrdArray) {
      list.add(addr);
    }
    this.remotingClient.updateNameServerAddressList(list);
  }



  public RegisterPiperResult registerPiper(final PiperTaskData piperTaskData,
                                           final String group,
                                           final String location,
                                           final int piperId,
                                           final String haServerLocation,
                                           final boolean oneway,
                                           final int timeoutMills) {
    RegisterPiperResult registerBrokerResult = null;

    List<String> namerLocatoinList = this.remotingClient.getNamerLocatoinList();
    if (namerLocatoinList != null) {
      for (String namerLocation : namerLocatoinList) {
        try {
          RegisterPiperResult result = this.registerPiper(piperTaskData, group, namerLocation, location, piperId,
                  haServerLocation, oneway, timeoutMills);
          if (result != null) {
            registerBrokerResult = result;
          }

          log.info("register piper to namer server {} OK", namerLocation);
        } catch (Exception e) {
          log.warn("registerPiper Exception, {}", namerLocation, e);
        }
      }
    }
    return registerBrokerResult;
  }

  private RegisterPiperResult registerPiper(final PiperTaskData piperTaskData,
                                            final String group,
                                            final String namerLocation,
                                            final String location,
                                            final int piperId,
                                            final String haServerLocation,
                                            final boolean oneway,
                                            final int timeoutMills
  ) throws RemotingCommandException, PiperException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException,
          InterruptedException {
    RegisterPiperRequestHeader requestHeader = new RegisterPiperRequestHeader();
    requestHeader.setGroup(group);
    requestHeader.setLocation(location);
    requestHeader.setPiperId(piperId);
    requestHeader.setHaServerAddr(haServerLocation);
    RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.REGISTER_PIPER, requestHeader);

    if (piperTaskData != null) {
      request.setBody(piperTaskData.encode());
    }
    if (oneway) {
      try {
        this.remotingClient.invokeOneway(namerLocation, request, timeoutMills);
      } catch (RemotingTooMuchRequestException e) {
        // Ignore
      }
      return null;
    }

    RemotingCommand response = this.remotingClient.invokeSync(namerLocation, request, timeoutMills);
    assert response != null;
    switch (response.getCode()) {
      case ResponseCode.SUCCESS: {
        RegisterPiperResponseHeader responseHeader =
                (RegisterPiperResponseHeader) response.decodeCommandCustomHeader(RegisterPiperResponseHeader.class);
        RegisterPiperResult result = new RegisterPiperResult();
        result.setMasterAddr(responseHeader.getMasterAddr());
        result.setHaServerAddr(responseHeader.getHaServerAddr());
        return result;
      }
      default:
        break;
    }

    throw new PiperException(response.getCode(), response.getRemark());
  }


  public void unregisterPiper(final String group,
                              final String location,
                              final int piperId,
                              final int timeoutMills) {
    List<String> namerLocatoinList = this.remotingClient.getNamerLocatoinList();
    if (namerLocatoinList != null) {
      for (String namerLocation : namerLocatoinList) {
        try {
          this.unregisterPiper(group, namerLocation, location, piperId, timeoutMills);
          log.info("unregister piper to namer server {} OK", namerLocation);
        } catch (Exception e) {
          log.warn("unregister Exception, {}", namerLocation, e);
        }
      }
    }
  }

  public void unregisterPiper(final String group,
                              final String namerLocation,
                              final String location,
                              final int piperId,
                              final int timeoutMills) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, PiperException {
    UnRegisterPiperRequestHeader requestHeader = new UnRegisterPiperRequestHeader();
    requestHeader.setGroup(group);
    requestHeader.setLocation(location);
    requestHeader.setPiperId(piperId);

    RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UNREGISTER_PIPER, requestHeader);
    RemotingCommand response = this.remotingClient.invokeSync(namerLocation, request, timeoutMills);
    assert response != null;
    switch (response.getCode()) {
      case ResponseCode.SUCCESS:
        return;

      default:
        break;
    }
    throw new PiperException(response.getCode(), response.getRemark());
  }

  public PiperAllData updatePiperInfoFromNamer(String namerLocation, String location, final long timeoutMillis)
      throws PiperException, RemotingException, InterruptedException {
    GetPipersFromNamerRequestHeader requestHeader = new GetPipersFromNamerRequestHeader();
    requestHeader.setSrcLocation(location);
    RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_PIPER_INFO_FROM_NAMER, requestHeader);
    RemotingCommand response = this.remotingClient.invokeSync(namerLocation, request, timeoutMillis);
    assert response != null;
    switch (response.getCode()) {
      case ResponseCode.SUCCESS:
        byte[] body = response.getBody();
        if (body != null) {
          return PiperAllData.decode(body, PiperAllData.class);
        }
        break;
    }
    throw new PiperException(response.getCode(), response.getRemark());
  }

  public RemotingCommand getPiperInfo(String syncPiperLocation, final long timeoutMillis)
      throws PiperException, RemotingException, InterruptedException {

    GetPiperInfoRequestHeader requestHeader = new GetPiperInfoRequestHeader();
    RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_PIPER_INFO_BETWEEN_PIPER, requestHeader);
    RemotingCommand response = this.remotingClient.invokeSync(syncPiperLocation, request, timeoutMillis);
    assert response != null;
    switch (response.getCode()) {
      case ResponseCode.SUCCESS:
        return response;
      default:
        break;
    }
    throw new PiperException(response.getCode(), response.getRemark());
  }

  public PullResult pullCommand(final String addr, final PullMessageRequestHeader requestHeader,
                                final long timeoutMillis, final CommunicationMode communicationMode,
                                final PullCallback pullCallback)
          throws PiperException, RemotingException,  InterruptedException {
    RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.PULL_MESSAGE, requestHeader);
    switch (communicationMode) {
      case SYNC:
        return this.pullCommandSync(addr, request, timeoutMillis);
      case ASYNC:
        this.pullCommandAsync(addr, request, timeoutMillis, pullCallback);
        return null;
        default:
          assert false;
    }
    return null;
  }

  private void pullCommandAsync(final String addr, final RemotingCommand request, final long timeoutMillis,
                                final PullCallback pullCallback) throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException, RemotingTooMuchRequestException, RemotingConnectException {

    this.remotingClient.invokeAsync(addr, request, timeoutMillis, (responseFuture) -> {
      RemotingCommand response = responseFuture.getResponseCommand();
      if (response != null) {
        try {
          PullResult pullResult = PiperClientOuterImpl.this.processPullResponse(response);
          assert pullResult != null;
          pullCallback.OnSuccess(pullResult);
        } catch (Exception e) {
          pullCallback.onException(e, RequestExceptionReason.OTHER);
        }

      } else {
        RequestExceptionReason reason = RequestExceptionReason.OTHER;
        if (!responseFuture.isSendRequestOK()) {
          reason = RequestExceptionReason.SEND_ERROR;
          pullCallback.onException(new PiperException("send request failed to " + addr + ". Request:" + request, responseFuture.getCause()), reason);
        } else if (responseFuture.isTimeout()) {
          reason = RequestExceptionReason.TIME_OUT;
          pullCallback.onException(new PiperException("wait response from " + addr + " timeout :" + responseFuture.getTimeoutMillis() + "ms" + ". Request:" + request,
                  responseFuture.getCause()), reason);
        } else {
          pullCallback.onException(new PiperException("unknow reason. addr: " + addr + ", timeoutMillis:" + timeoutMillis + ". Request:" + request,
                  responseFuture.getCause()), reason);
        }
      }
    });
  }


  private PullResult pullCommandSync(final String addr, final RemotingCommand request, final long timeoutMillis)
    throws PiperException, RemotingException, InterruptedException {
    RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
    assert response != null;
    return processPullResponse(response);
  }

  private PullResult processPullResponse(final RemotingCommand response) throws PiperException, RemotingCommandException {
    PullStatus pullStatus = PullStatus.NO_NEW_MSG;
    switch (response.getCode()) {
      case ResponseCode.SUCCESS:
        pullStatus = PullStatus.FOUND;
        break;
      case ResponseCode.PULL_NOT_FOUND:
        pullStatus = PullStatus.NO_NEW_MSG;
        break;
      case ResponseCode.PULL_OFFSET_MOVED:
        pullStatus = PullStatus.OFFSET_ILLEGAL;
        break;
        default:
          throw new PiperException(response.getCode(), response.getRemark());
    }
    PullMessageResponseHeader responseHeader = (PullMessageResponseHeader) response.decodeCommandCustomHeader(PullMessageResponseHeader.class);
    return new PullResultExt(pullStatus, responseHeader.getNextBeginOffset(), responseHeader.getMinOffset(),
            responseHeader.getMaxOffset(), null, response.getBody());
  }

  public RemotingClient getRemotingClient() {
    return remotingClient;
  }
}
