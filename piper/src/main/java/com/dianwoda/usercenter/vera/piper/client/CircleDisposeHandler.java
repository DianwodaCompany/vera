package com.dianwoda.usercenter.vera.piper.client;

/**
 * 数据循环覆盖的处理
 * @author seam
 */
public interface CircleDisposeHandler<T> {

  public void start();

  public void stop();

  /**
   * 是否是循环数据
   * @param data
   * @return
   */
  boolean isCycleData(T data);

  /**
   * 添加循环数据
   * @param data
   * @return
   */
  boolean addCycleData(T data);

}
