package com.dianwoda.usercenter.vera.store;

 import com.dianwoda.usercenter.vera.common.UtilAll;
 import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author seam
 */
public class StoreCheckpoint {
  private static final Logger log = LoggerFactory.getLogger(StoreCheckpoint.class);
  private final RandomAccessFile randomAccessFile;
  private final FileChannel fileChannel;
  private final MappedByteBuffer mappedByteBuffer;
  private volatile long physicMsgTimestamp = 0;

  public StoreCheckpoint(final String scpPath) throws IOException{
    File file = new File(scpPath);
    BlockFile.ensureDirOK(file.getParent());

    boolean fileExists = file.exists();
    this.randomAccessFile = new RandomAccessFile(file, "rw");
    this.fileChannel = this.randomAccessFile.getChannel();
    this.mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, BlockFile.OS_PAGE_SIZE);
    if (fileExists) {
      log.info("store checkpoint file exists." + scpPath);
      this.physicMsgTimestamp = this.mappedByteBuffer.getLong(0);

      log.info("store checkpoint file physicMsgTimestamp " + this.physicMsgTimestamp + "," +
              UtilAll.timeMillisToHumanString(this.physicMsgTimestamp));
    } else {
      log.info("store checkpoint file not exists," + scpPath);
    }

  }

  public void shutdown() {
    this.flush();
    BlockFile.clean(this.mappedByteBuffer);
    try {
      this.fileChannel.close();
    } catch (IOException e) {
      log.error("Failed to properly close the channnel", e);
    }
  }

  public void flush() {
    this.mappedByteBuffer.putLong(0, this.physicMsgTimestamp);
    this.mappedByteBuffer.force();
  }

  public long getPhysicMsgTimestamp() {
    return physicMsgTimestamp;
  }

  public void setPhysicMsgTimestamp(long physicMsgTimestamp) {
    this.physicMsgTimestamp = physicMsgTimestamp;
  }
}
