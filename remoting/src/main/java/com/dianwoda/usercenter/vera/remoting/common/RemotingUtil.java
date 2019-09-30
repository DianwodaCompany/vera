package com.dianwoda.usercenter.vera.remoting.common;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

/**
 * @author seam
 */
public class RemotingUtil {
  protected static final Logger log = LoggerFactory.getLogger(RemotingUtil.class);

  public static void closeChannel(Channel channel) {
    final String addrRemote = parseChannelRemoteAddr(channel);
    channel.close().addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        log.info("closeChannel: close the connection to remote address[{}] result: {}", addrRemote,
                future.isSuccess());
      }
    });

  }
  public static String parseChannelRemoteAddr(final Channel channel) {
    if (null == channel) {
      return "";
    }
    SocketAddress remote = channel.remoteAddress();
    final String addr = remote != null ? remote.toString() : "";

    if (addr.length() > 0) {
      int index = addr.lastIndexOf("/");
      if (index >= 0) {
        return addr.substring(index + 1);
      }

      return addr;
    }

    return "";
  }

  public static SocketAddress string2SocketAddress(final String addr) {
    String[] s = addr.split(":");
    InetSocketAddress isa = new InetSocketAddress(s[0], Integer.parseInt(s[1]));
    return isa;
  }

  public static String socketAddress2String(final SocketAddress addr) {
    StringBuilder sb = new StringBuilder();
    InetSocketAddress inetSocketAddress = (InetSocketAddress) addr;
    sb.append(inetSocketAddress.getAddress().getHostAddress());
    sb.append(":");
    sb.append(inetSocketAddress.getPort());
    return sb.toString();
  }

  public static SocketChannel connect(SocketAddress remote) {
    return connect(remote, 1000 * 5);
  }

  public static SocketChannel connect(SocketAddress remote, final int timeoutMillis) {
    SocketChannel sc = null;
    try {
      sc = SocketChannel.open();
      sc.configureBlocking(true);
      sc.socket().setSoLinger(false, -1);
      sc.socket().setTcpNoDelay(true);
      sc.socket().setReceiveBufferSize(1024 * 64);
      sc.socket().setSendBufferSize(1024 * 64);
      sc.socket().connect(remote, timeoutMillis);
      sc.configureBlocking(false);
      return sc;
    } catch (Exception e) {
      if (sc != null) {
        try {
          sc.close();
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    }

    return null;
  }

}
