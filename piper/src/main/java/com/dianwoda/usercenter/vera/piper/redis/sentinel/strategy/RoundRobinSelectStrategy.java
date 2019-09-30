package com.dianwoda.usercenter.vera.piper.redis.sentinel.strategy;

import java.util.concurrent.atomic.AtomicLong;

public class RoundRobinSelectStrategy implements SelectStrategy {
	private AtomicLong iter = new AtomicLong(0);

	public int select(int count) {
		long iterValue = iter.incrementAndGet();

		// Still race condition, but it doesn't matter
		if (iterValue == Long.MAX_VALUE)
			iter.set(0);

		return (int) iterValue % count;
	}
}
