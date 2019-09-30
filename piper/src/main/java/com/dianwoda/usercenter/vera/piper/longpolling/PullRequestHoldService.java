package com.dianwoda.usercenter.vera.piper.longpolling;

import com.dianwoda.usercenter.vera.common.PullSysFlag;
import com.dianwoda.usercenter.vera.common.protocol.header.PullMessageRequestHeader;
import com.dianwoda.usercenter.vera.piper.PiperController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 将pull request保存起来, 代码实现
 * @author seam
 */
public class PullRequestHoldService {
  protected static final Logger log = LoggerFactory.getLogger(PullRequestHoldService.class);
  private ConcurrentMap<String, ManyPullRequest> pullRequestTable = new ConcurrentHashMap<String, ManyPullRequest>();
  private final PiperController piperController;

  public PullRequestHoldService(final PiperController piperController) {
    this.piperController = piperController;
  }

  public void suspendPullRequest(final String fromLocation, final PullRequest pullRequest) {
    String key = this.buildKey(fromLocation);
    ManyPullRequest manyPullRequest = this.pullRequestTable.get(key);
    if (manyPullRequest == null) {
      manyPullRequest = new ManyPullRequest();
      ManyPullRequest prev = this.pullRequestTable.putIfAbsent(key, manyPullRequest);
      if (prev != null) {
        manyPullRequest = prev;
      }
    }
    manyPullRequest.addPullRequest(pullRequest);
  }

  public boolean notifyMessageArriving(final long writeOffset, final long writeBytes) {

    for (Map.Entry<String, ManyPullRequest> entry : this.pullRequestTable.entrySet()) {

      ManyPullRequest mpr = this.pullRequestTable.get(entry.getKey());
      if (mpr != null) {
        List<PullRequest> requestList = mpr.cloneListAddClear();
        if (requestList != null) {
          for (PullRequest request : requestList) {
            long newestOffset = writeOffset;
            if (newestOffset < request.getPullFromThisOffset()) {
              continue;
            }
            if (newestOffset >= request.getPullFromThisOffset()) {
              try {
                final PullMessageRequestHeader requestHeader = (PullMessageRequestHeader) request.getRequestCommand().decodeCommandCustomHeader(PullMessageRequestHeader.class);
                requestHeader.setSysFlag(PullSysFlag.clearSuspendFlag(requestHeader.getSysFlag()));
                this.piperController.getPullMessageProcessor()
                        .executeRequestWhenWakeup(request.getClientChannel(), request.getRequestCommand());
              } catch (Throwable e) {
                log.error("execute executeRequestWhenWakeup fail.", e);
              }
              continue;
            }
          }
        }
      }
    }
    return true;
  }

  private String buildKey(final String fromLocation) {
    StringBuilder sb = new StringBuilder();
    sb.append(fromLocation);
    return sb.toString();
  }
}
