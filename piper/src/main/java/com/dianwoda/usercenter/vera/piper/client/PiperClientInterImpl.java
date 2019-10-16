package com.dianwoda.usercenter.vera.piper.client;

import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.ThreadFactoryImpl;
import com.dianwoda.usercenter.vera.common.enums.ConsumeFromWhere;
import com.dianwoda.usercenter.vera.common.protocol.header.ListenRedisRequestHeader;
import com.dianwoda.usercenter.vera.common.protocol.header.SyncPiperRequestHeader;
import com.dianwoda.usercenter.vera.common.protocol.hearbeat.PiperTaskData;
import com.dianwoda.usercenter.vera.common.protocol.route.TaskState;
import com.dianwoda.usercenter.vera.piper.client.protocal.SyncPiperPullRequest;
import com.dianwoda.usercenter.vera.piper.redis.facade.RedisFacadeProcessor;
import com.dianwoda.usercenter.vera.piper.redis.RedisConfig;
import com.dianwoda.usercenter.vera.piper.data.ActivePiperData;
import com.dianwoda.usercenter.vera.piper.offset.ConsumerOffsetManager;
import com.moilioncircle.redis.replicator.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.moilioncircle.redis.replicator.Status.DISCONNECTED;
import static com.moilioncircle.redis.replicator.Status.DISCONNECTING;

/**
 * piper做为客户端对内的实现
 * @author seam
 */
public class PiperClientInterImpl {
  protected static final Logger log = LoggerFactory.getLogger(PiperClientInterImpl.class);

