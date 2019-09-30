package com.dianwoda.usercenter.vera.store;


import com.dianwoda.usercenter.vera.common.message.GetCommandStatus;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author seam
 */
public class GetCommandResult {
  private final List<SelectMappedBufferResult> commandMappedList = new ArrayList<SelectMappedBufferResult>(100);
  private final List<ByteBuffer> commandBufferList = new ArrayList<>(100);
  private GetCommandStatus status;
  private long nextBeginOffset;
  private long minOffset;
  private long maxOffset;
  private int bufferTotalSize = 0;

  public void addCommand(final SelectMappedBufferResult selectMappedBufferResult) {
    this.commandMappedList.add(selectMappedBufferResult);
    this.commandBufferList.add(selectMappedBufferResult.getByteBuffer());
    this.bufferTotalSize += selectMappedBufferResult.getSize();
  }

  public long getNextBeginOffset() {
    return nextBeginOffset;
  }

  public void setNextBeginOffset(long nextBeginOffset) {
    this.nextBeginOffset = nextBeginOffset;
  }

  public long getMinOffset() {
    return minOffset;
  }

  public void setMinOffset(long minOffset) {
    this.minOffset = minOffset;
  }

  public long getMaxOffset() {
    return maxOffset;
  }

  public void setMaxOffset(long maxOffset) {
    this.maxOffset = maxOffset;
  }


  public GetCommandStatus getStatus() {
    return status;
  }

  public void setStatus(GetCommandStatus status) {
    this.status = status;
  }

  public List<SelectMappedBufferResult> getCommandMappedList() {
    return commandMappedList;
  }

  public List<ByteBuffer> getCommandBufferList() {
    return commandBufferList;
  }

  public int getBufferTotalSize() {
    return bufferTotalSize;
  }

  public void release() {
    for (SelectMappedBufferResult select : this.commandMappedList) {
      select.release();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("GetCommandResult ( Commands:" + this.getCommandMappedList());
    sb.append(" GetCommandStatus: " + this.status.name());
    sb.append(" nextBeginOffset:" + this.nextBeginOffset);
    sb.append(" minOffset:" + this.minOffset);
    sb.append(" maxOffset:" + this.maxOffset + " )");
    return sb.toString();
  }
}
