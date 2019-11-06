package com.dianwoda.usercenter.vera.store;


import java.util.concurrent.atomic.AtomicLong;

/**
 * command store
 * @author seam
 */
public interface CommandStore {

  public void start();
  public void stop();
  public AtomicLong getLogicOffsetMax();
  public boolean load();
  public PutCommandResult appendCommand(byte[] data);
  public boolean appendCommandToBlockFile(long phyOffset, byte[] data);
  public GetCommandResult getCommand(String location, long offset, int msgNums);
  public SelectMappedBufferResult getCommand(long offset);
  public long getMaxWriteOffset();

}
