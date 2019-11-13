package com.dianwoda.usercenter.vera.piper.util;

import com.dianwoda.usercenter.vera.common.VeraVersion;
import com.dianwoda.usercenter.vera.common.util.IOTinyUtils;
import com.dianwoda.usercenter.vera.piper.client.PiperClientOuterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

/**
 * @author seam
 */
public class MixUtil {
  protected static final Logger log = LoggerFactory.getLogger(MixUtil.class);

  public static String fetchNamerServerAddr(String namerServerHttp, int timeoutMills) {
    String url = "http://" + namerServerHttp + "/config/getNamerAddrs";
    String encoding = "UTF-8";
    HttpURLConnection conn = null;

    try {
      conn = (HttpURLConnection) new URL(url).openConnection();
      conn.setRequestMethod("GET");
      conn.setConnectTimeout((int) timeoutMills);
      conn.setReadTimeout((int) timeoutMills);
      setHeaders(conn, encoding);

      conn.connect();
      int respCode = conn.getResponseCode();
      String resp = null;

      if (HttpURLConnection.HTTP_OK == respCode) {
        resp = IOTinyUtils.toString(conn.getInputStream(), encoding);
      } else {
        resp = IOTinyUtils.toString(conn.getErrorStream(), encoding);
      }

      if (respCode == 200) {
        if (resp != null) {
          return resp;
        }
      } else {
        log.error("fetch name server address failed. statusCode=" + respCode);
      }

    } catch (Exception e) {
      log.error("fetch name server address exception, url:" + url, e);
    }
    return null;
  }

  static private void setHeaders(HttpURLConnection conn, String encoding) {
    conn.addRequestProperty("Client-Version", VeraVersion.getVersionDesc(VeraVersion.CURRENT_VERSION));
    conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + encoding);

    String ts = String.valueOf(System.currentTimeMillis());
    conn.addRequestProperty("Metaq-Client-RequestTS", ts);
  }
}
