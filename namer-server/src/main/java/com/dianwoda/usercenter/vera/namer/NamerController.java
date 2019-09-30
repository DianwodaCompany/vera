package com.dianwoda.usercenter.vera.namer;

import com.dianwoda.usercenter.vera.namer.processor.DefaultRequestProcessor;
import com.dianwoda.usercenter.vera.namer.routeinfo.PiperHousekeepingService;
import com.dianwoda.usercenter.vera.remoting.RemotingServer;
import com.dianwoda.usercenter.vera.common.ThreadFactoryImpl;
import com.dianwoda.usercenter.vera.namer.boot.AppContext;
import com.dianwoda.usercenter.vera.namer.config.NamerConfig;

import com.dianwoda.usercenter.vera.namer.routeinfo.RouteInfoManager;
import com.dianwoda.usercenter.vera.remoting.netty.NettyRemotingServer;
import com.dianwoda.usercenter.vera.remoting.netty.NettyServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author seam
 */
public class NamerController {
  protected static final Logger log = LoggerFactory.getLogger(NamerController.class);

  private NamerConfig namerConfig;
  private NettyServerConfig nettyServerConfig;
  private RouteInfoManager routeInfoManager;
  private RemotingServer remotingServer;
  private PiperHousekeepingService piperHousekeepingService;
  private ExecutorService remotingExecutor;
  private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl(
          "NamerScheduledThread"));

  public NamerController() {

    this.routeInfoManager = RouteInfoManager.getIntance();
    this.piperHousekeepingService = new PiperHousekeepingService(this);
  }

  public void setNamerConfig(NamerConfig namerConfig) {
    this.namerConfig = namerConfig;
  }

  public void setNettyServerConfig(NettyServerConfig nettyServerConfig) {
    this.nettyServerConfig = nettyServerConfig;
  }

  public boolean initialize() {
    this.remotingServer = new NettyRemotingServer(this.nettyServerConfig, this.piperHousekeepingService);
    this.remotingExecutor =
            Executors.newFixedThreadPool(nettyServerConfig.getServerWorkerThreads(), new ThreadFactoryImpl("NamerExecutorThread_"));

    this.registerProcessor();

    this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        NamerController.this.routeInfoManager.scanNotActivePiper();
      }
    }, 5, 10, TimeUnit.SECONDS);
    return true;
  }

  private void registerProcessor() {
    this.remotingServer.registerDefaultProcessor(new DefaultRequestProcessor(this), this.remotingExecutor);
  }

  public void start() {
    this.remotingServer.start();
  }

  public void shutdown() {
    this.remotingServer.shutdown();
    this.remotingExecutor.shutdown();
    this.scheduledExecutorService.shutdown();
  }

  public RouteInfoManager getRouteInfoManager() {
    return routeInfoManager;
  }

  public RemotingServer getRemotingServer() {
    return remotingServer;
  }
}
