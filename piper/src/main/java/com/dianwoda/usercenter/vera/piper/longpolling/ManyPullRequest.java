package com.dianwoda.usercenter.vera.piper.longpolling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author seam
 */
public class ManyPullRequest {
  private volatile ArrayList<PullRequest> pullRequestArrayList = new ArrayList<PullRequest>();


  public synchronized void addPullRequest(final PullRequest pullRequest) {
    Map<io.netty.channel.Channel, List<PullRequest>> map = pullRequestArrayList.stream().collect(Collectors.groupingBy(PullRequest::getClientChannel));
    if (map.containsKey(pullRequest.getClientChannel())) {
      List<PullRequest> list = map.get(pullRequest.getClientChannel());
      PullRequest maxOffsetPullRequest = list.stream().max((a, b) -> (a.getSuspendTimestamp() > b.getSuspendTimestamp() && a.getPullFromThisOffset() > b.getPullFromThisOffset()) ? 1 : -1).get();
      if (pullRequest.getPullFromThisOffset() >= maxOffsetPullRequest.getPullFromThisOffset()) {
        map.put(pullRequest.getClientChannel(), Collections.singletonList(pullRequest));
      }
    } else {
      map.put(pullRequest.getClientChannel(), Collections.singletonList(pullRequest));
    }
    ArrayList<PullRequest> newPullRequest = new ArrayList<>();
    map.values().stream().forEach(pullRequests -> newPullRequest.addAll(pullRequests));
    this.pullRequestArrayList = newPullRequest;
  }


  public synchronized List<PullRequest> cloneListAddClear() {
    if (!this.pullRequestArrayList.isEmpty()) {
      List<PullRequest> result = (ArrayList<PullRequest>) this.pullRequestArrayList.clone();
      this.pullRequestArrayList.clear();
      return result;
    }
    return null;
  }

}
