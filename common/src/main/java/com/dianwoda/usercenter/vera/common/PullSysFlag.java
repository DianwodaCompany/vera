package com.dianwoda.usercenter.vera.common;

/**
 * @author seam
 */
public class PullSysFlag {
  private final static int FLAG_SUSPEND = 0x1 << 0;

  public static int buildPullSysFlag(final boolean suspend) {
    int flag = 0;
    if (suspend) {
      flag |= FLAG_SUSPEND;
    }
    return flag;
  }

  public static int setSuspend(int flag, final boolean suspend) {
    if (suspend) {
      flag |= FLAG_SUSPEND;
    }
    return flag;
  }

  public static int clearSuspendFlag(final int flag) {
    return flag & (~FLAG_SUSPEND);
  }

  public static boolean hasSuspendFlag(final int flag) {
    return (flag & FLAG_SUSPEND) == FLAG_SUSPEND;
  }
}
