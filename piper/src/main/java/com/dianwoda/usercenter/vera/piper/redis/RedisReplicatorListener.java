package com.dianwoda.usercenter.vera.piper.redis;

import com.dianwoda.usercenter.vera.common.redis.command.RedisCommand;

/**
 * 接收Redis数据后的操作扩展接口
 * @author seam
 */
public interface RedisReplicatorListener {
  /**
   * 接收命令
   */
  void receive(RedisCommand command);
}
