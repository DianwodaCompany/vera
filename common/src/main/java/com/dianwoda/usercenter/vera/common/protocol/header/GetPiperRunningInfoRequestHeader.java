package com.dianwoda.usercenter.vera.common.protocol.header;

import com.dianwoda.usercenter.vera.remoting.CommandCustomHeader;
import com.dianwoda.usercenter.vera.remoting.annotation.CFNotNull;
import com.dianwoda.usercenter.vera.remoting.annotation.CFNullable;
import com.dianwoda.usercenter.vera.remoting.exception.RemotingCommandException;

/**
 * @author seam
 */
public class GetPiperRunningInfoRequestHeader implements CommandCustomHeader {
  @CFNullable
  private boolean jstackEnable;

  @Override
  public void checkFields() throws RemotingCommandException {
  }

  public boolean isJstackEnable() {
    return jstackEnable;
  }

  public void setJstackEnable(boolean jstackEnable) {
    this.jstackEnable = jstackEnable;
  }
}
