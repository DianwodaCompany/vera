package com.dianwoda.usercenter.vera.piper.client.listener;

import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.UtilAll;
import com.dianwoda.usercenter.vera.common.message.CommandExt;
import com.dianwoda.usercenter.vera.common.redis.command.RedisCommand;
import com.dianwoda.usercenter.vera.piper.client.PiperClientInstance;
import com.dianwoda.usercenter.vera.piper.enums.ConsumeOrderlyStatus;
import com.dianwoda.usercenter.vera.piper.redis.serializer.RedisCommandDeserializer;
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
  public ConsumeOrderlyStatus consumer(String syncPiperLocation, List<CommandExt> commands) {
    if (commands == null || commands.isEmpty()) {
      return ConsumeOrderlyStatus.SUSPEND;
    }
    // just for debug
    int i = 0, j = 0, k = 0, m = 0, n = 0, l = 0;
    try {
      for (CommandExt command : commands) {
        i++;
        long storeTimeStamp = command.getStoreTimestamp();
        log.info("command store data time:" + UtilAll.timeMillisToHumanString2(storeTimeStamp));

        this.piperClientInstance.getConsumerStatsManager().incConsumeLifeCircleRT(
                syncPiperLocation,SystemClock.now() - storeTimeStamp);

        //  根据时间过滤数据
        if (this.piperClientInstance.isSyncCommandFilterSwitch() && ((storeTimeStamp + this.piperClientInstance.getCommandPostDueTimeMillis() < SystemClock.now()))) {
          log.info("filter command:" + command + " because of storetime is too old!");
          continue;
        }
        k++;
        if (command.getData() != null) {
          m++;
          RedisCommand redisCommand = deserializer.deserialize(command.getData());
          l++;
          if (this.piperClientInstance.getPiperClientInterImpl().getRedisFacadeProcessor().write(redisCommand)) {
            log.info("Write into redis success, length {}", command.getDataLength());
          }
          n++;
        } else {
          log.info("command with no data!");
        }
        j++;
      }
    } catch (Throwable e) {
      log.error("command consume error", e);
      return ConsumeOrderlyStatus.SUSPEND;
    } finally {
      log.info("commands len:" + commands.size() + " process successly, i=" + i + ", j=" + j + ", k=" + k + ", m=" + m + ", n=" + n + ", l=" + l);
    }
    return ConsumeOrderlyStatus.SUCCESS;
  }
}
