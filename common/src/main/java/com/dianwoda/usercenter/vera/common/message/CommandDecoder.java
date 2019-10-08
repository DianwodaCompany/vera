package com.dianwoda.usercenter.vera.common.message;

import com.dianwoda.usercenter.vera.common.protocol.Common;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author seam
 */
public class CommandDecoder {

  public final static int MAGIC_CODE_POSITION = 4;
  public final static int STORE_TIMESTAMP_POSITION = 12;

  public final static int BODY_SIZE_POSITION =
            4 // 1 totalsize
          + 4 // 2 MAGICCODE
          + 4 // 3 crc
          + 8 // 4 STORETIMESTAMP
          + 8 // 5 store offset
          + 8;// 6 logic offset

  public static List<CommandExt> decodes(ByteBuffer byteBuffer) {
    return decodes(byteBuffer, true);
  }

  public static List<CommandExt> decodes(ByteBuffer byteBuffer, final boolean readBody) {
    List<CommandExt> commandExts = new ArrayList<>();

    while (byteBuffer.hasRemaining()) {
      CommandExt commandExt = decode(byteBuffer, true);
      if (commandExt != null) {
        commandExts.add(commandExt);
      } else {
        break;
      }
    }
    return commandExts;
  }

  public static CommandExt decode(ByteBuffer byteBuffer, final boolean readBody) {
    try {
      CommandExt commandExt = new CommandExt();
      // 1 total size
      int totalSize = byteBuffer.getInt();
      commandExt.setTotalSize(totalSize);

      // 2 magic code
      int magicCode = byteBuffer.getInt();
      commandExt.setMagicCode(magicCode);
      if (magicCode != Common.MESSAGE_MAGIC_CODE) {
        return null;
      }

      // 3 crc
      int crc = byteBuffer.getInt();
      commandExt.setCrc(crc);

      // 4 store timestamp
      long storeTimestamp = byteBuffer.getLong();
      commandExt.setStoreTimestamp(storeTimestamp);

      // 5 store offset
      long storeOffset = byteBuffer.getLong();
      commandExt.setStoreOffset(storeOffset);

      // 6 logic offset
      long logicOffset = byteBuffer.getLong();
      commandExt.setLogicOffset(logicOffset);

      // 7 data
      if (readBody) {
        // 7 data length
        int dataLength = byteBuffer.getInt();
        commandExt.setDataLength(dataLength);

        // 8 data
        byte[] data = new byte[dataLength];
        byteBuffer.get(data);
        commandExt.setData(data);
      }
      return commandExt;

    } catch (Exception e) {
      byteBuffer.position(byteBuffer.limit());
    }
    return null;
  }


}
