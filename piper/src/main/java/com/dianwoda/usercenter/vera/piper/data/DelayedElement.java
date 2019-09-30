package com.dianwoda.usercenter.vera.piper.data;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * DelayQueue的数据元素，结构类型
 * @author seam
 */
public class DelayedElement<T> implements Delayed {
  T data;
  final long expire;

  public DelayedElement(long delay, T data) {
    this.data = data;
    expire = System.currentTimeMillis() + delay;
  }

  @Override
  public long getDelay(TimeUnit unit) {
    return unit.convert(this.expire - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
  }

  @Override
  public int compareTo(Delayed o) {
    return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
  }

  public T getData() {
    return data;
  }

}
