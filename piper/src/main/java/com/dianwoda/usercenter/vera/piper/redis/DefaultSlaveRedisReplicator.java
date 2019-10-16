package com.dianwoda.usercenter.vera.piper.redis;


import com.dianwoda.usercenter.vera.common.ThreadFactoryImpl;
import com.dianwoda.usercenter.vera.common.redis.command.RedisCommand;
import com.dianwoda.usercenter.vera.piper.redis.command.RedisCommandBuilder;
import com.moilioncircle.redis.replicator.RedisReplicator;
import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.cmd.Command;
import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.*;

/**
 * @program: vera
 * @description: redis 复制服务
 * @author: zhouqi1
 * @create: 2018-10-10 14:51
 **/
public class DefaultSlaveRedisReplicator {

  private Logger logger = LoggerFactory.getLogger(DefaultSlaveRedisReplicator.class);
  private CommandInterceptor<RedisCommand> commandInterceptor;
  private RedisReplicatorListener redisReplicatorListener;
  private Replicator replicator;
  private ExecutorService redisReplicatorThreadPoolExecutor = Executors.newSingleThreadScheduledExecutor(
          new ThreadFactoryImpl("SlaveRedisReplicatorThread_"));

  public DefaultSlaveRedisReplicator(HostAndPort addr) {
    try {
      replicator = new RedisReplicator("redis://" + addr.getHost() + ":" + addr.getPort());
      replicator.addEventListener(new RedisReplicatorEventListener());
    } catch (URISyntaxException e) {
      logger.error("error", e);
      throw new RuntimeException(e);
    } catch (IOException e) {
      logger.error("error", e);
      throw new RuntimeException(e);
    }
  }

  public DefaultSlaveRedisReplicator(String uri) {
    try {
      replicator = new RedisReplicator(uri);
      replicator.addEventListener(new RedisReplicatorEventListener());
    } catch (URISyntaxException e) {
      logger.error("error", e);
      throw new RuntimeException(e);
    } catch (IOException e) {
      logger.error("error", e);
      throw new RuntimeException(e);
    }
  }



  public void start() {
    redisReplicatorThreadPoolExecutor.submit(() -> {
      try {
        replicator.open();
      } catch (IOException e) {
        logger.error("error", e);
        throw new RuntimeException(e);
      }
    });
  }

  public void stop() {
    try {
      replicator.close();
      logger.info("redis replicator close");
    } catch (IOException e) {
      logger.error("error", e);
      throw new RuntimeException(e);
    }
  }

  public void setCommandInterceptor(CommandInterceptor interceptor) {
    this.commandInterceptor = interceptor;
  }

  public void setRedisReplicatorListener(RedisReplicatorListener redisReplicatorListener) {
    this.redisReplicatorListener = redisReplicatorListener;
  }

  public Replicator getReplicator() {
    return replicator;
  }

  /**
   * redis 复制时间
   */
  private class RedisReplicatorEventListener implements EventListener {

    @Override
    public void onEvent(Replicator replicator, Event event) {
      if (event instanceof Command) {
        Command command = (Command) event;
        RedisCommand redisCommand = RedisCommandBuilder.buildSwordCommand(command);
        if (redisCommand.getType() == 0
                || (redisCommand = commandInterceptor.interceptor(redisCommand)) == null) {
          return;
        }
        try {
          logger.info("Recevie {}", redisCommand);
          redisReplicatorListener.receive(redisCommand);
        } catch (Exception e) {
          logger.error("接收命令出错", e);
        }

      }
    }
  }

  public static void main(String[] args) throws IOException {
    DefaultSlaveRedisReplicator replicator = new DefaultSlaveRedisReplicator("redis://127.0.0.1:6379");
    replicator.getReplicator().getConfiguration().setAuthPassword("foobared");
    replicator.getReplicator().open();
  }
}
