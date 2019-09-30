package com.dianwoda.usercenter.vera.common.protocol.header;

import com.dianwoda.usercenter.vera.remoting.CommandCustomHeader;
import com.dianwoda.usercenter.vera.remoting.annotation.CFNotNull;
import com.dianwoda.usercenter.vera.remoting.exception.RemotingCommandException;

/**
 * @author seam
 */
public class GetPiperInfoRequestHeader implements CommandCustomHeader {
  @Override
  public void checkFields() throws RemotingCommandException {

  }
}
