package com.dianwoda.usercenter.vera.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author seam
 */
public class IPUtil {

  private static Logger logger = LoggerFactory.getLogger(IPUtil.class);

  /**
   * 获取本机IP地址
   * @return
   */
  public static String getServerIp(){
    try{
      return NetUtils.getLocalHost(); //获取本机ip
    }catch (Exception e){
      logger.error("获取IP地址出错", e);
    }
    return null;
  }
}
