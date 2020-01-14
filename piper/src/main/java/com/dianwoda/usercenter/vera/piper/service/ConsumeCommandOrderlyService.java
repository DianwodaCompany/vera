package com.dianwoda.usercenter.vera.piper.service;

import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.ThreadFactoryImpl;
import com.dianwoda.usercenter.vera.common.UtilAll;
import com.dianwoda.usercenter.vera.common.message.CommandExt;
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
  private final ExecutorService checkExecutorService;
  private CheckConsumeService checkConsumeService;

  public ConsumeCommandOrderlyService(DefaultPullConsumerImpl defaultPullConsumer, CommandListenerOrderly commandListener) {
    this.defaultPullConsumer = defaultPullConsumer;
    this.commandListener = commandListener;
    this.consumerPoolExecutor = new ThreadPoolExecutor(defaultPullConsumer.getConsumeThreadMin(), defaultPullConsumer.getConsumeThreadMax(),
            1000 * 60, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactoryImpl("ConsumerCommandThread_"));
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("ConsumerCommandScheduleThread_"));
    this.checkExecutorService = Executors.newFixedThreadPool(4, new ThreadFactoryImpl("CheckConsumeThread_", new CustomUncaughtExceptionHandler()));
    this.checkConsumeService = new CheckConsumeService();
  }

  public void submitConsumeRequest(List<CommandExt> commandExts, ProcessQueue processQueue, boolean dispatchConsume) {
    if (dispatchConsume) {
      this.consumerPoolExecutor.submit(new ConsumeRequest(processQueue));
      this.checkConsumeService.setFirstDispatchSuccess(true);
    } else {
      this.checkConsumeService.setFirstDispatchFail(processQueue);
    }
  }

  public void start() {
    this.checkExecutorService.submit(this.checkConsumeService);
  }

  class ConsumeRequest implements Runnable {
    final ProcessQueue processQueue;

    public ConsumeRequest(ProcessQueue processQueue) {
      this.processQueue = processQueue;
    }

    @Override
    public void run() {
      try {
        processQueue.getConsumeLock().lockInterruptibly();
        for (boolean continueConsume = true; continueConsume; ) {
          try {
            if (this.processQueue.isDropped()) {
              log.warn("the process queue not be able to consume, because it's dropped. {}", this.processQueue);
              break;
            }

            List<CommandExt> commands = processQueue.takeCommands(ConsumeCommandOrderlyService.this.defaultPullConsumer.getConsumeMessageBatchMaxSize());

            if (!commands.isEmpty()) {
              long beginTimestamp = SystemClock.now();
              ConsumeOrderlyStatus status = ConsumeCommandOrderlyService.this.commandListener.consumer(processQueue.getSyncPiperLocation(), commands);
              log.info("process command consume result, ConsumeOrderlyStatus:" + status);
              boolean processResult = processCommandsResult(status, commands, processQueue);
              if (!processResult) {
                log.warn("process command consume result, ConsumeOrderlyStatus:" + status + ", processResult:" + processResult);
              } else {
                log.info("process command consume result, ConsumeOrderlyStatus:" + status + ", processResult:" + processResult);
              }
              long consumeRT = SystemClock.now() - beginTimestamp;
              ConsumeCommandOrderlyService.this.getConsumerStatsManager().incConsumeRT(
                      processQueue.getSyncPiperLocation(), consumeRT);
            } else {
              log.info("commands is empty, size:" + this.processQueue.getTreeSize());
              break;
            }
          } catch (Throwable e) {
            log.info("Consume command error", e);
          }
        }
      } catch (Throwable e) {
        log.info("ConsumeRequest run error" , e);

      } finally {
        processQueue.getConsumeLock().unlock();
        log.info("processQueue process over, but queue size: " + this.processQueue.getMsgCount() +
                ", temp tree size:" + this.processQueue.getTempTreeSize() +
                ", tree size:" + this.processQueue.getTreeSize());
      }
    }
  }

  /**
   * 检测processqueue 消费的正常
   */
  class CheckConsumeService implements Runnable {
    private long firstDispatchFailTimestamp = 0;
    private int firstDispatchFailTimes = 0;
    private int firstDispatchFailTimesThreshold = 3;
    private ProcessQueue processQueue;
    private boolean check = true;
    private int count = 0;

    public void setFirstDispatchFail(ProcessQueue processQueue) {
      this.processQueue = processQueue;

      if (this.firstDispatchFailTimestamp == 0) {
        this.firstDispatchFailTimestamp = SystemClock.now();
      }
      this.firstDispatchFailTimes++;

      if (check()) {
        log.info("CheckConsumeService submit ConsumeRequest, firstDispatchFailTimes:" + firstDispatchFailTimes +
          ", firstDispatchFailTimestamp:" + UtilAll.timeMillisToHumanString2(firstDispatchFailTimestamp));
        ConsumeCommandOrderlyService.this.checkExecutorService.submit(new ConsumeRequest(processQueue));
        clean();
      }
    }

    public void setFirstDispatchSuccess(boolean firstDispatchSuccess) {
      if (firstDispatchSuccess == true) {
        this.firstDispatchFailTimestamp = 0;
        this.firstDispatchFailTimes = 0;
      }
    }

    @Override
    public void run() {
      long firstSatisfyTimestamp = -1;
      int timeSpan = 1000 * 30;
      while (check) {
        try {
          if (++count % 500 == 0 ) {
            log.info("CheckConsumeService start checking");
            count = 0;
          }

          if (this.processQueue == null || this.processQueue.isDropped()) {
            suspend(1500);
            continue;
          }

          long maxSpan = this.processQueue.getMaxSpan();
          int msgCount = this.processQueue.getMsgCount().get();
          if ((maxSpan > DefaultPullConsumerImpl.pullThresholdForQueue ||
                  msgCount > DefaultPullConsumerImpl.consumeConcurrentlyMaxSpan)
                  && this.processQueue.isConsuming()) {
            if (firstSatisfyTimestamp == -1) {
              firstSatisfyTimestamp = SystemClock.now();
              suspend(1200);
              continue;
            }
            if (SystemClock.now() - firstSatisfyTimestamp < timeSpan) {
              suspend(1200);
              continue;
            }
            log.info("CheckConsumeService submit ConsumeRequest, maxSpan:" + maxSpan +
                    ", msgCount:" + msgCount);
            firstSatisfyTimestamp = -1;
            ConsumeCommandOrderlyService.this.checkExecutorService.submit(new ConsumeRequest(processQueue));
            clean();
            suspend(1500);
            continue;
          } else {
            firstSatisfyTimestamp = -1;
          }
          suspend(1200);
        } catch (Exception e) {
          log.error("CheckConsumeService check error", e);
          try {
            suspend(500);
          } catch (InterruptedException e1) {
            log.error("CheckConsumeService check error", e1);
          }
        }
      }
    }

    public boolean check() {
      if (this.firstDispatchFailTimes > 1 &&
              (SystemClock.now() - this.firstDispatchFailTimestamp) >= 1000 * 30) {
        return true;
      }

      if (this.firstDispatchFailTimestamp == 0 ||
              (SystemClock.now() - this.firstDispatchFailTimestamp) < 1000) {
        return false;
      }
      if (this.firstDispatchFailTimes < firstDispatchFailTimesThreshold) {
        return false;
      }
      return true;
    }

    private void clean() {
      this.firstDispatchFailTimestamp = 0;
      this.firstDispatchFailTimes = 0;
    }

    private void suspend(int timestamp) throws InterruptedException {
      Thread.sleep(timestamp);
    }
  }


  public class CustomUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
      //自定义异常处理逻辑
      log.error("exception caughted, thread:" + t.getName() + " exception:" + e);
    }
  }

  public boolean processCommandsResult(ConsumeOrderlyStatus status, List<CommandExt> commands,
                                       ProcessQueue processQueue) throws InterruptedException {
    boolean continueConsume = true;
    long commitOffset = -1L;
    switch (status) {
      case SUCCESS:
        commitOffset = processQueue.commit();
        if (commitOffset == -1) {
          log.error("commit error!");
        }
        this.defaultPullConsumer.getConsumerStatsManager().incConsumeOKTPS(processQueue.getSyncPiperLocation(), commands.size());
        break;
      case SUSPEND:
        this.defaultPullConsumer.getConsumerStatsManager().incConsumeFailedTPS(processQueue.getSyncPiperLocation(), commands.size());
        if (checkConsumeTime(commands)) {
          log.warn("ConsumeRequest consume again, msg size:" + commands.size());

          processQueue.consumeAgain(commands);
//          submitConsumeRequestLater(processQueue, defaultPullConsumer.getSuspendCurrentQueueTimeMillis());
          continueConsume = false;
        } else {
          this.defaultPullConsumer.getConsumerStatsManager().incConsumeFinalFailedTPS(processQueue.getSyncPiperLocation(), commands.size());

          log.warn("ConsumeRequest already consume max times:" + defaultPullConsumer.getConsumeTimeMax() +
                  ", msg size:" + commands.size() + ",   drop it! ");
          try {
            RedisCommandDeserializer deserializer = new RedisCommandDeserializer();
            StringBuilder sb = new StringBuilder();
            commands.forEach(command -> {
              sb.append("command: ").append(deserializer.deserialize(command.getData())).append(",");
            });
            log.error("drop redis command:" + sb.toString());
          } catch (Throwable e) {
            log.error("drop redis command error", e);
          }
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
