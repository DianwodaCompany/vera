package com.dianwoda.usercenter.vera.store;

import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.protocol.Common;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * append command call back
 * @author seam
 */
public class DefaultAppendCommandCallBack implements AppendCommandCallBack {
  protected static final Logger log = LoggerFactory.getLogger(DefaultAppendCommandCallBack.class);
  private final ByteBuffer msgStoreItemMemory;
  private CommandStore commandStore;
  private static int FILE_END_MARGIN_LEFT = 4 + 4;

  public DefaultAppendCommandCallBack(CommandStore commandStore) {
    this.commandStore = commandStore;
    this.msgStoreItemMemory = ByteBuffer.allocate(DefaultCommandStore.MAX_SIZE);
  }

  /**
   * message format
   * length  4  byte
   * crc32   4  byte
   * body    不定长
   * @param fileFromOffset
   * @param byteBuffer
   * @param maxBlank
   * @param commandInter
   * @return
   */
  @Override
  public AppendCommandResult doAppend(long fileFromOffset, ByteBuffer byteBuffer, int maxBlank, CommandInter commandInter) {
    // PHY OFFSET
    long wroteOffset = fileFromOffset + byteBuffer.position();
    int totalSize = calculateCommandLength(commandInter.getDataLength());

    if (totalSize + FILE_END_MARGIN_LEFT > maxBlank) {
      resetByteBuffer(this.msgStoreItemMemory, maxBlank);
      // length
      this.msgStoreItemMemory.putInt(maxBlank);
      // magic code
      this.msgStoreItemMemory.putInt(Common.BLANK_MAGIC_CODE);
      // the remaining space may be any vale
      byteBuffer.put(this.msgStoreItemMemory.array(), 0, maxBlank);
      log.warn("message match end of file, maxBlank:" + maxBlank + ", totalSize:" + totalSize);
      return new AppendCommandResult(AppendCommandStatus.END_OF_FILE, wroteOffset, maxBlank, SystemClock.now());
    }

    resetByteBuffer(this.msgStoreItemMemory, totalSize);
    // 1 total size
    this.msgStoreItemMemory.putInt(totalSize);
    // 2 magic code
    this.msgStoreItemMemory.putInt(Common.MESSAGE_MAGIC_CODE);
    // 3 crc
    this.msgStoreItemMemory.putInt(commandInter.getCrc());
    // 4 store timestamp
    this.msgStoreItemMemory.putLong(commandInter.getStoreTimestamp());
    // 5 store offset
    this.msgStoreItemMemory.putLong(wroteOffset);
    // 6 logic offset
    this.msgStoreItemMemory.putLong(this.commandStore.getLogicOffsetMax().longValue());
    this.commandStore.getLogicOffsetMax().incrementAndGet();
    // 7 data length
    this.msgStoreItemMemory.putInt(commandInter.getDataLength());
    // 8 data
    this.msgStoreItemMemory.put(commandInter.getData());
    byteBuffer.put(this.msgStoreItemMemory.array(), 0, totalSize);
    return new AppendCommandResult(AppendCommandStatus.PUT_OK, wroteOffset, totalSize, SystemClock.now());
  }

  private void resetByteBuffer(final ByteBuffer byteBuffer, final int limit) {
    byteBuffer.flip();
    byteBuffer.limit(limit);
  }


  private int calculateCommandLength(int dataLength) {
    return    4     // 1 totalsize
            + 4     // 2 MAGICCODE
            + 4     // 3 BODYCRC
            + 8     // 4 STORETIMESTAMP
            + 8     // 5 store offset
            + 8     // 6 logic offset
            + 4 + dataLength;   // 7 data
  }
}
