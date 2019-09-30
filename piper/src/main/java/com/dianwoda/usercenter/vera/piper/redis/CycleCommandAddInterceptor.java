package com.dianwoda.usercenter.vera.piper.redis;

import com.dianwoda.usercenter.vera.piper.client.CircleDisposeHandler;

/**
 * redis command 添加前拦截处理
 * @author seam
 */
public class CycleCommandAddInterceptor<RedisCommand> implements CommandInterceptor<RedisCommand> {
  private CircleDisposeHandler<RedisCommand> circleDisposeHandler;
  private CommandInterceptor<RedisCommand> next;

  public CycleCommandAddInterceptor(CircleDisposeHandler<RedisCommand> circleDisposeHandler,
                                    CommandInterceptor<RedisCommand> next) {
    this.circleDisposeHandler = circleDisposeHandler;
    this.next = null;
  }
  public CycleCommandAddInterceptor(CircleDisposeHandler<RedisCommand> circleDisposeHandler) {
    this.circleDisposeHandler = circleDisposeHandler;
    this.next = null;
  }

  @Override
  public RedisCommand interceptor(RedisCommand redisCommand) {
    if ((this.next != null && this.next.interceptor(redisCommand) != null)
            || this.next == null) {
      this.circleDisposeHandler.addCycleData(redisCommand);
      return redisCommand;

    } else {
      return null;
    }
  }

}
