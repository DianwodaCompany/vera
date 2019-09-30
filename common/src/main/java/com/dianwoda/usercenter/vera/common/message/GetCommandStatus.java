package com.dianwoda.usercenter.vera.common.message;

import java.io.Serializable;

/**
 * @author seam
 */
public enum GetCommandStatus implements Serializable {
  FOUND,

  OFFSET_FOUND_NULL,

  OFFSET_OVERFLOW_BADLY,

  OFFSET_OVERFLOW_ONE,

  OFFSET_TOO_SMALL,

  NO_MESSAGE_IN_BLOCK,
}
