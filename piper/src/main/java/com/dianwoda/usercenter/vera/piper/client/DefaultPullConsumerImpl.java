package com.dianwoda.usercenter.vera.piper.client;

import com.dianwoda.usercenter.vera.common.MixAll;
import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.protocol.body.ConsumeStatus;
import com.dianwoda.usercenter.vera.common.protocol.body.ConsumerRunningInfo;
import com.dianwoda.usercenter.vera.common.protocol.route.PiperData;
import com.dianwoda.usercenter.vera.piper.client.listener.CommandListenerOrderly;
import com.dianwoda.usercenter.vera.piper.client.listener.DefaultCommandListenerOrderlyImpl;
import com.dianwoda.usercenter.vera.piper.client.protocal.PullResult;
import com.dianwoda.usercenter.vera.piper.client.protocal.SyncPiperPullRequest;
import com.dianwoda.usercenter.vera.piper.client.stat.ConsumerStatsManager;
import com.dianwoda.usercenter.vera.piper.enums.CommunicationMode;
import com.dianwoda.usercenter.vera.piper.enums.RequestExceptionReason;
import com.dianwoda.usercenter.vera.piper.offset.ConsumerOffsetManager;
import com.dianwoda.usercenter.vera.piper.service.ConsumeCommandOrderlyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;

/**
 * 拉取消息的默认实现
 * @author seam
 */
public class DefaultPullConsumerImpl {
  protected static final Logger log = LoggerFactory.getLogger(DefaultPullConsumerImpl.class);
  /**
   * Flow control interval
   */
  private static final long PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL = 1000;
  /**
   * WAIT FOR TIMEOUT
   */
  private static final long PULL_TIME_DELAY_MILLS_WHEN_TIMEOUT = 1000 * 10;
  /**
   * Flow control threshold
   */
  public static  int pullThresholdForQueue = 1000;
  /**
   * Concurrently max span offset.it has no effect on sequential consumption
   */
  public static int consumeConcurrentlyMaxSpan = 200000;
  /**
   * Batch pull size
   */
  private int pullBatchSize = 50;
  /**
   * Minimum consumer thread number
   */
  private int consumeThreadMin = 10;

  /**
   * Max consumer thread number
   */
  private int consumeThreadMax = 10;
  /**
   * Message pull Interval
   */
  private long pullInterval = 20;
  /**
   * Batch consumption size
   */
  private int consumeMessageBatchMaxSize = 5;
  /**
   * Max consume times
   */
  private int consumeTimeMax = 3;
  /**
   * Suspending pulling time for cases requiring slow pulling like flow-control scenario.
   */
  private long suspendCurrentQueueTimeMillis = 1000;

  private static final long PIPER_SUSPEND_MAX_TIME_MILLIS = 1000 * 10;
  private final long consumerStartTimestamp = SystemClock.now();
  private PiperClientInstance piperClientInstance;
  private long flowControlTimes1 = 0;
  private long flowControlTimes2 = 0;
  private PullAPIWrapper pullAPIWrapper;
  private final ConsumeCommandOrderlyService commandOrderlyService;
  private final CommandListenerOrderly commandListener;
  private final ConsumerOffsetManager offsetManager;

  public DefaultPullConsumerImpl(PiperClientInstance piperClientInstance) {
    this.piperClientInstance = piperClientInstance;
    this.offsetManager = this.piperClientInstance.getOffsetManager();
    this.pullAPIWrapper = new PullAPIWrapper(this.piperClientInstance);
    this.commandListener = new DefaultCommandListenerOrderlyImpl(this.piperClientInstance);
    this.commandOrderlyService = new ConsumeCommandOrderlyService(this, this.commandListener);
  }

  public void start() {
    this.commandOrderlyService.start();
  }

  public void pullCommand(SyncPiperPullRequest pullRequest) throws Exception {
    final ProcessQueue processQueue = pullRequest.getProcessQueue();
    if (processQueue.isDropped()) {
      log.info("the pull request [{}] is dropped.", pullRequest.toString());
      return;
    }

    processQueue.setLastPullTimestamp(SystemClock.now());
    int size = processQueue.getMsgCount().get();
    if (size > pullThresholdForQueue) {
      this.piperClientInstance.getPullMessageService().executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL);
      if ((flowControlTimes1++ % 200) == 0) {
        log.warn(String.format("the consumer message buffer is full, so do flow control, minoffset=%d, maxOffset=%d, size=%d, " +
                " pullRequest=%s, flowControlTimes=%d", processQueue.getCommandTreeMap().firstKey(), processQueue.getCommandTreeMap().lastKey(),
                processQueue.getMaxSpan(), pullRequest, flowControlTimes1));
      }
      return;
    }

