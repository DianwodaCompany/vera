package com.dianwoda.usercenter.vera.common.message;

/**
 * @author seam
 */
public class Command {

  /**
   * total size             4   byte
   * magic code             4   byte
   * crc32                  4   byte
   * store timestamp        8   byte
   * store offset           8   byte
   * logic offset           8   byte
   * data  length           4   byte
   * data                   不定长
   */

  private int totalSize;
  private int magicCode;
  private int crc;
  private long storeTimestamp;
  private long storeOffset;
  private long logicOffset;
  private int dataLength = 0;
  private byte[] data;

  public int getTotalSize() {
    return totalSize;
  }

  public void setTotalSize(int totalSize) {
    this.totalSize = totalSize;
  }

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

  public int getMagicCode() {
    return magicCode;
  }

  public void setMagicCode(int magicCode) {
    this.magicCode = magicCode;
  }

  public void setDataLength(int dataLength) {
    this.dataLength = dataLength;
  }


  public long getLogicOffset() {
    return logicOffset;
  }

  public void setLogicOffset(long logicOffset) {
    this.logicOffset = logicOffset;
  }

  public long getStoreOffset() {
    return storeOffset;
  }

  public void setStoreOffset(long storeOffset) {
    this.storeOffset = storeOffset;
  }
}
