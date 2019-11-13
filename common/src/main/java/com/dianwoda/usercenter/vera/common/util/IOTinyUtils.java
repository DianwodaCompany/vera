package com.dianwoda.usercenter.vera.common.util;

import com.dianwoda.usercenter.vera.remoting.common.RemotingHelper;

import java.io.*;

/**
 * @author seam
 */
public class IOTinyUtils {

  static public String toString(InputStream input, String encoding) throws IOException {
    return (null == encoding) ? toString(new InputStreamReader(input, RemotingHelper.DEFAULT_CHARSET)) : toString(new InputStreamReader(
            input, encoding));
  }

  static public String toString(Reader reader) throws IOException {
    CharArrayWriter sw = new CharArrayWriter();
    copy(reader, sw);
    return sw.toString();
  }

  static public long copy(Reader input, Writer output) throws IOException {
    char[] buffer = new char[1 << 12];
    long count = 0;
    for (int n = 0; (n = input.read(buffer)) >= 0; ) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }
}
