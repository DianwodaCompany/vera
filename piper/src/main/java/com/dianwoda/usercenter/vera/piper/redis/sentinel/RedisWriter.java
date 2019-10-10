package com.dianwoda.usercenter.vera.piper.redis.sentinel;

import com.dianwoda.usercenter.vera.common.redis.command.RedisCommand;
import com.dianwoda.usercenter.vera.piper.redis.CommandInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RecursiveAction;

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
      try {
        log.info("Write Command: " + command + " success!");
      } catch (Exception e) {
        log.error("out put error!", e);
      }
      return true;
    } else {
      try {
        log.info("Command: " + command + " is filtered!");
      } catch (Exception e) {
        log.error("out put error!", e);
      }
      return false;
    }
  }

  /**
   * just for test
   */
  public boolean delete(RedisCommand command) throws Exception {
    this.redic.delete(command);
    return true;
  }
}
