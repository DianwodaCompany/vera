package com.dianwoda.usercenter.vera.piper.client.listener;

import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.UtilAll;
import com.dianwoda.usercenter.vera.common.message.CommandExt;
import com.dianwoda.usercenter.vera.common.redis.command.RedisCommand;
import com.dianwoda.usercenter.vera.piper.redis.serializer.RedisCommandDeserializer;
import com.dianwoda.usercenter.vera.piper.client.PiperClientInstance;
import com.dianwoda.usercenter.vera.piper.enums.ConsumeOrderlyStatus;
import com.dianwoda.usercenter.vera.store.io.ObjectDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 顺序消费redis command的实现
 * @author seam
 */
public class DefaultCommandListenerOrderlyImpl implements CommandListenerOrderly {
  protected static final Logger log = LoggerFactory.getLogger(DefaultCommandListenerOrderlyImpl.class);
  private ObjectDeserializer<RedisCommand> deserializer;
  private PiperClientInstance piperClientInstance;

  public DefaultCommandListenerOrderlyImpl(PiperClientInstance piperClientInstance) {
    this.piperClientInstance = piperClientInstance;
    this.deserializer = new RedisCommandDeserializer();
  }
  @Override
  public ConsumeOrderlyStatus consumer(List<CommandExt> commands) {
    if (commands == null || commands.isEmpty()) {
      return ConsumeOrderlyStatus.SUSPEND;
    }
    try {
      for (CommandExt command : commands) {
        long storeTimeStamp = command.getStoreTimestamp();
        log.info("command store data time:" + UtilAll.timeMillisToHumanString2(storeTimeStamp));

        //  根据时间过滤数据
        if (this.piperClientInstance.isSyncCommandFilterSwitch() && ((storeTimeStamp + this.piperClientInstance.getCommandPostDueTimeMillis() < SystemClock.now()))) {
          log.info("filter command:" + command + " because of storetime is too old!");
          continue;
        }
        if (command.getData() != null) {
          RedisCommand redisCommand = deserializer.deserialize(command.getData());
          if (this.piperClientInstance.getPiperClientInterImpl().getRedisFacadeProcessor().write(redisCommand)) {
            log.info("Write into redis success, length {}", command.getDataLength());
          } else {
            log.error("Write into redis fail, redisCommand {}", redisCommand);
          }
        }
      }
    } catch (Exception e) {
      log.error("command consume error", e);
      return ConsumeOrderlyStatus.SUSPEND;
    }
    return ConsumeOrderlyStatus.SUCCESS;
  }
}
