package com.dianwoda.usercenter.vera.store;


import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.UtilAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * block file 集合封装
 * @author seam
 */
public class BlockFileQueue  {
  protected static final Logger log = LoggerFactory.getLogger(BlockFileQueue.class);

  private String storePath;
  private int blockFileSize;
  private final CopyOnWriteArrayList<BlockFile> blockFiles = new CopyOnWriteArrayList<BlockFile>();
  private long flushedWhere = 0;
  // command flush的存储时间
  private volatile long storeTimestamp = 0;

  public BlockFileQueue(String storePath, int blockFileSize) {
    this.storePath = storePath;
    this.blockFileSize = blockFileSize;
  }

  public boolean load() {
    File dir = new File(this.storePath);
    File[] files = dir.listFiles();
    files = filter(files);
    if (files != null) {
      Arrays.sort(files);
      for (File file : files) {
        if (file.length() != this.blockFileSize) {
          log.warn(file + "\t" + file.length() +
                    " length not matched command store config value, ignore it");
          return true;
        }

        try {
          BlockFile blockFile = new BlockFile(file.getPath(), this.blockFileSize);
          blockFile.setFlushedPosition(this.blockFileSize);
          blockFile.setWrotePosition(this.blockFileSize);
          blockFiles.add(blockFile);

          log.info(file + "\t" + file.length() +
                  " add to block file queue!");
        } catch (IOException e) {
          log.error("Lood file " + file + " error", e);
          return false;
        }
      }
    }
    return true;
  }

  /**
   * 根据offset查找block file
   * @return
   */
  public BlockFile findBlockFileByOffset(final long offset) {
    BlockFile firstBlockFile = getFirstBlockFile();
    if (firstBlockFile == null) {
      return null;
    }
    long firstOffset = firstBlockFile.getFileFromOffset();
    if (offset <firstOffset) {
      log.error("Error can't find block file using offset: " + offset);
      return null;
    }
    try {
      int index = (int)(offset/this.blockFileSize - (int)firstOffset/this.blockFileSize);
      if (index >= this.blockFiles.size()) {
        log.error(String.format("offset too big, can't match. offset:%d, blockfile size:%d, index:%d", offset,
                this.blockFiles.size(), index));
        return null;
      }
      return this.blockFiles.get(index);
    } catch (Exception e) {
      log.error("find block file by offset: " + offset + " failture", e);
      log.info("blockFiles:" + blockFiles);
    }
    return null;
  }

  public BlockFile getLastBlockFile(final long startOffset, boolean needCreate) {
    BlockFile lastBlockFile = getLastBlockFile();
    long createOffset = -1;
    if (lastBlockFile == null) {
      createOffset = startOffset - (startOffset % this.blockFileSize);
    }
    if (lastBlockFile != null && lastBlockFile.isFull()) {
      createOffset = lastBlockFile.getFileFromOffset() + this.blockFileSize;
    }

    if (createOffset != -1 && needCreate) {
      String nextFilePath = this.storePath + File.separator + UtilAll.offset2FileName(createOffset);
      BlockFile blockFile = null;
      try {
        blockFile = new BlockFile(nextFilePath, this.blockFileSize);
      } catch (IOException e) {
        log.error("create blockfile exception", e);
      }
      if (blockFile != null) {
        this.blockFiles.add(blockFile);
      }
      return blockFile;
    }
    return lastBlockFile;
  }

  public BlockFile getLastBlockFile() {
    BlockFile blockFile = null;
    while (!this.blockFiles.isEmpty()) {
      try {
        blockFile = this.blockFiles.get(this.blockFiles.size() - 1);
        break;
      } catch (IndexOutOfBoundsException e) {

      }
    }
    return blockFile;
  }

  public BlockFile getFirstBlockFile() {
    BlockFile blockFile = null;
    while (!this.blockFiles.isEmpty()) {
      try {
        blockFile = this.blockFiles.get(0);
        break;
      } catch (IndexOutOfBoundsException e) {

      }

    }
    return blockFile;
  }

  public long getMaxOffset() {
    BlockFile blockFile = getLastBlockFile();
    if (blockFile != null) {
      return blockFile.getFileFromOffset() + blockFile.getReadPosition();
    }
    return 0;
  }

  public long getMinOffset() {
    BlockFile blockFile = getFirstBlockFile();
    if (blockFile != null) {
      return blockFile.getFileFromOffset();
    }
    return 0;
  }

