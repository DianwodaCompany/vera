package com.dianwoda.usercenter.vera.piper.boot;

import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.piper.PiperController;
import com.dianwoda.usercenter.vera.piper.PiperFactory;
import com.dianwoda.usercenter.vera.piper.config.PiperConfig;
import com.dianwoda.usercenter.vera.piper.config.PiperConfigKey;
import com.dianwoda.usercenter.vera.remoting.netty.NettyClientConfig;
import com.dianwoda.usercenter.vera.remoting.netty.NettyServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author seam
 */
@Component
public class PiperSarter implements CommandLineRunner, EnvironmentAware {
  protected static final Logger log = LoggerFactory.getLogger(PiperSarter.class);

  private PiperController piper;
  private PiperConfig piperConfig;

  @Override
  public void run(String... args) throws Exception {
    log.info("PiperSarter starting running");

    final NettyServerConfig nettyServerConfig = new NettyServerConfig();
    nettyServerConfig.setListenPort(this.piperConfig.getParam(PiperConfigKey.PIPER_BIND_PORT, Integer.class));

    this.piper.setNettyServerConfig(nettyServerConfig);

    try {
      boolean initResult = this.piper.initialize();
      if (!initResult) {
        this.piper.shutdown();
        System.exit(-3);
      }

      Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
        private volatile boolean hasShutdown = false;
        private AtomicInteger shutdownTimes = new AtomicInteger(0);

        @Override
        public void run() {
          synchronized (this) {
            log.info("Shutdown hook was invoked, {}", this.shutdownTimes.incrementAndGet());
            if (!this.hasShutdown) {
              this.hasShutdown = true;
              long beginTime = System.currentTimeMillis();
              piper.shutdown();
              long consumingTimeTotal = SystemClock.now() - beginTime;
              log.info("Shutdown hook over, consuming total time(ms): " + consumingTimeTotal);
            }
          }
        }
      }, "ShutdownHook"));

      this.piper.start();
      String tip = "The piper[" + this.piper.getPiperConfig().location() + ", "
              + this.piper.getPiperConfig().group() + "] boot success. id=" + this.piper.getPiperConfig().piperId();

      if (null != piper.getPiperConfig().nameLocation()) {
        tip += " and name server is " + piper.getPiperConfig().nameLocation();
      }

      log.info(tip);
    } catch (Throwable e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.piperConfig = new PiperConfig(environment);
    this.piper = PiperFactory.make(this.piperConfig);
  }
}
