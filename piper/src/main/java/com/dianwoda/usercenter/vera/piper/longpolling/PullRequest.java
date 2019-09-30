package com.dianwoda.usercenter.vera.piper.longpolling;

import com.dianwoda.usercenter.vera.remoting.protocol.RemotingCommand;
import io.netty.channel.Channel;

/**
 * @author seam
 */
public class PullRequest {
  private final RemotingCommand requestCommand;
  private final Channel clientChannel;
  private final long timeoutMillis;
  private final long suspendTimestamp;
  private final long pullFromThisOffset;

  public PullRequest(RemotingCommand requestCommand, Channel clientChannel, long timeoutMillis,
                     long suspendTimestamp, long pullFromThisOffset) {
    this.requestCommand = requestCommand;
    this.clientChannel = clientChannel;
    this.timeoutMillis = timeoutMillis;
    this.suspendTimestamp = suspendTimestamp;
    this.pullFromThisOffset = pullFromThisOffset;
  }

  public RemotingCommand getRequestCommand() {
    return requestCommand;
  }

  public Channel getClientChannel() {
    return clientChannel;
  }

  public long getTimeoutMillis() {
    return timeoutMillis;
  }

  public long getSuspendTimestamp() {
    return suspendTimestamp;
  }

  public long getPullFromThisOffset() {
    return pullFromThisOffset;
  }
}
