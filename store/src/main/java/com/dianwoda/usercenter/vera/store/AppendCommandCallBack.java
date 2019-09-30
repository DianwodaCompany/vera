package com.dianwoda.usercenter.vera.store;


import java.nio.ByteBuffer;

/**
 * @author seam
 */
public interface AppendCommandCallBack {

  AppendCommandResult doAppend(final long fileFromOffset, final ByteBuffer byteBuffer,
                               final int maxBlank, final CommandInter commandInter);
}
