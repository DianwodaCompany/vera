package com.dianwoda.usercenter.vera.common.util;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * @author seam
 */
public class NetUtils {

  public static final String LOCALHOST = "127.0.0.1";
  public static final String ANYHOST = "0.0.0.0";
  private static final Logger logger = LoggerFactory.getLogger(NetUtils.class);
  private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");
  private static volatile InetAddress LOCAL_ADDRESS = null;

  private static boolean isValidAddress(InetAddress address) {
    if (address == null || address.isLoopbackAddress()) {
      return false;
    }
    String name = address.getHostAddress();
    return (name != null
            && !ANYHOST.equals(name)
            && !LOCALHOST.equals(name)
            && IP_PATTERN.matcher(name).matches());
  }

  public static String getLocalHost() {
    InetAddress address = getLocalAddress();
    return address == null ? LOCALHOST : address.getHostAddress();
  }

  public static InetAddress getLocalAddress() {
    if (LOCAL_ADDRESS != null) {
      return LOCAL_ADDRESS;
    }
    InetAddress localAddress = getLocalAddress0();
    LOCAL_ADDRESS = localAddress;
    return localAddress;
  }

  private static InetAddress getLocalAddress0() {
    InetAddress localAddress = null;
    try {
      localAddress = InetAddress.getLocalHost();
      if (isValidAddress(localAddress)) {
        return localAddress;
      }
    } catch (Throwable e) {
      logger.warn("Failed to retrieving ip address, " + e.getMessage(), e);
    }
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      if (interfaces != null) {
        while (interfaces.hasMoreElements()) {
          try {
            NetworkInterface network = interfaces.nextElement();
            Enumeration<InetAddress> addresses = network.getInetAddresses();
            if (addresses != null) {
              while (addresses.hasMoreElements()) {
                try {
                  InetAddress address = addresses.nextElement();
                  if (isValidAddress(address)) {
                    return address;
                  }
                } catch (Throwable e) {
                  logger.warn("Failed to retrieving ip address, " + e.getMessage(), e);
                }
              }
            }
          } catch (Throwable e) {
            logger.warn("Failed to retrieving ip address, " + e.getMessage(), e);
          }
        }
      }
    } catch (Throwable e) {
      logger.warn("Failed to retrieving ip address, " + e.getMessage(), e);
    }
    logger.error("Could not get local host ip address, will use 127.0.0.1 instead.");
    return localAddress;
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

  public static String getLocalHostname() {
    try {
      InetAddress addr = InetAddress.getLocalHost();
      return addr.getHostName();
    } catch (UnknownHostException var7) {
      try {
        Enumeration interfaces = NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
          NetworkInterface nic = (NetworkInterface) interfaces.nextElement();
          Enumeration addresses = nic.getInetAddresses();

          while (addresses.hasMoreElements()) {
            InetAddress address = (InetAddress) addresses.nextElement();
            if (!address.isLoopbackAddress()) {
              String hostname = address.getHostName();
              if (hostname != null) {
                return hostname;
              }
            }
          }
        }
      } catch (SocketException var6) {
        return "UNKNOWN_LOCALHOST";
      }

      return "UNKNOWN_LOCALHOST";
    }
  }
}
