package com.dianwoda.usercenter.vera.piper.redis.sentinel;

import com.dianwoda.usercenter.vera.common.redis.command.RedisCommand;
import com.dianwoda.usercenter.vera.piper.redis.CommandInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * redis writer 封装
 * @author seam
 */
public class RedisWriter {
  protected static final Logger log = LoggerFactory.getLogger(RedisWriter.class);
  private Redic redic;
  private CommandInterceptor<RedisCommand> interceptor;

  public RedisWriter(Redic redic) {
    this.redic = redic;
  }


  public void start() {

  }
  public void stop() {

  }

  public void setInterceptor(CommandInterceptor<RedisCommand> interceptor) {
    this.interceptor = interceptor;
  }

  public boolean write(RedisCommand command) throws Exception {

    if (this.interceptor.interceptor(command) != null) {
      this.redic.write(command);
      log.warn("Write Command: " + command + " success!");
      return true;
    } else {
      log.warn("Command: " + command + " is filtered!");
      return false;
    }
  }


}