  public boolean flush(final int flushLeastPages) {
    boolean result = true;
    BlockFile blockFile = this.findBlockFileByOffset(this.flushedWhere);
    if (blockFile != null) {
      long tmpTimeStamp = blockFile.getStoreTimestamp();
      int offset = blockFile.flush(flushLeastPages);
      long where = blockFile.getFileFromOffset() + offset;
      result = where == this.flushedWhere;
      this.flushedWhere = where;
      if (0 == flushLeastPages) {
        this.storeTimestamp = tmpTimeStamp;
      }
    }
    return result;
  }

  public List<BlockFile> getBlockFiles() {
    return blockFiles;
  }


  public void truncateDirtyFiles(long offset) {
    List<BlockFile> willRemoveFiles = new ArrayList<>();
    for (BlockFile file : this.blockFiles) {
      long fileTailOffset = file.getFileFromOffset() + this.blockFileSize;
      if (fileTailOffset > offset) {
        if (offset >= file.getFileFromOffset()) {
          file.setWrotePosition((int)(offset % this.blockFileSize));
          file.setFlushedPosition((int)(offset % this.blockFileSize));

        } else {
          file.destroy(1000);
          willRemoveFiles.add(file);
        }
      }
    }
    this.deleteExpiredFile(willRemoveFiles);
  }

  private void deleteExpiredFile(List<BlockFile> files) {
    if (!files.isEmpty()) {
      Iterator<BlockFile> iterator = files.iterator();
      while (iterator.hasNext()) {
        BlockFile blockFile = iterator.next();
        if (!this.blockFiles.contains(blockFile)) {
          iterator.remove();
          log.info("This blockFile {} is not contained by mappedFiles, so skip it.", blockFile.getFileName());
        }
      }

      try {
        if (!this.blockFiles.removeAll(files)) {
          log.error("deleteExpiredFile remove failed.");
        }
      } catch (Exception e) {
        log.error("deleteExpiredFile has exception.", e);
      }
    }
  }

  public long getFlushedWhere() {
    return flushedWhere;
  }

  public void setFlushedWhere(long flushedWhere) {
    this.flushedWhere = flushedWhere;
  }

  private File[] filter(File[] files) {
    if (files == null || files.length == 0) {
      return files;
    }
    List<File> newFiles = Arrays.asList(files).stream().filter(new Predicate<File>(){

      @Override
      public boolean test(File file) {
        String name = file.getName();
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(name).matches();
      }
    }).collect(Collectors.toList());
    return  newFiles.toArray(new File[0]);
  }

  public long getStoreTimestamp() {
    return storeTimestamp;
  }

  public int  deleteExpiredFileByTime(final long expiredTime, final int deleteFilesInterval,
                                      final long intervalForcibly) {
    BlockFile[] blockFiles = this.copyBlockFile(0);

    List<BlockFile> files = new ArrayList<>();
    if (blockFiles != null) {
      int deleteCount = 0;
      long timeStamp = SystemClock.now();
      for (BlockFile blockFile : blockFiles) {

        if (timeStamp - blockFile.getFile().lastModified() >= expiredTime) {
          log.info(blockFile.getFileName() + " last modified timestamp:" + UtilAll.timeMillisToHumanString2(blockFile.getFile().lastModified()) + " expired");
          blockFile.destroy(intervalForcibly);
          files.add(blockFile);
          deleteCount++;

          if (deleteFilesInterval > 0) {
            try {
              Thread.sleep(deleteFilesInterval);
            } catch (InterruptedException e) {
              log.error("deleteExpiredFileByTime sleep, error", e);
            }
          }
        }
      }
      this.clearFileStructure(files);
      return deleteCount;
    }
    return 0;
  }

  private void clearFileStructure(List<BlockFile> deleteFiles) {
    Iterator<BlockFile> iter = this.blockFiles.iterator();
    while (iter.hasNext()) {
      BlockFile blockFile = iter.next();
      if (deleteFiles.contains(blockFile)) {
        iter.remove();
      }
    }
  }

  private BlockFile[] copyBlockFile(final int reserveFiles) {
    if (this.blockFiles.size() < reserveFiles) {
      return null;
    }
    return this.blockFiles.toArray(new BlockFile[0]);
  }
}
