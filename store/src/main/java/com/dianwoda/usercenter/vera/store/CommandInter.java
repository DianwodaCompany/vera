package com.dianwoda.usercenter.vera.store;

/**
 * command 内部监时封装
 * @author seam
 */
public class CommandInter {

  // length + magic code + crc + storetimestamp + datalength
  public static int END_FILE_MIN_BLANK_LENGTH_BYTE = 4 + 4 + 4 + 8 + 4;

  /**
   * crc32                  4   byte
   * store timestamp        8   byte
   * data  length           4   byte
   * data                   不定长
   */
  private int crc;
  private long storeTimestamp;
  private int dataLength = 0;
  private byte[] data;

  public int getCrc() {
    return crc;
  }

  public void setCrc(int crc) {
    this.crc = crc;
  }

  public long getStoreTimestamp() {
    return storeTimestamp;
  }

  public void setStoreTimestamp(long storeTimestamp) {
    this.storeTimestamp = storeTimestamp;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
    this.dataLength = this.data == null ? 0 : this.data.length;
  }

  public int getDataLength() {
    return this.dataLength;
  }


}
