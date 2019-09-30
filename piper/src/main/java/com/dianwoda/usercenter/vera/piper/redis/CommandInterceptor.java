package com.dianwoda.usercenter.vera.piper.redis;

/**
 * command 处理前拦截
 * @author seam
 */
public interface CommandInterceptor<RedisCommand> {

  public RedisCommand interceptor(RedisCommand redisCommand);

}
