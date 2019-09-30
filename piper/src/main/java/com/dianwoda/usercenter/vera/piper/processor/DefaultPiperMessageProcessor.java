package com.dianwoda.usercenter.vera.piper.processor;

import com.dianwoda.usercenter.vera.common.protocol.RequestCode;
import com.dianwoda.usercenter.vera.common.protocol.ResponseCode;
import com.dianwoda.usercenter.vera.common.protocol.header.GetPiperInfoResponseHeader;
import com.dianwoda.usercenter.vera.piper.PiperController;
import com.dianwoda.usercenter.vera.remoting.common.RemotingHelper;
import com.dianwoda.usercenter.vera.remoting.exception.RemotingCommandException;
import com.dianwoda.usercenter.vera.remoting.netty.NettyRequestProcessor;
import com.dianwoda.usercenter.vera.remoting.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 对来自piper间消息消费的默认实现
 * @author seam
 */
public class DefaultPiperMessageProcessor implements NettyRequestProcessor {
  protected static final Logger log = LoggerFactory.getLogger(DefaultPiperMessageProcessor.class);
  private PiperController piperController;

  public DefaultPiperMessageProcessor(PiperController controller) {
    this.piperController = controller;
  }

  @Override
  public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws RemotingCommandException {
    log.info(String.format("receive request, %d %s %s",
            request.getCode(),
            RemotingHelper.parseChannelRemoteAddr(ctx.channel()),
            request));

    switch (request.getCode()) {
      case RequestCode.GET_PIPER_INFO_BETWEEN_PIPER:
        return this.getPiperInfo(ctx, request);

      default:
        break;
    }
    return null;
  }

  public RemotingCommand getPiperInfo(ChannelHandlerContext ctx, RemotingCommand request) {
    RemotingCommand response = RemotingCommand.createResponseCommand(GetPiperInfoResponseHeader.class);
    final GetPiperInfoResponseHeader responseHeader = (GetPiperInfoResponseHeader) response.readCustomHeader();
    long maxWriteOffset = piperController.getCommandStore().getMaxWriteOffset();
    responseHeader.setMaxWriteOffset(maxWriteOffset);
    response.setCode(ResponseCode.SUCCESS);
    return response;
  }

  @Override
  public boolean rejectRequest() {
    return false;
  }
}
