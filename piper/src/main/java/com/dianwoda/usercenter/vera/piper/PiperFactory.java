package com.dianwoda.usercenter.vera.piper;

import com.dianwoda.usercenter.vera.piper.config.PiperConfig;

/**
 * @author seam
 */
public class PiperFactory {

  public static PiperController make(PiperConfig piperConfig) {
    return new PiperController(piperConfig);
  }

}
