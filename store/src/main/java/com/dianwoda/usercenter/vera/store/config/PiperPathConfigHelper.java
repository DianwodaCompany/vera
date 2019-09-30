package com.dianwoda.usercenter.vera.store.config;

import java.io.File;

/**
 * @author seam
 */
public class PiperPathConfigHelper {
  public static String getConsumerOffsetPath(final String rootDir) {
    return rootDir + File.separator + "config" + File.separator + "consumerOffset.json";
  }

  public static String getAbortFile(final String rootDir) {
    return rootDir + File.separator + "abort";
  }

  public static String getStoreCheckpoint(final String rootDir) {
    return rootDir + File.separator + "checkpoint";
  }
}
