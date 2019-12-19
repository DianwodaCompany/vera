package com.dianwoda.usercenter.vera.piper.ha;

import com.dianwoda.usercenter.vera.common.ThreadFactoryImpl;
import com.dianwoda.usercenter.vera.piper.PiperController;
import com.dianwoda.usercenter.vera.piper.config.PiperConfig;
import com.dianwoda.usercenter.vera.piper.service.ConsumeCommandOrderlyService;
import com.dianwoda.usercenter.vera.store.CommandStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author seam
 */
public class HAServer implements Runnable{
  protected static final Logger log = LoggerFactory.getLogger(HAServer.class);

  private PiperConfig piperConfig;
  private PiperController piperController;
  private Selector selector;
  private ExecutorService executorService;
  private List<HAConnection> connectionList;
  private boolean stop = false;
  private ServerSocketChannel serverSocketChannel;

  public HAServer(PiperController piperController, PiperConfig piperConfig) {
    this.piperConfig = piperConfig;
    this.piperController = piperController;
    this.connectionList = new ArrayList();
    this.executorService = Executors.newSingleThreadExecutor(new ThreadFactoryImpl("HAServerAcceptThread_"));
  }

  public boolean start() throws IOException{

    selector = Selector.open();
    this.serverSocketChannel = ServerSocketChannel.open();
    this.serverSocketChannel.socket().setReuseAddress(true);
    this.serverSocketChannel.socket().bind(new InetSocketAddress(this.piperConfig.masterSyncPort()));
    this.serverSocketChannel.configureBlocking(false);
    this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

    this.executorService.submit(this);
    return true;

  }

  @Override
  public void run() {
    log.info("HAServer run starting");

    while (!isStop()) {
      try {
        selector.select(1000);
        Set<SelectionKey> selectionKeys = selector.selectedKeys();
        Iterator<SelectionKey> iterator = selectionKeys.iterator();

        while (iterator.hasNext()) {
          SelectionKey key = iterator.next();
          iterator.remove();

          if ((key.readyOps() & SelectionKey.OP_ACCEPT) != 0) {
            SocketChannel sc = ((ServerSocketChannel)key.channel()).accept();
            if (sc != null) {
              log.info("HAServer add connection:" + sc.socket().getRemoteSocketAddress());

              HAConnection connection = new HAConnection(this, sc);
              connection.start();
              this.addConnection(connection);
            }
          }
        }

      } catch (Exception e) {
        log.error("HAServer run exception", e);
      }
    }
    this.destory();
    log.info("HAServer stopping");
  }

  public boolean isStop() {
    return stop;
  }

  public void setStop(boolean stop) {
    this.stop = stop;
  }

  public void destory() {
    this.setStop(true);
    this.executorService.shutdownNow();
  }

  public void addConnection(HAConnection connection) {
    synchronized (this.connectionList) {
      this.connectionList.add(connection);
    }
  }

  public void removeConnection(HAConnection connection) {
    synchronized (this.connectionList) {
      this.connectionList.remove(connection);
    }
  }

  public CommandStore getCommandStore() {
    return this.piperController.getCommandStore();
  }
}
