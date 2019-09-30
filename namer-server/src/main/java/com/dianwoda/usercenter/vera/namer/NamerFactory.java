package com.dianwoda.usercenter.vera.namer;

/**
 * @author seam
 */
public class NamerFactory {
  private static NamerController namer = null;
  private static Object obj = new Object();

  public static NamerController make() {
    if (namer == null) {
      synchronized (obj) {
        if (namer == null) {
          namer = new NamerController();
        }
      }
    }
    return namer;
  }

}
