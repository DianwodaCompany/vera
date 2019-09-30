package com.dianwoda.usercenter.vera.piper.service;

import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.ThreadFactoryImpl;
import com.dianwoda.usercenter.vera.common.message.CommandExt;
import com.dianwoda.usercenter.vera.common.redis.command.RedisCommand;
import com.dianwoda.usercenter.vera.piper.client.DefaultPullConsumerImpl;
import com.dianwoda.usercenter.vera.piper.client.ProcessQueue;
import com.dianwoda.usercenter.vera.piper.client.listener.CommandListenerOrderly;
import com.dianwoda.usercenter.vera.piper.client.stat.ConsumerStatsManager;
import com.dianwoda.usercenter.vera.piper.enums.ConsumeOrderlyStatus;
import com.dianwoda.usercenter.vera.piper.redis.serializer.RedisCommandDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

/**
 * 顺序消费redis command, 消费失败可重新消费
 * @author seam
 */
public class ConsumeCommandOrderlyService {
  protected static final Logger log = LoggerFactory.getLogger(ConsumeCommandOrderlyService.class);
  private final DefaultPullConsumerImpl defaultPullConsumer;
  private final CommandListenerOrderly commandListener;
  private final ThreadPoolExecutor consumerPoolExecutor;
  private final ScheduledExecutorService scheduledExecutorService;

  public ConsumeCommandOrderlyService(DefaultPullConsumerImpl defaultPullConsumer, CommandListenerOrderly commandListener) {
    this.defaultPullConsumer = defaultPullConsumer;
    this.commandListener = commandListener;
    this.consumerPoolExecutor = new ThreadPoolExecutor(defaultPullConsumer.getConsumeThreadMin(), defaultPullConsumer.getConsumeThreadMax(),
            1000 * 60, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactoryImpl("ConsumerCommandThread_"));
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("ConsumerCommandScheduleThread_"));
  }

  public void submitConsumeRequest(List<CommandExt> commandExts, ProcessQueue processQueue, boolean dispatchConsume) {
    if (dispatchConsume) {
      this.consumerPoolExecutor.submit(new ConsumeRequest(processQueue));
    }
  }

  class ConsumeRequest implements Runnable {
    final ProcessQueue processQueue;

    public ConsumeRequest(ProcessQueue processQueue) {
      this.processQueue = processQueue;
    }

    @Override
    public void run() {
      try {
        processQueue.getConsumeLock().lock();
        for (boolean continueConsume = true; continueConsume; ) {
          List<CommandExt> commands = processQueue.takeCommands(ConsumeCommandOrderlyService.this.defaultPullConsumer.getConsumeMessageBatchMaxSize());
          if (!commands.isEmpty()) {
            long beginTimestamp = SystemClock.now();
            ConsumeOrderlyStatus status = ConsumeCommandOrderlyService.this.commandListener.consumer(commands);
            long consumeRT = SystemClock.now() - beginTimestamp;
            ConsumeCommandOrderlyService.this.getConsumerStatsManager().incConsumeRT(
                    processQueue.getSyncPiperLocation(), consumeRT);
            continueConsume = processCommandsResult(status, commands, processQueue);
          } else {
            continueConsume = false;
          }
        }
      } catch (Exception e) {
        log.error("ConsumeRequest run error", e);

      } finally {
        processQueue.getConsumeLock().unlock();
      }
    }
  }

  public boolean processCommandsResult(ConsumeOrderlyStatus status, List<CommandExt> commands,
                                       ProcessQueue processQueue) {
    boolean continueConsume = true;
    long commitOffset = -1L;
    switch (status) {
      case SUCCESS:
        commitOffset = processQueue.commit();
        this.defaultPullConsumer.getConsumerStatsManager().incConsumeOKTPS(processQueue.getSyncPiperLocation(), commands.size());
        break;
      case SUSPEND:
        this.defaultPullConsumer.getConsumerStatsManager().incConsumeFailedTPS(processQueue.getSyncPiperLocation(), commands.size());
        if (checkConsumeTime(commands)) {
          processQueue.consumeAgain(commands);
          submitConsumeRequestLater(processQueue, defaultPullConsumer.getSuspendCurrentQueueTimeMillis());
          continueConsume = false;
        } else {
          this.defaultPullConsumer.getConsumerStatsManager().incConsumeFinalFailedTPS(processQueue.getSyncPiperLocation(), commands.size());

          log.error("ConsumeRequest already consume max times:" + defaultPullConsumer.getConsumeTimeMax() +
                  ", msg size:" + commands.size() + ",   drop it!" + commands.get(0).getData());
          RedisCommandDeserializer deserializer = new RedisCommandDeserializer();
          StringBuilder sb = new StringBuilder();
          commands.forEach(command -> {
            sb.append("command: ").append(deserializer.deserialize(command.getData())).append(",");
          });
          log.error("drop redis command:" + sb.toString());
          commitOffset = processQueue.commit();
        }
        break;
      default:
        break;
    }
    if (commitOffset > -1L) {
      defaultPullConsumer.getOffsetManager().commitOffset(processQueue.getSyncPiperLocation(), commitOffset);
    }
    return continueConsume;
  }

  private boolean checkConsumeTime(List<CommandExt> commands) {
    if (commands != null && !commands.isEmpty()) {
      for (CommandExt command : commands) {
        int reconsumeTime = command.getReconsumeTimes();
        if (reconsumeTime >= defaultPullConsumer.getConsumeTimeMax()) {
          return false;
        } else {
          command.setReconsumeTimes(reconsumeTime + 1);
        }
      }
      return true;
    }
    return false;
  }

  private void submitConsumeRequestLater(ProcessQueue processQueue, long suspendTime) {
    if (suspendTime < 0) {
      suspendTime = defaultPullConsumer.getSuspendCurrentQueueTimeMillis();
    }
    scheduledExecutorService.schedule(() -> {
      submitConsumeRequest(null, processQueue, true);

    }, suspendTime, TimeUnit.MILLISECONDS);
  }

  public int getCorePollSize() {
    return this.consumerPoolExecutor.getCorePoolSize();
  }

  public ConsumerStatsManager getConsumerStatsManager() {
    return this.defaultPullConsumer.getConsumerStatsManager();
  }
}