    if (processQueue.getMaxSpan() > consumeConcurrentlyMaxSpan) {
      this.piperClientInstance.getPullMessageService().executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL);
      if ((flowControlTimes2++ % 200) == 0) {
        log.warn(String.format("the consumer message span too long, so do flow control, " +
                        "minoffset=%d, maxOffset=%d, size=%d, " +
                        "pullRequest=%s, flowControlTimes=%d",
                processQueue.getCommandTreeMap().firstKey(), processQueue.getCommandTreeMap().lastKey(),
                processQueue.getMaxSpan(), pullRequest, flowControlTimes2));
      }
      return;
    }

    final long beginTimestamp = SystemClock.now();
    PullCallback pullCallback = new PullCallback() {
      @Override
      public void onSuccess(PullResult pullResult) {
        if (pullResult != null) {
          pullResult = DefaultPullConsumerImpl.this.pullAPIWrapper.processPullResult(pullResult);
          log.info("pullRequest:" + pullRequest + " ,PullResult:" + pullResult);

          switch (pullResult.getPullStatus()) {
            case FOUND:
              long prevRequestOffset = pullRequest.getNextOffset();
              pullRequest.setNextOffset(pullResult.getNextBeginOffset());
              String targetLocation = pullRequest.getTargetLocation();
              long pullRT = SystemClock.now() - beginTimestamp;
              DefaultPullConsumerImpl.this.getConsumerStatsManager().incPullRT(targetLocation, pullRT);

              if ((pullResult.getCmdFoundList() == null || pullResult.getCmdFoundList().isEmpty()) ||
                      (DefaultPullConsumerImpl.this.offsetManager.getOffset(targetLocation) > prevRequestOffset + pullResult.getCmdFoundList().size())) {
                DefaultPullConsumerImpl.this.piperClientInstance.getPullMessageService().executePullRequestImmediately(pullRequest);
                return;
              } else {
                DefaultPullConsumerImpl.this.getConsumerStatsManager().incPullTPS(targetLocation, pullResult.getCmdFoundList().size());

                ProcessQueue.ProcessQueueStatus processQueueStatus = pullRequest.getProcessQueue().putCommand(pullResult.getCmdFoundList());
                log.info("processQueueStatus:" + processQueueStatus);
                DefaultPullConsumerImpl.this.commandOrderlyService.submitConsumeRequest(pullResult.getCmdFoundList(), pullRequest.getProcessQueue(), processQueueStatus.isDispatchConsume());
                pullRequest.setCommitOffset(offsetManager.getOffset(targetLocation));
                if (DefaultPullConsumerImpl.this.pullInterval > 0) {
                  DefaultPullConsumerImpl.this.piperClientInstance.getPullMessageService().executePullRequestLater(pullRequest, DefaultPullConsumerImpl.this.pullInterval);

                } else {
                  DefaultPullConsumerImpl.this.piperClientInstance.getPullMessageService().executePullRequestImmediately(pullRequest);
                }
              }

              if (prevRequestOffset >= pullResult.getNextBeginOffset()) {
                log.warn(String.format("[BUG] pull message result maybe data wrong, nextBeginOffset: %d prevRequestOffset: %d",
                                pullResult.getNextBeginOffset(), prevRequestOffset));
              }
              break;
            case NO_NEW_MSG:
              DefaultPullConsumerImpl.this.piperClientInstance.getPullMessageService().executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL);
              break;
            case OFFSET_ILLEGAL:
              pullRequest.setNextOffset(pullResult.getNextBeginOffset());
              DefaultPullConsumerImpl.this.piperClientInstance.getPullMessageService().executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL);
            default:
              break;
          }
        }
      }

      @Override
      public void onException(Throwable e, RequestExceptionReason reason) {
        log.warn("execute the pull requesst:" + pullRequest + ",  exception, reason:" + reason, e);
        if (reason == RequestExceptionReason.TIME_OUT) {
          DefaultPullConsumerImpl.this.piperClientInstance.getPullMessageService().executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_TIMEOUT);
        } else {
          DefaultPullConsumerImpl.this.piperClientInstance.getPullMessageService().executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL);
        }
      }
    };

    try {
      this.pullAPIWrapper.pullKernelImpl(pullRequest.getTargetLocation(),
              pullRequest.getTargetGroup(), pullRequest.getNextOffset(),
              pullRequest.getCommitOffset(), true, pullBatchSize, PIPER_SUSPEND_MAX_TIME_MILLIS, CommunicationMode.ASYNC, pullCallback);
    } catch (Exception e) {
      log.error("pullKernelImpl exception", e);
      this.piperClientInstance.getPullMessageService().executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL);
      throw e;
    }
  }


  public ConsumerRunningInfo piperRunningInfo() {
    ConsumerRunningInfo info = new ConsumerRunningInfo();
    Properties prop = MixAll.object2Properties(this);
    prop.setProperty(ConsumerRunningInfo.PROP_CONSUMER_START_TIMESTAMP, String.valueOf(this.consumerStartTimestamp));
    prop.setProperty(ConsumerRunningInfo.PROP_THREADPOOL_CORE_SIZE, String.valueOf(this.commandOrderlyService.getCorePollSize()));

    info.setProperties(prop);
    Map<String, PiperData> piperDataMap = this.piperClientInstance.getActivePiperData().getPiperDataMap();
    for (Map.Entry<String, PiperData> entry : piperDataMap.entrySet()) {
      ConsumeStatus consumeStatus = piperClientInstance.getConsumerStatsManager().consumeStatus(entry.getKey());
      info.getStatusTable().put(entry.getKey(), consumeStatus);
    }

    return info;
  }

  public int getConsumeThreadMin() {
    return consumeThreadMin;
  }

  public int getConsumeThreadMax() {
    return consumeThreadMax;
  }

  public int getConsumeMessageBatchMaxSize() {
    return consumeMessageBatchMaxSize;
  }

  public int getConsumeTimeMax() {
    return consumeTimeMax;
  }

  public long getSuspendCurrentQueueTimeMillis() {
    return suspendCurrentQueueTimeMillis;
  }

  public ConsumerOffsetManager getOffsetManager() {
    return offsetManager;
  }

  public ConsumerStatsManager getConsumerStatsManager() {
    return this.piperClientInstance.getConsumerStatsManager();
  }

  public void setPullAPIWrapper(PullAPIWrapper pullAPIWrapper) {
    this.pullAPIWrapper = pullAPIWrapper;
  }
}
