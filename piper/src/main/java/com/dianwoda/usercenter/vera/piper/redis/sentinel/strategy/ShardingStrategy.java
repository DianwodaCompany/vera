package com.dianwoda.usercenter.vera.piper.redis.sentinel.strategy;

public interface ShardingStrategy {
  public <T> int key2node(T key, int nodeCount);

  public <T> int key2node(int nodeCount);
}
