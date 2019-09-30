package com.dianwoda.usercenter.vera.piper.client;

import com.dianwoda.usercenter.vera.common.ServiceThread;
import com.dianwoda.usercenter.vera.common.ThreadFactoryImpl;
import com.dianwoda.usercenter.vera.piper.client.protocal.SyncPiperPullRequest;
import com.dianwoda.usercenter.vera.remoting.exception.RemotingConnectException;
import com.dianwoda.usercenter.vera.remoting.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 命令拉取service, 将动作转为队列实现
 * @author seam
 */
public class PullCommandService extends ServiceThread {
  protected static final Logger log = LoggerFactory.getLogger(PullCommandService.class);
  private final LinkedBlockingQueue<SyncPiperPullRequest> pullRequestQueue = new LinkedBlockingQueue<>();
  private PiperClientInstance piperClientInstance;
  private final ScheduledExecutorService scheduledExecutorService = Executors.
          newSingleThreadScheduledExecutor(new ThreadFactoryImpl("PullMessageService_"));

  public PullCommandService(PiperClientInstance piperClientInstance) {
    this.piperClientInstance = piperClientInstance;
  }

  public void executePullRequestImmediately(final SyncPiperPullRequest pullRequest) {
    try {
      this.pullRequestQueue.put(pullRequest);

    } catch (InterruptedException e) {
      log.error("PullMessageService executePullRequestImmediately pullRequestQueue.put", e);
    }
  }

  public void executePullRequestLater(final SyncPiperPullRequest pullRequest, long delay) {
    this.scheduledExecutorService.schedule(() -> PullCommandService.this.executePullRequestImmediately(pullRequest),
            delay, TimeUnit.MILLISECONDS);
  }

  @Override
  public String getServiceName() {
    return PullCommandService.class.getSimpleName();
  }

  // 拉取commands
  private void pullCommand(final SyncPiperPullRequest pullRequest) throws Exception {
    this.piperClientInstance.getPullConsumerImpl().pullCommand(pullRequest);
  }

  @Override
  public void run() {
    log.info(this.getServiceName() + " service started");
    while (!this.isStopped()) {
      try {
        SyncPiperPullRequest pullRequest = this.pullRequestQueue.take();
        if (pullRequest != null) {
          pullCommand(pullRequest);
        }
      } catch (InterruptedException e) {
        log.error("Pull Command Service Run Method exception", e);
      } catch (RemotingConnectException e) {
        int sleep = 1000 * 5;
        log.error("RemotingConnectException error, sleep " + sleep + "ms");
        try {
          Thread.sleep(sleep);
        } catch (InterruptedException e1) {
          e1.printStackTrace();
        }
      } catch (Exception e) {
        log.error("Pull Command Service Run Method exception", e);
      }
    }

    log.info(this.getServiceName() + " service end");
  }
}
