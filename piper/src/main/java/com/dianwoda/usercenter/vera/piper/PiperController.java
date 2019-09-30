package com.dianwoda.usercenter.vera.piper;

import com.dianwoda.usercenter.vera.common.ThreadFactoryImpl;
import com.dianwoda.usercenter.vera.common.protocol.body.RegisterPiperResult;
import com.dianwoda.usercenter.vera.common.protocol.RequestCode;
import com.dianwoda.usercenter.vera.common.protocol.hearbeat.PiperTaskData;
import com.dianwoda.usercenter.vera.piper.client.CircleDisposeHandler;
import com.dianwoda.usercenter.vera.piper.client.DefaultCircleDisposeHandler;
import com.dianwoda.usercenter.vera.piper.client.listener.NotifyCommandArriveListener;
import com.dianwoda.usercenter.vera.piper.client.PiperClientInstance;
import com.dianwoda.usercenter.vera.piper.config.PiperConfig;
import com.dianwoda.usercenter.vera.piper.longpolling.PullRequestHoldService;
import com.dianwoda.usercenter.vera.piper.offset.ConsumerOffsetManager;
import com.dianwoda.usercenter.vera.piper.processor.DefaultPiperMessageProcessor;
import com.dianwoda.usercenter.vera.piper.processor.PullMessageProcessor;
import com.dianwoda.usercenter.vera.piper.service.ClientHousekeepingService;
import com.dianwoda.usercenter.vera.remoting.RPCHook;
import com.dianwoda.usercenter.vera.remoting.RemotingServer;
import com.dianwoda.usercenter.vera.remoting.netty.NettyClientConfig;
import com.dianwoda.usercenter.vera.remoting.netty.NettyRemotingServer;
import com.dianwoda.usercenter.vera.remoting.netty.NettyServerConfig;
import com.dianwoda.usercenter.vera.store.CommandStore;
import com.dianwoda.usercenter.vera.store.DefaultCommandStore;
import com.dianwoda.usercenter.vera.store.listener.CommandArrivingListener;
import com.dianwoda.usercenter.vera.store.stats.PiperStatsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * @author seam
 */
public class PiperController {
  protected static final Logger log = LoggerFactory.getLogger(PiperController.class);

