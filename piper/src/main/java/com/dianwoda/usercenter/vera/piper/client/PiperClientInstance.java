package com.dianwoda.usercenter.vera.piper.client;

import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.protocol.header.GetPiperInfoResponseHeader;
import com.dianwoda.usercenter.vera.common.protocol.route.PiperAllData;
import com.dianwoda.usercenter.vera.common.protocol.route.PiperData;
import com.dianwoda.usercenter.vera.piper.PiperController;
import com.dianwoda.usercenter.vera.piper.client.stat.ConsumerStatsManager;
import com.dianwoda.usercenter.vera.piper.config.PiperConfig;
import com.dianwoda.usercenter.vera.piper.data.ActivePiperData;
import com.dianwoda.usercenter.vera.piper.exception.PiperException;
import com.dianwoda.usercenter.vera.piper.offset.ConsumerOffsetManager;
import com.dianwoda.usercenter.vera.piper.processor.NamerManagerProcessor;
import com.dianwoda.usercenter.vera.remoting.exception.RemotingException;
import com.dianwoda.usercenter.vera.remoting.netty.NettyClientConfig;
import com.dianwoda.usercenter.vera.remoting.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * piper做为客户端的单例实现
 * @author seam
 */
public class PiperClientInstance {
  protected static final Logger log = LoggerFactory.getLogger(PiperClientInstance.class);
  private PullCommandService pullMessageService;
  private DefaultPullConsumerImpl pullConsumerImpl;
  private final NamerManagerProcessor namerManagerProcessor;
  private PiperClientOuterImpl piperClientAPIImpl;
  private PiperClientInterImpl piperClientInterImpl;
  private final NettyClientConfig nettyClientConfig;
  private ConsumerStatsManager consumerStatsManager;
  private ConsumerOffsetManager offsetManager;
  private ActivePiperData activePiperData = new ActivePiperData();
  private ReentrantLock namerOperLock = new ReentrantLock();
  private PiperConfig piperConfig;
  private PiperController piperController;

  // 从namer获取piper的超时时间
  private static final long GET_PIPER_INFO_MAX_TIMEOUT_MILLIS = 1000 * 5;
  // 10分钟内的redis command数据才允许同步
  private static final int COMMAND_POST_DUE_TIME_MILLIS = 1000 * 60 * 10;
  // 同步过来的数据是否按时间过滤的开关
  private boolean syncCommandFilterSwitch = true;

  private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
    @Override
    public Thread newThread(Runnable r) {
      return new Thread(r, "PiperClientInstanceScheduledThread");
    }
  });

  public PiperClientInstance(PiperController piperController, ConsumerOffsetManager offsetManager) {
    this.piperController = piperController;
    this.pullMessageService = new PullCommandService(this);
    this.offsetManager = offsetManager;
    this.pullConsumerImpl = new DefaultPullConsumerImpl(this);
    this.namerManagerProcessor = new NamerManagerProcessor(this);
    this.nettyClientConfig = new NettyClientConfig();
    this.piperConfig = piperController.getPiperConfig();
    this.piperClientAPIImpl = new PiperClientOuterImpl(this, this.nettyClientConfig, this.namerManagerProcessor);
    this.piperClientInterImpl = new PiperClientInterImpl(this);
    this.consumerStatsManager = new ConsumerStatsManager(scheduledExecutorService);

    this.initial();
  }

  public void initial() {
    this.piperClientAPIImpl.updateNamerLocation(this.piperConfig.nameLocation());
  }
  public void start() {
    this.piperClientAPIImpl.start();
    this.pullMessageService.start();
    scheduledExecutorService.scheduleAtFixedRate(() -> {
      PiperClientInstance.this.updatePiperInfoFromNamer();
    }, 1000 * 60, 1000 * 60 * 2, TimeUnit.MILLISECONDS);
  }

  public void stop() {
    this.scheduledExecutorService.shutdown();
    this.piperClientAPIImpl.stop();
  }

  public long getPiperMaxWriteOffset(String syncPiperLocation) {
    try {
      RemotingCommand response = this.piperClientAPIImpl.getPiperInfo(syncPiperLocation, GET_PIPER_INFO_MAX_TIMEOUT_MILLIS);
      GetPiperInfoResponseHeader responseHeader = (GetPiperInfoResponseHeader) response.decodeCommandCustomHeader(GetPiperInfoResponseHeader.class);
      return responseHeader.getMaxWriteOffset();
    } catch (Exception e) {
      log.error("getPiperMaxWriteOffset error", e);
      return -1;
    }
  }

  public void updatePiperInfoFromNamer() {
    try {
      namerOperLock.tryLock(1000 * 3, TimeUnit.MILLISECONDS);
      PiperAllData piperAllData = this.piperClientAPIImpl.updatePiperInfoFromNamer(this.piperConfig.nameLocation(),
              this.piperConfig.location(), GET_PIPER_INFO_MAX_TIMEOUT_MILLIS);
      if (piperAllData != null) {
        List<PiperData> needAdd = new ArrayList<PiperData>();
        for (PiperData piperData : piperAllData.getPiperDataList()) {
          if (!this.activePiperData.containPiperData(piperData)) {
            needAdd.add(piperData);
          }
        }
        List<PiperData> needRemove = new ArrayList<PiperData>();
        Collection<PiperData> values = this.activePiperData.values();
        for (PiperData value : values) {
          if (piperAllData.getPiperDataList().contains(value)) {
            needRemove.add(value);
          }
        }
        this.activePiperData.addPiperDatas(needAdd);
        this.activePiperData.removePiperDatas(needRemove);
        this.activePiperData.setUpdateTime(SystemClock.now());
      }
    } catch (InterruptedException e) {
      log.error("updatePiperInfoFromNamer error", e);
    } catch (PiperException e) {
      log.error("updatePiperInfoFromNamer error", e);
    } catch (RemotingException e) {
      log.error("updatePiperInfoFromNamer error", e);
    } finally {
      namerOperLock.unlock();
    }
  }

  public PullCommandService getPullMessageService() {
    return pullMessageService;
  }

  public DefaultPullConsumerImpl getPullConsumerImpl() {
    return pullConsumerImpl;
  }

  public PiperClientOuterImpl getPiperClientAPIImpl() {
    return piperClientAPIImpl;
  }

  public ConsumerStatsManager getConsumerStatsManager() {
    return consumerStatsManager;
  }

  public ConsumerOffsetManager getOffsetManager() {
    return offsetManager;
  }

  public void setOffsetManager(ConsumerOffsetManager offsetManager) {
    this.offsetManager = offsetManager;
  }

  public PiperController getPiperController() {
    return piperController;
  }

  public PiperClientInterImpl getPiperClientInterImpl() {
    return piperClientInterImpl;
  }


  public ActivePiperData getActivePiperData() {
    return activePiperData;
  }

  public int getCommandPostDueTimeMillis() {
    return COMMAND_POST_DUE_TIME_MILLIS;
  }

  public boolean isSyncCommandFilterSwitch() {
    return syncCommandFilterSwitch;
  }
}
