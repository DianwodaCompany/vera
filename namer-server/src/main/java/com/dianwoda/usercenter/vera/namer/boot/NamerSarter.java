package com.dianwoda.usercenter.vera.namer.boot;

import com.dianwoda.usercenter.vera.common.ShutdownHookThread;
import com.dianwoda.usercenter.vera.namer.NamerController;
import com.dianwoda.usercenter.vera.namer.NamerFactory;
import com.dianwoda.usercenter.vera.namer.config.NamerConfig;
import com.dianwoda.usercenter.vera.namer.config.NamerConfigKey;
import com.dianwoda.usercenter.vera.remoting.netty.NettyServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * @author seam
 */
@Component
public class NamerSarter implements CommandLineRunner, EnvironmentAware {
  final Logger log = LoggerFactory.getLogger(NamerSarter.class);
  private NamerController namerController;

  @Override
  public void run(String... args) throws Exception {

    if (!this.namerController.initialize()) {
      namerController.shutdown();
      System.exit(-3);
    }

    Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(log, new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        namerController.shutdown();
        return null;
      }
    }));

    this.namerController.start();
  }

  @Override
  public void setEnvironment(Environment environment) {
    log.info("environment:" + environment);
    final NamerConfig namerConfig = new NamerConfig(environment);

    final NettyServerConfig nettyServerConfig = new NettyServerConfig();
    nettyServerConfig.setListenPort(namerConfig.getParam(NamerConfigKey.NAMER_NAMER_PORT, Integer.class));
    log.info("namerConfig:" + namerConfig);
    this.namerController = NamerFactory.make();
    this.namerController.setNamerConfig(namerConfig);
    this.namerController.setNettyServerConfig(nettyServerConfig);
  }
}
