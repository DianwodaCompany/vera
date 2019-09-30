package com.dianwoda.usercenter.vera.namer.tools;

import com.dianwoda.usercenter.vera.namer.NamerController;
import com.dianwoda.usercenter.vera.remoting.exception.RemotingSendRequestException;
import com.dianwoda.usercenter.vera.remoting.exception.RemotingTimeoutException;
import com.dianwoda.usercenter.vera.remoting.protocol.RemotingCommand;
import io.netty.channel.Channel;

/**
 * @author seam
 */
public class PiperClient {
  private NamerController namerController;

  public PiperClient(NamerController namerController) {
    this.namerController = namerController;
  }

  public RemotingCommand callPiper(final Channel channel, final RemotingCommand request)
      throws RemotingTimeoutException, InterruptedException, RemotingSendRequestException {

    return this.namerController.getRemotingServer().invokeSync(channel, request, 10000);
  }
}
