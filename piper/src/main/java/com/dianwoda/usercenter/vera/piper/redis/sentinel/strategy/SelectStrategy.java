package com.dianwoda.usercenter.vera.piper.redis.sentinel.strategy;

public interface SelectStrategy {
	public int select(int count);
}
