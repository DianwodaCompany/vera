package com.dianwoda.usercenter.vera.piper.redis.sentinel.strategy;

import com.dianwoda.usercenter.vera.common.SystemClock;

import java.util.Random;

public class HashShardingStrategy implements ShardingStrategy {
	@Override
	public <T> int key2node(T key, int nodeCount) {
		int hashCode = key.hashCode();
		return hashCode % nodeCount;
	}

  @Override
  public <T> int key2node(int nodeCount) {
    Random random = new Random(SystemClock.now());
    return random.nextInt(nodeCount);
  }
}
