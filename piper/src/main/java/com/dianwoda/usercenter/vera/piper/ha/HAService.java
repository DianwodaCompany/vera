package com.dianwoda.usercenter.vera.piper.ha;

import com.dianwoda.usercenter.vera.piper.PiperController;
import com.dianwoda.usercenter.vera.piper.config.PiperConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author seam
 */
public class HAService {
  protected static final Logger log = LoggerFactory.getLogger(HAService.class);
  private HAClient haClient;
  private HAServer haServer;

  public HAService(final PiperController piperController, final PiperConfig piperConfig) {
    this.haClient = new HAClient(piperController, piperConfig);
    this.haServer = new HAServer(piperController, piperConfig);

  }

  public void start() {
    log.info("HAService starting");
    try {
      this.haServer.start();
    } catch (Exception e) {
      log.error("HAService start error", e);
    }
    this.haClient.start();
    log.info("HAService start success");
  }
}
