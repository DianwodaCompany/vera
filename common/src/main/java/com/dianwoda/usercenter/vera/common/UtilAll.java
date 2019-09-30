package com.dianwoda.usercenter.vera.common;

import com.dianwoda.usercenter.vera.remoting.common.RemotingHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * @author seam
 */
public class UtilAll {

  public static String offset2FileName(final long offset) {
    final NumberFormat nf = NumberFormat.getInstance();
    nf.setMinimumIntegerDigits(20);
    nf.setMaximumFractionDigits(0);
    nf.setGroupingUsed(false);
    return nf.format(offset);
  }

  public static long computeEclipseTimeMilliseconds(final long beginTime) {
    return System.currentTimeMillis() - beginTime;
  }

  public static String jstack(Map<Thread, StackTraceElement[]> map) {
    StringBuilder result = new StringBuilder();
    try {
      Iterator<Map.Entry<Thread, StackTraceElement[]>> ite = map.entrySet().iterator();
      while (ite.hasNext()) {
        Map.Entry<Thread, StackTraceElement[]> entry = ite.next();
        StackTraceElement[] elements = entry.getValue();
        Thread thread = entry.getKey();
        if (elements != null && elements.length > 0) {
          String threadName = entry.getKey().getName();
          result.append(String.format("%-40sTID: %d STATE: %s%n", threadName, thread.getId(), thread.getState()));
          for (StackTraceElement el : elements) {
            result.append(String.format("%-40s%s%n", threadName, el.toString()));
          }
          result.append("\n");
        }
      }
    } catch (Throwable e) {
      result.append(RemotingHelper.exceptionSimpleDesc(e));
    }

    return result.toString();
  }
  public static byte[] translate(int data) {
    byte ret[] = new byte[4];
    ret[0] = (byte)((data >>> 24) & 0xFF);
    ret[1] = (byte)((data >>> 16) & 0xFF);
    ret[2] = (byte)((data >>> 8)  & 0xFF);
    ret[3] = (byte)((data >>> 0)  & 0xFF);
    return ret;
  }

  public static int translate(byte[] data) {
     int ret = ((data[0] << 24) & 0xFF000000) + ((data[1] << 16) & 0x00FF0000) +
             ((data[2] << 8) & 0x0000FF00) + ((data[3] << 0) & 0x000000FF);
    return ret;
  }

  public static String responseCode2String(final int code) {
    return Integer.toString(code);
  }

  public static String file2String(final String fileName) throws IOException {
    File file = new File(fileName);
    return file2String(file);
  }

  public static String file2String(final File file) throws IOException {
    if (file.exists()) {
      byte[] data = new byte[(int) file.length()];
      boolean result;

      FileInputStream inputStream = null;
      try {
        inputStream = new FileInputStream(file);
        int len = inputStream.read(data);
        result = len == data.length;
      } finally {
        if (inputStream != null) {
          inputStream.close();
        }
      }

      if (result) {
        return new String(data);
      }
    }
    return null;
  }

  public static void string2File(final String str, final String fileName) throws IOException {

    String tmpFile = fileName + ".tmp";
    string2FileNotSafe(str, tmpFile);

    String bakFile = fileName + ".bak";
    String prevContent = file2String(fileName);
    if (prevContent != null) {
      string2FileNotSafe(prevContent, bakFile);
    }

    File file = new File(fileName);
    file.delete();

    file = new File(tmpFile);
    file.renameTo(new File(fileName));
  }

  public static void string2FileNotSafe(final String str, final String fileName) throws IOException {
    File file = new File(fileName);
    File fileParent = file.getParentFile();
    if (fileParent != null) {
      fileParent.mkdirs();
    }
    FileWriter fileWriter = null;

    try {
      fileWriter = new FileWriter(file);
      fileWriter.write(str);
    } catch (IOException e) {
      throw e;
    } finally {
      if (fileWriter != null) {
        fileWriter.close();
      }
    }
  }

  public static String timeMillisToHumanString(final long t) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(t);
    return String.format("%04d%02d%02d%02d%02d%02d%03d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND),
            cal.get(Calendar.MILLISECOND));
  }

  public static String timeMillisToHumanString2(final long t) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(t);
    return String.format("%04d-%02d-%02d %02d:%02d:%02d,%03d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND),
            cal.get(Calendar.MILLISECOND));
  }

  public static double getDiskPartitionSpaceUsedPercent(final String path) {
    if (null == path || path.isEmpty())
      return -1;

    try {
      File file = new File(path);
      if (!file.exists())
        return -1;

      long totalSpace = file.getTotalSpace();
      if (totalSpace > 0) {
        long freeSpace = file.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;

        return usedSpace / (double) totalSpace;
      }
    } catch (Exception e) {
      return -1;
    }
    return -1;
  }

  public static long computNextMorningTimeMillis() {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(System.currentTimeMillis());
    cal.add(Calendar.DAY_OF_MONTH, 1);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    return cal.getTimeInMillis();
  }

  public static long computNextMinutesTimeMillis() {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(System.currentTimeMillis());
    cal.add(Calendar.DAY_OF_MONTH, 0);
    cal.add(Calendar.HOUR_OF_DAY, 0);
    cal.add(Calendar.MINUTE, 1);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    return cal.getTimeInMillis();
  }

  public static long computNextHourTimeMillis() {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(System.currentTimeMillis());
    cal.add(Calendar.DAY_OF_MONTH, 0);
    cal.add(Calendar.HOUR_OF_DAY, 1);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    return cal.getTimeInMillis();
  }

  public static int crc32(byte[] array) {
    if (array != null) {
      return crc32(array, 0, array.length);
    }

    return 0;
  }

  public static int crc32(byte[] array, int offset, int length) {
    CRC32 crc32 = new CRC32();
    crc32.update(array, offset, length);
    return (int) (crc32.getValue() & 0x7FFFFFFF);
  }

  public static boolean isItTimeToDo(final String when) {
    String[] whiles = when.split(";");
    if (whiles.length > 0) {
      Calendar now = Calendar.getInstance();
      for (String w : whiles) {
        int nowHour = Integer.parseInt(w);
        if (nowHour == now.get(Calendar.HOUR_OF_DAY)) {
          return true;
        }
      }
    }
    return false;
  }
}