  private PiperClientInstance piperClientInstance;
  private RedisFacadeProcessor redisFacadeProcessor;
  private volatile TaskState listenRedisState = TaskState.BLANK;
  private Map<String /* sync piper location */, TaskState> syncPiperStateMap = new ConcurrentHashMap<String, TaskState>();
  private Map<String /* sync piper location */, ProcessQueue> syncPiperProcessMap = new ConcurrentHashMap<>();
  private ScheduledExecutorService scheduleExceutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("PiperClientInteThread_"));

  public PiperClientInterImpl(PiperClientInstance piperClientInstance) {
    this.piperClientInstance = piperClientInstance;
    this.redisFacadeProcessor = new RedisFacadeProcessor();
  }

  public void start() {
    // check redis replicator status
    scheduleExceutor.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        if (redisFacadeProcessor.getSlaveRedisReplicator().getReplicator() != null) {
          Status status = redisFacadeProcessor.getSlaveRedisReplicator().getReplicator().getStatus();
          if (status == DISCONNECTING || status == DISCONNECTED) {
            listenRedisState = TaskState.TASK_LISTEN_REDIS_ABORT;
          }
        }
      }
    }, 1000 * 5, 1000 * 60, TimeUnit.MILLISECONDS);

    // check sync piper status
    scheduleExceutor.scheduleAtFixedRate(new Runnable() {
      ActivePiperData activePiperData = piperClientInstance.getActivePiperData();
      @Override
      public void run() {
        for (String location : syncPiperStateMap.keySet()) {
          if (!activePiperData.containPiperData(location)) {
            syncPiperStateMap.put(location, TaskState.TASK_SYNC_PIPER_ABORT);
          }
        }
      }
    }, 1000 * 6, 1000 * 60, TimeUnit.MILLISECONDS);
  }

  public PiperTaskData getPiperTaskInfo() {
    PiperTaskData taskData = new PiperTaskData();
    taskData.setMasterName(redisFacadeProcessor.getRedisConfig() != null ? redisFacadeProcessor.getRedisConfig().getMasterName() : null);
    taskData.setSentinelList(redisFacadeProcessor.getRedisConfig() != null ? redisFacadeProcessor.getRedisConfig().getSentinalList() : null);
    taskData.setPassword(redisFacadeProcessor.getRedisConfig() != null ? redisFacadeProcessor.getRedisConfig().getPassword() : null);
    taskData.setListenRedisState(this.listenRedisState);
    taskData.setSyncPiperStateMap(this.syncPiperStateMap);
    taskData.setUpdateTime(SystemClock.now());
    return taskData;
  }

  public boolean checkListenRedisOperation(Integer operateType) {
    switch (operateType) {
      case 0:  // 初始化
        if (this.listenRedisState == TaskState.BLANK || this.listenRedisState == TaskState.TASK_LISTEN_REDIS_ABORT) {
          return true;
        }
        break;
      case 1:   // 运行
        if (this.listenRedisState == TaskState.TASK_LISTEN_REDIS_INITIAL) {
          return true;
        }
        break;
      case 2:   // 停止
        if (this.listenRedisState != TaskState.BLANK) {
          return true;
        }
        break;
    }
    return false;
  }

  public boolean redisReplicatorInitial(ListenRedisRequestHeader requestHeader) {
    RedisConfig redisConfig = new RedisConfig(requestHeader);
    boolean result = false;
    if (!redisFacadeProcessor.isInitial()) {
      result = this.redisFacadeProcessor.initial(redisConfig, this.piperClientInstance);
    } else if (!this.redisFacadeProcessor.isSameRedisCluster(redisConfig)){
      result = this.redisFacadeProcessor.reConfigure(redisConfig, this.piperClientInstance);
    }
    if (result) {
      this.listenRedisState = TaskState.TASK_LISTEN_REDIS_INITIAL;
    }
    return result;
  }

  public boolean redisReplicatorRun() {
    try {
      this.redisFacadeProcessor.start();
      this.listenRedisState = TaskState.TASK_LISTEN_REDIS_RUNNING;
    } catch (Exception e) {
      log.error("listen redis task start exception", e);
      this.listenRedisState = TaskState.TASK_SYNC_PIPER_ABORT;
      return false;
    }
    return true;
  }

  public boolean redisReplicatorStop() {
    try {
      if (this.redisFacadeProcessor != null) {
        this.redisFacadeProcessor.stop();
      }
      this.listenRedisState = TaskState.TASK_LISTEN_REDIS_ABORT;
    } catch (Exception e) {
      log.error("listen redis task start exception", e);
      return false;
    }
    return true;
  }

  public boolean checkSyncPiperOperation(String location, Integer operateType) {
    TaskState taskState = this.syncPiperStateMap.get(location);

    switch (operateType) {
      case 0:  // 初始化
        if (taskState == null || taskState == TaskState.BLANK || taskState == TaskState.TASK_SYNC_PIPER_ABORT) {
          return true;
        }
        break;
      case 1:   // 运行
        if (taskState == TaskState.TASK_SYNC_PIPER_INITIAL) {
          return true;
        }
        break;
      case 2:   // 停止
        if (taskState != TaskState.BLANK) {
          return true;
        }
        break;
    }
    return false;
  }

  public synchronized boolean syncPiperInitial(String location) {
    this.syncPiperStateMap.put(location, TaskState.TASK_SYNC_PIPER_INITIAL);
    return true;
  }

  public synchronized boolean syncPiperRunning(SyncPiperRequestHeader requestHeader) {
    String syncPiperLocation = requestHeader.getSyncPiperLocation();
    ConsumerOffsetManager consumerOffsetManager = this.piperClientInstance.getPiperController().getOffsetManager();
    long commitOffset = consumerOffsetManager.getOffset(syncPiperLocation);

    ConsumeFromWhere consumeFromWhere = ConsumeFromWhere.getConsumeFromWhere(requestHeader.getConsumeFromWhere());
    long nextOffset = -1;
    switch (consumeFromWhere) {
      case CONSUME_FROM_LAST_OFFSET:
        nextOffset = this.getPiperMaxWriteOffset(syncPiperLocation);
        break;
      case CONSUME_FROM_LOCAL_OFFSET:
        nextOffset = commitOffset;
        break;
    }

    if (nextOffset < 0) {
      return false;
    }
    SyncPiperPullRequest pullRequest = new SyncPiperPullRequest();
    pullRequest.setNextOffset(nextOffset);
    pullRequest.setCommitOffset(nextOffset);
    pullRequest.setTargetLocation(syncPiperLocation);
    ProcessQueue pq = this.createProcessQueue(syncPiperLocation);
    pullRequest.setProcessQueue(pq);
    this.piperClientInstance.getPiperController().getOffsetManager().
            commitOffsetForce(syncPiperLocation, nextOffset);
    this.piperClientInstance.getPiperController().getPiperClientInstance().
            getPullMessageService().executePullRequestImmediately(pullRequest);
    this.syncPiperStateMap.put(syncPiperLocation, TaskState.TASK_SYNC_PIPER_RUNNING);
    return true;
  }

  public synchronized boolean syncPiperStop(String syncPiperLocation) {
    this.syncPiperStateMap.put(syncPiperLocation, TaskState.TASK_SYNC_PIPER_ABORT);
    this.stopProcessQueue(syncPiperLocation);
    return true;
  }

  private long getPiperMaxWriteOffset(String syncPiperLocation) {
    return this.piperClientInstance.getPiperMaxWriteOffset(syncPiperLocation);
  }

  private ProcessQueue createProcessQueue(String syncPiperLocation) {
    ProcessQueue pq = new ProcessQueue(syncPiperLocation);
    ProcessQueue old = this.syncPiperProcessMap.put(syncPiperLocation, pq);
    if (old != null) {
      pq = old;
      pq.reset();
    }
    return pq;
  }

  private ProcessQueue stopProcessQueue(String syncPiperLocation) {
    ProcessQueue pq = this.syncPiperProcessMap.get(syncPiperLocation);
    if (pq != null) {
      pq.close();
    }
    return pq;
  }

  public RedisFacadeProcessor getRedisFacadeProcessor() {
    return redisFacadeProcessor;
  }

  public Map<String, TaskState> getSyncPiperStateMap() {
    return syncPiperStateMap;
  }

  public TaskState getSyncPiperState(String syncPiperLocation) {
    return this.syncPiperStateMap.get(syncPiperLocation);
  }

  public TaskState getListenRedisState() {
    return listenRedisState;
  }
}
