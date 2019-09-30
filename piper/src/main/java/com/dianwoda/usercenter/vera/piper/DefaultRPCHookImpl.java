package com.dianwoda.usercenter.vera.piper;

import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.ThreadFactoryImpl;
import com.dianwoda.usercenter.vera.common.protocol.RequestCode;
import com.dianwoda.usercenter.vera.piper.data.DelayedElement;
import com.dianwoda.usercenter.vera.remoting.RPCHook;
import com.dianwoda.usercenter.vera.remoting.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

/**
 * @author seam
 */
public class DefaultRPCHookImpl implements RPCHook {
  protected static final Logger log = LoggerFactory.getLogger(DefaultRPCHookImpl.class);
  private Map<Integer, Long> rtMap = new ConcurrentHashMap<>();
  final BlockingQueue<DelayedElement<Integer>> deque = new DelayQueue<DelayedElement<Integer>>();
  final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactoryImpl("DefaultRPCHookImplThread_"));

  public DefaultRPCHookImpl() {
  }

  @Override
  public void start() {
    executorService.submit(() -> {
      while (true) {
        try {
          Integer key = deque.take().getData();
          rtMap.remove(key);
        } catch (InterruptedException e) {
          log.error("DefaultRPCHookImpl error", e);
        }
      }
    });
  }

  @Override
  public void stop() {
    executorService.shutdown();
  }

  @Override
  public void doBeforeRequest(String remoteAddr, RemotingCommand request) {
    int opaque = request.getOpaque();
    if (opaque > 0) {
      rtMap.put(opaque, SystemClock.now());
      this.deque.add(new DelayedElement<>(20 * 1000, opaque));
    }
  }

  @Override
  public void doAfterResponse(String remoteAddr, RemotingCommand request, RemotingCommand response) {
    if (request == null || request.getCode() == RequestCode.REGISTER_PIPER) {
      return;
    }
    StringBuilder sb = new StringBuilder();
    sb.append("Request:").append(request).append(" \t Response:").append(response);
    if (response != null) {
      int opaque = response.getOpaque();
      Long start = rtMap.remove(opaque);
      if (start != null) {
        sb.append(" \t RT:" + (SystemClock.now() - start));
      }
    }
    log.info(sb.toString());
  }
}
