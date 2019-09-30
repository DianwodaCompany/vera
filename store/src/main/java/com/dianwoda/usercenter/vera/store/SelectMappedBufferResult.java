package com.dianwoda.usercenter.vera.store;

import java.nio.ByteBuffer;

/**
 * @author seam
 */
public class SelectMappedBufferResult {

  private final long startOffset;
  private final ByteBuffer byteBuffer;
  private int size;
  private BlockFile blockFile;

  public SelectMappedBufferResult(ByteBuffer byteBuffer, long startOffset, int size, BlockFile blockFile) {
    this.startOffset = startOffset;
    this.byteBuffer = byteBuffer;
    this.size = size;
    this.blockFile = blockFile;
  }

  public ByteBuffer getByteBuffer() {
    return byteBuffer;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public long getStartOffset() {
    return startOffset;
  }

  public BlockFile getBlockFile() {
    return blockFile;
  }

  public synchronized void release() {
    if (this.blockFile != null) {
      this.blockFile.release();
      this.blockFile = null;
    }
  }
}
