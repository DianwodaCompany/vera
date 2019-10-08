package com.dianwoda.usercenter.vera.piper.client;


import com.dianwoda.usercenter.vera.common.ThreadFactoryImpl;
import com.dianwoda.usercenter.vera.piper.data.DelayedElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.*;

/**
 * 情况：1. A侦听到redis动作，将redis数据写入本地；2. B根据偏移请求A获取到该数据写入本地；
 *      3. A根据偏移再请求B获取到数据，如果再写入本地就将造成数据重复添加，循环写入，该类就是为避免这种现象，
 *      同一个数据30秒内不允许重复写入；
 * @author seam
 */
public class DefaultCircleDisposeHandler<RedisCommand> implements CircleDisposeHandler<RedisCommand> {
  protected static final Logger log = LoggerFactory.getLogger(DefaultCircleDisposeHandler.class);
  final BlockingQueue<DelayedElement<RedisCommand>> deque = new DelayQueue<DelayedElement<RedisCommand>>();
  final Set<RedisCommand> commandSet = new CopyOnWriteArraySet<RedisCommand>();
  final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryImpl("DefaultCircleDisposeHandlerThread_"));

  public DefaultCircleDisposeHandler() {
  }

  @Override
  public void start() {
    executor.submit(() -> {
      while (true) {
        try {
          RedisCommand command = deque.take().getData();
          commandSet.remove(command);
        } catch (InterruptedException e) {
          log.error("DefaultCircleDisposeHandler remove error, ", e);
        }
      }
    });
  }

  @Override
  public void stop() {
    executor.shutdown();
  }

  @Override
  public boolean isCycleData(RedisCommand data) {
    if (commandSet.contains(data)) {
      return true;
    }
    return false;
  }

  @Override
  public boolean addCycleData(RedisCommand data) {
    if (!commandSet.contains(data)) {
//      deque.add(new DelayedElement(2 * 60 * 60 * 1000, data)); // delay two hours
      deque.add(new DelayedElement(60 * 1000, data)); // delay 60 seconds
      commandSet.add(data);
      return true;
    }
    return false;
  }
}
