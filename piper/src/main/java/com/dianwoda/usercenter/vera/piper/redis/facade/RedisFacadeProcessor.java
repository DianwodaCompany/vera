package com.dianwoda.usercenter.vera.piper.redis.facade;

import com.dianwoda.usercenter.vera.common.redis.command.RedisCommand;
import com.dianwoda.usercenter.vera.piper.client.PiperClientInstance;
import com.dianwoda.usercenter.vera.piper.redis.*;
import com.dianwoda.usercenter.vera.piper.redis.sentinel.Redic;
import com.dianwoda.usercenter.vera.piper.redis.sentinel.RedisWriter;

import java.io.IOException;

/**
 * redis操作facade 封装
 * @author seam
 */
public class RedisFacadeProcessor {
  private volatile boolean initial;
  private RedisConfig redisConfig;
  private DefaultSlaveRedisReplicator slaveRedisReplicator;
  private CommandInterceptor redisAddInterceptor;
  private RedisReplicatorListener replicatorListener;
  private CommandInterceptor redisCommandInterceptor;
  private RedisWriter redisWriter;
  private Redic redic;

  public RedisFacadeProcessor() {
    this.initial = false;
  }

  public boolean isInitial() {
    return this.initial;
  }

  public boolean isSameRedisCluster(RedisConfig redisConfig) {
    return this.redisConfig.equals(redisConfig);
  }

  public synchronized boolean initial(RedisConfig redisConfig, PiperClientInstance piperClientInstance) {
    if (!this.initial) {
      this.redisConfig = redisConfig;
      this.redic = new Redic(redisConfig.getMasterName(), redisConfig.getSentinalList(), redisConfig.getPassword());
      // add interceptor
      CommandInterceptor filterInterceptor = new CycleCommandFilterInterceptor(piperClientInstance.getPiperController().getCircleDisposeHandler());
      this.redisAddInterceptor = new CycleCommandAddInterceptor(piperClientInstance.getPiperController().getCircleDisposeHandler(), filterInterceptor);

      // add listener
      this.replicatorListener = new DefaultRedisReplicatorListener(piperClientInstance.getPiperController().getCommandStore());

      // 初始化redis writer
      this.redisCommandInterceptor = new CycleCommandFilterInterceptor(piperClientInstance.getPiperController().getCircleDisposeHandler());
      this.redisWriter = new RedisWriter(this.redic);
      this.redisWriter.setInterceptor(this.redisCommandInterceptor);

      // redis replicator
      this.slaveRedisReplicator = new DefaultSlaveRedisReplicator(this.redic.getRandomMaster());
      this.slaveRedisReplicator.setCommandInterceptor(this.redisAddInterceptor);
      this.slaveRedisReplicator.setRedisReplicatorListener(this.replicatorListener);
      this.slaveRedisReplicator.getReplicator().getConfiguration().setAuthPassword(this.redisConfig.getPassword());

      this.initial = true;
    }
    return true;
  }

  public boolean reConfigure(RedisConfig redisConfig, PiperClientInstance piperClientInstance) {

    if (!this.redisConfig.equals(redisConfig)) {
      this.stop();
      this.redisConfig = redisConfig;
      this.redic = new Redic(redisConfig.getMasterName(), redisConfig.getSentinalList(), redisConfig.getPassword());

      this.slaveRedisReplicator = new DefaultSlaveRedisReplicator(this.redic.getRandomMaster());
      this.slaveRedisReplicator.setCommandInterceptor(this.redisAddInterceptor);
      this.slaveRedisReplicator.setRedisReplicatorListener(this.replicatorListener);
      this.slaveRedisReplicator.getReplicator().getConfiguration().setAuthPassword(this.redisConfig.getPassword());

      this.redisWriter = new RedisWriter(this.redic);
      this.redisWriter.setInterceptor(this.redisCommandInterceptor);
    }
    return true;
  }


  public boolean write(RedisCommand redisCommand) throws Exception {
    return this.redisWriter.write(redisCommand);
  }

  public DefaultSlaveRedisReplicator getSlaveRedisReplicator() {
    return slaveRedisReplicator;
  }

  public RedisConfig getRedisConfig() {
    return redisConfig;
  }

  public void start() throws IOException {
    this.slaveRedisReplicator.start();

  }

  public void stop() {
    this.initial = false;
    if (this.slaveRedisReplicator != null) {
      this.slaveRedisReplicator.stop();
    }
    if (this.redisWriter != null) {
      this.redisWriter.stop();
    }
  }

  public RedisWriter getRedisWriter() {
    return redisWriter;
  }
}
