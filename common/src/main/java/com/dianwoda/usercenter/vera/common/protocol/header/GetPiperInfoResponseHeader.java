package com.dianwoda.usercenter.vera.common.protocol.header;

import com.dianwoda.usercenter.vera.remoting.CommandCustomHeader;
import com.dianwoda.usercenter.vera.remoting.annotation.CFNotNull;
import com.dianwoda.usercenter.vera.remoting.exception.RemotingCommandException;

/**
 * @author seam
 */
public class GetPiperInfoResponseHeader implements CommandCustomHeader {
  @CFNotNull
  private long maxWriteOffset;

  @Override
  public void checkFields() throws RemotingCommandException {
  }

  public long getMaxWriteOffset() {
    return maxWriteOffset;
  }

  public void setMaxWriteOffset(long maxWriteOffset) {
    this.maxWriteOffset = maxWriteOffset;
  }

  @Override
  public String toString() {
    return "GetPiperInfoResponseHeader [maxWriteOffset: " + maxWriteOffset + "]";
  }
}
