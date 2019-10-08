package com.dianwoda.usercenter.vera.piper.processor;

import com.dianwoda.usercenter.vera.common.PullSysFlag;
import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.message.GetCommandStatus;
import com.dianwoda.usercenter.vera.common.message.CommandDecoder;
import com.dianwoda.usercenter.vera.common.protocol.ResponseCode;
import com.dianwoda.usercenter.vera.common.protocol.header.PullMessageRequestHeader;
import com.dianwoda.usercenter.vera.common.protocol.header.PullMessageResponseHeader;
import com.dianwoda.usercenter.vera.piper.PiperController;
import com.dianwoda.usercenter.vera.piper.longpolling.PullRequest;
import com.dianwoda.usercenter.vera.remoting.common.RemotingHelper;
import com.dianwoda.usercenter.vera.remoting.exception.RemotingCommandException;
import com.dianwoda.usercenter.vera.remoting.netty.NettyRequestProcessor;
import com.dianwoda.usercenter.vera.remoting.netty.RequestTask;
import com.dianwoda.usercenter.vera.remoting.protocol.RemotingCommand;
import com.dianwoda.usercenter.vera.store.GetCommandResult;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * 对pull消息的实现
 * @author seam
 */
public class PullMessageProcessor implements NettyRequestProcessor {
  protected static final Logger log = LoggerFactory.getLogger(PullMessageProcessor.class);
  private PiperController piper;
  private long pollingTimeMills = 1000 * 2;

  public PullMessageProcessor(PiperController piper) {
    this.piper = piper;
  }


  public void executeRequestWhenWakeup(final Channel channel, final RemotingCommand request) {
    Runnable run = () -> {

      log.info("wakeup request:" + request);
      final RemotingCommand response;
      try {
        response = PullMessageProcessor.this.processRequest(channel, request, false);

        if (response != null) {
          try {
            channel.writeAndFlush(response).addListener(new ChannelFutureListener() {
              @Override
              public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                  log.error(String.format("process request wrapper response to %s failed",
                          future.channel().remoteAddress()), future.cause());
                  log.error(request.toString());
                  log.error(response.toString());
                }
              }
            });
          } catch (Throwable e) {
            log.error("processRequestWrapper process request over, but response failed", e);
            log.error(request.toString());
            log.error(response.toString());
          }
        }
      } catch (RemotingCommandException e) {
        e.printStackTrace();
      }
    };
    this.piper.getPullMessageExecutor().submit(new RequestTask(run, channel, request));
  }

  @Override
  public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws RemotingCommandException {
    log.info(String.format("receive request, code=%d %s %s",
            request.getCode(),
            RemotingHelper.parseChannelRemoteAddr(ctx.channel()), request));

    return this.processRequest(ctx.channel(), request, true);
  }

  private RemotingCommand processRequest(final Channel channel, RemotingCommand request, boolean allowSuspend)
          throws RemotingCommandException {
    RemotingCommand response = RemotingCommand.createResponseCommand(PullMessageResponseHeader.class);
    final PullMessageResponseHeader responseHeader = (PullMessageResponseHeader) response.readCustomHeader();
    final PullMessageRequestHeader requestHeader = (PullMessageRequestHeader) request.decodeCommandCustomHeader(PullMessageRequestHeader.class);
    response.setOpaque(request.getOpaque());

    GetCommandResult getCommandResult = this.piper.getCommandStore().getCommand(requestHeader.getLocation(),
            requestHeader.getReadOffset(), requestHeader.getMaxMsgNums());
    if (getCommandResult.getStatus() == GetCommandStatus.FOUND) {
      response.setCode(ResponseCode.SUCCESS);

    } else if (getCommandResult.getStatus() == GetCommandStatus.OFFSET_OVERFLOW_ONE ) {
      response.setCode(ResponseCode.PULL_NOT_FOUND);

    } else if (getCommandResult.getStatus() == GetCommandStatus.NO_MESSAGE_IN_BLOCK ) {
      response.setCode(ResponseCode.PULL_NOT_FOUND);

    } else if (getCommandResult.getStatus() == GetCommandStatus.OFFSET_TOO_SMALL) {
      response.setCode(ResponseCode.PULL_NOT_FOUND);
    } else if (getCommandResult.getStatus() == GetCommandStatus.OFFSET_FOUND_NULL) {
      response.setCode(ResponseCode.PULL_OFFSET_MOVED);
    } else {
      assert false;
    }

    final boolean hasSuspendFlag = PullSysFlag.hasSuspendFlag(requestHeader.getSysFlag());
    switch (response.getCode()) {
      case ResponseCode.SUCCESS:
        responseHeader.setMaxOffset(getCommandResult.getMaxOffset());
        responseHeader.setMinOffset(getCommandResult.getMinOffset());
        responseHeader.setNextBeginOffset(getCommandResult.getNextBeginOffset());

        final byte[] r = this.readGetMessageResult(getCommandResult, requestHeader.getLocation());
        response.setBody(r);
        break;
      case ResponseCode.PULL_NOT_FOUND:
        if (hasSuspendFlag) {
          PullRequest pullRequest = new PullRequest(request, channel, this.pollingTimeMills,
                  System.currentTimeMillis(), requestHeader.getReadOffset());
          this.piper.getPullRequestHoldService().suspendPullRequest(requestHeader.getLocation(), pullRequest);
          response = null;
          break;
        }
      case ResponseCode.PULL_OFFSET_MOVED:
        responseHeader.setMaxOffset(getCommandResult.getMaxOffset());
        responseHeader.setMinOffset(getCommandResult.getMinOffset());
        responseHeader.setNextBeginOffset(getCommandResult.getNextBeginOffset());
        break;
    }
    return response;
  }

  @Override
  public boolean rejectRequest() {
    return false;
  }


  private byte[] readGetMessageResult(final GetCommandResult getMessageResult, final String location) {
    final ByteBuffer byteBuffer = ByteBuffer.allocate(getMessageResult.getBufferTotalSize());
    long storeTimestamp = 0;
    try {
      List<ByteBuffer> messageBufferList = getMessageResult.getCommandBufferList();
      for (ByteBuffer bb : messageBufferList) {
        byteBuffer.put(bb);
        storeTimestamp = bb.getLong(CommandDecoder.STORE_TIMESTAMP_POSITION);
      }
    } finally {
      getMessageResult.release();
    }

    this.piper.getPiperStatsManager().recordDiskFallBehindTime(location, SystemClock.now() - storeTimestamp);
    return byteBuffer.array();
  }
}