  private PiperConfig piperConfig;
  private NettyServerConfig nettyServerConfig;
  private final PiperStatsManager piperStatsManager;
  private final ConsumerOffsetManager offsetManager;
  private final CommandStore commandStore;
  private final PullMessageProcessor pullMessageProcessor;
  private final DefaultPiperMessageProcessor defaultPiperMessageProcessor;
  private final PullRequestHoldService pullRequestHoldService;
  private final CommandArrivingListener commandArrivingListener;
  private RemotingServer remotingServer;
  private ClientHousekeepingService clientHousekeepingService;
  private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("PiperScheduledThread"));
  private ExecutorService pullMessageExecutor;
  private boolean updateMasterHAServerAddrPeriodically = false;
  private CircleDisposeHandler circleDisposeHandler;
  private PiperClientInstance piperClientInstance;

  public PiperController(PiperConfig piperConfig) {
    this.piperConfig = piperConfig;
    this.offsetManager = new ConsumerOffsetManager(piperConfig.storePath());
    this.pullRequestHoldService = new PullRequestHoldService(this);
    this.commandArrivingListener = new NotifyCommandArriveListener(this.pullRequestHoldService);
    this.piperStatsManager = new PiperStatsManager(this.piperConfig.group());
    this.commandStore = new DefaultCommandStore(piperConfig.storePath(), this.piperStatsManager, this.commandArrivingListener);
    this.clientHousekeepingService = new ClientHousekeepingService(this);
    this.pullMessageProcessor = new PullMessageProcessor(this);
    this.defaultPiperMessageProcessor = new DefaultPiperMessageProcessor(this);
    this.circleDisposeHandler = new DefaultCircleDisposeHandler();
    this.piperClientInstance = new PiperClientInstance(this, this.offsetManager);
  }

  public boolean initialize() {
    boolean result = this.offsetManager.load();
    result = result & this.commandStore.load();

    if (result) {
      this.remotingServer = new NettyRemotingServer(this.nettyServerConfig, this.clientHousekeepingService);
      RPCHook hook = new DefaultRPCHookImpl();
      this.remotingServer.registerRPCHook(hook);
      this.pullMessageExecutor = new ThreadPoolExecutor(
              16, 16, 1000 * 60, TimeUnit.MILLISECONDS,
              new LinkedBlockingQueue<Runnable>(100000), new ThreadFactoryImpl("PullMessageThread_"));
    }

    this.registerProcessor();

    this.scheduledExecutorService.scheduleAtFixedRate(() -> {
      this.offsetManager.persist();
    }, 1000*10, 1000*5, TimeUnit.MILLISECONDS);

    return result;
  }

  private void registerProcessor() {
    /**
     * from piper
     */
    this.remotingServer.registerProcessor(RequestCode.PULL_MESSAGE, this.pullMessageProcessor, this.pullMessageExecutor);
    this.remotingServer.registerProcessor(RequestCode.GET_PIPER_INFO_BETWEEN_PIPER, this.defaultPiperMessageProcessor, this.pullMessageExecutor);
  }

  public void start() {
    this.remotingServer.start();
    this.piperClientInstance.start();
    if (this.clientHousekeepingService != null) {
      this.clientHousekeepingService.start();
    }
    this.commandStore.start();
    this.registerPiper(false);
    this.circleDisposeHandler.start();

    this.scheduledExecutorService.scheduleAtFixedRate(() -> {
      PiperController.this.registerPiper(false);
    }, 1000*10, 1000 * 10, TimeUnit.MILLISECONDS);
  }

  public void shutdown() {
    this.remotingServer.shutdown();
    this.pullMessageExecutor.shutdown();
    this.piperClientInstance.stop();
    this.scheduledExecutorService.shutdown();
    if (this.clientHousekeepingService != null) {
      this.clientHousekeepingService.shutdown();
    }
    this.commandStore.stop();
    this.circleDisposeHandler.stop();

    this.unregisterPiper();
  }

  public void registerPiper(boolean oneway) {
    PiperTaskData piperTaskData = this.piperClientInstance.getPiperClientInterImpl().getPiperTaskInfo();
    RegisterPiperResult registerPiperResult = this.piperClientInstance.getPiperClientAPIImpl().registerPiper(piperTaskData,
            this.piperConfig.group(), this.piperConfig.location(), this.piperConfig.piperId(),
            getHAServerLocation(), oneway, piperConfig.getRegisterPiperTimeoutMills());

    if (registerPiperResult != null) {
      if (this.updateMasterHAServerAddrPeriodically && registerPiperResult.getHaServerAddr() != null) {
        // to do
      }
    }
  }

  public void unregisterPiper() {
    this.piperClientInstance.getPiperClientAPIImpl().unregisterPiper(this.piperConfig.group(), this.piperConfig.location(),
            this.piperConfig.piperId(), piperConfig.getUnregisterPiperTimeoutMills());
  }

  public PiperConfig getPiperConfig() {
    return piperConfig;
  }

  public void setNettyServerConfig(NettyServerConfig nettyServerConfig) {
    this.nettyServerConfig = nettyServerConfig;
  }

  public CommandStore getCommandStore() {
    return commandStore;
  }

  public PiperStatsManager getPiperStatsManager() {
    return piperStatsManager;
  }

  public PullMessageProcessor getPullMessageProcessor() {
    return pullMessageProcessor;
  }

  public PullRequestHoldService getPullRequestHoldService() {
    return pullRequestHoldService;
  }

  public ExecutorService getPullMessageExecutor() {
    return pullMessageExecutor;
  }

  public String getHAServerLocation() {
    return null;
  }

  public CircleDisposeHandler getCircleDisposeHandler() {
    return circleDisposeHandler;
  }

  public ConsumerOffsetManager getOffsetManager() {
    return offsetManager;
  }

  public PiperClientInstance getPiperClientInstance() {
    return piperClientInstance;
  }
}
