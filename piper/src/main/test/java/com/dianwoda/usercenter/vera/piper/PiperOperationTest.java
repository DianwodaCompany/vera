package com.dianwoda.usercenter.vera.piper;

import com.dianwoda.usercenter.vera.common.SystemClock;
import com.dianwoda.usercenter.vera.common.message.CommandDecoder;
import com.dianwoda.usercenter.vera.common.message.CommandExt;
import com.dianwoda.usercenter.vera.common.message.GetCommandStatus;
import com.dianwoda.usercenter.vera.common.protocol.header.ListenRedisRequestHeader;
import com.dianwoda.usercenter.vera.common.redis.command.RedisCommand;
import com.dianwoda.usercenter.vera.common.util.IPUtil;
import com.dianwoda.usercenter.vera.common.util.NetUtils;
import com.dianwoda.usercenter.vera.piper.client.ProcessQueue;
import com.dianwoda.usercenter.vera.piper.client.PullAPIWrapper;
import com.dianwoda.usercenter.vera.piper.client.protocal.PullResultExt;
import com.dianwoda.usercenter.vera.piper.client.protocal.SyncPiperPullRequest;
import com.dianwoda.usercenter.vera.piper.config.PiperConfig;
import com.dianwoda.usercenter.vera.piper.enums.CommunicationMode;
import com.dianwoda.usercenter.vera.piper.enums.PullStatus;
import com.dianwoda.usercenter.vera.piper.redis.command.CommandType;
import com.dianwoda.usercenter.vera.piper.redis.serializer.RedisCommandDeserializer;
import com.dianwoda.usercenter.vera.remoting.netty.NettyServerConfig;
import com.dianwoda.usercenter.vera.store.DefaultCommandStore;
import com.dianwoda.usercenter.vera.store.GetCommandResult;
import com.dianwoda.usercenter.vera.store.io.ObjectDeserializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.moilioncircle.redis.replicator.Status.CONNECTED;
import static org.mockito.Mockito.*;

/**
 * @author seam
 */
//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = Application.class)
//@ActiveProfiles(value = "hz-unit1")
public class PiperOperationTest extends com.dianwoda.usercenter.vera.piper.UnitTestBase {
  private static String storePath = "/Users/seam/git/vera/test/unit1/data";
  private static int PIPER_BIND_PORT = 16571;
  private static String group = "hz-unit1";
  private static String nameLocation = "127.0.0.1:16581";
  private String masterName = "mymaster";
  private String password = "foobared";
  private List<String> sentinels = new ArrayList();
  private File storeDir = new File(storePath);

  ObjectDeserializer<RedisCommand> deserializer = new RedisCommandDeserializer();

  @Before
  public void before() {
    sentinels.add("127.0.0.1:26379");
    delFile(storeDir);
  }


  @After
  public void after() {
//    delFile(storeDir);
  }

  @Test
  public void cleanRedisTest() throws Exception {
    PiperConfig piperConfig = mockPiperConfig();

    NettyServerConfig nettyServerConfig = new NettyServerConfig();
    nettyServerConfig.setListenPort(PIPER_BIND_PORT);
    piperConfig.setHostName(NetUtils.getLocalHostname());

    PiperController piper = PiperFactory.make(piperConfig);
    piper.setNettyServerConfig(nettyServerConfig);
    ((DefaultCommandStore) (piper.getCommandStore())).blockFileSize = 1024;

    ListenRedisRequestHeader requestHeader = this.createListenRedisRequest(0);
    piper.getPiperClientInstance().getPiperClientInterImpl().redisReplicatorInitial(requestHeader);

    // clean redis command
    this.clearRedisRecord(piper);
  }


  @Test
  public void commandWriteAndQueryTest() throws Exception {
    PiperController piper = mockPiper();

    ListenRedisRequestHeader requestHeader = this.createListenRedisRequest(0);
    piper.getPiperClientInstance().getPiperClientInterImpl().redisReplicatorInitial(requestHeader);
    piper.getPiperClientInstance().getPiperClientInterImpl().redisReplicatorRun();

    while( piper.getPiperClientInstance().getPiperClientInterImpl().getRedisFacadeProcessor()
            .getSlaveRedisReplicator().getReplicator().getStatus() != CONNECTED) {

      Thread.sleep(100);
      System.out.println("wait sleeping");
    }

    Thread.sleep(1000);

    // write redis command
    this.createRedisRecord(piper);

    while(!storeDir.exists()) {
      Thread.sleep(1000);
    }
    Thread.sleep(1000);

    String location = piper.getPiperConfig().location();
    long readOffset = 0;
    int msgNums = 100;
    // query redis command from block file
    GetCommandResult getCommandResult = piper.getCommandStore().getCommand(location, readOffset, msgNums);
    this.outputResult(getCommandResult);

    // change next offset, but no data
    readOffset = getCommandResult.getNextBeginOffset();
    getCommandResult = piper.getCommandStore().getCommand(location, readOffset, msgNums);
    this.outputResult(getCommandResult);

    // change next block file
    readOffset = getCommandResult.getNextBeginOffset();
    getCommandResult = piper.getCommandStore().getCommand(location, readOffset, msgNums);
    this.outputResult(getCommandResult);
  }


  @Test
  public void pullCommandTest() throws Exception {
    PiperController piper = mockPiper();
    ListenRedisRequestHeader requestHeader = this.createListenRedisRequest(0);
    piper.getPiperClientInstance().getPiperClientInterImpl().redisReplicatorInitial(requestHeader);
    piper.getPiperClientInstance().getPiperClientInterImpl().redisReplicatorRun();

    while( piper.getPiperClientInstance().getPiperClientInterImpl().getRedisFacadeProcessor()
            .getSlaveRedisReplicator().getReplicator().getStatus() != CONNECTED) {

      Thread.sleep(100);
      System.out.println("wait sleeping");
    }

    Thread.sleep(100);

    // write redis command
    this.createRedisRecord(piper);

    while(!storeDir.exists()) {
      Thread.sleep(1000);
    }
    Thread.sleep(1000);

    String location = piper.getPiperConfig().location();
    long readOffset = 0;
    int msgNums = 100;
    // query redis command from block file
    GetCommandResult getCommandResult = piper.getCommandStore().getCommand(location, readOffset, msgNums);
    byte[] binary = readGetMessageResult(getCommandResult);

    piper.getPiperClientInstance().start();

    PullResultExt pullResultExt = new PullResultExt(PullStatus.FOUND, getCommandResult.getNextBeginOffset(),
            getCommandResult.getMinOffset(), getCommandResult.getMaxOffset(), null, binary);
    PullAPIWrapper2 pullAPIWrapper = new PullAPIWrapper2(piper.getPiperClientInstance());
    pullAPIWrapper.setPullResult(pullResultExt);

    piper.getPiperClientInstance().getPullConsumerImpl().setPullAPIWrapper(pullAPIWrapper);

    SyncPiperPullRequest pullRequest = new SyncPiperPullRequest();
    pullRequest.setNextOffset(0);
    pullRequest.setCommitOffset(0);
    pullRequest.setTargetLocation(piper.getPiperConfig().location());
    ProcessQueue pq = new ProcessQueue(piper.getPiperConfig().location());
    pullRequest.setProcessQueue(pq);
    piper.getPiperClientInstance().
            getPullMessageService().executePullRequestImmediately(pullRequest);

    int i = 5;
    while(i-- > 0) {
      Thread.sleep(1000);
    }

    Thread.sleep(1000);
    while (pq.getMaxSpan() > 0) {
      Thread.sleep(1000);
    }
  }

  private PiperController mockPiper() {
    PiperConfig piperConfig = mockPiperConfig();

    NettyServerConfig nettyServerConfig = new NettyServerConfig();
    nettyServerConfig.setListenPort(PIPER_BIND_PORT);
    piperConfig.setHostName(NetUtils.getLocalHostname());

    DefaultCommandStore.blockFileSize = 1024;
    PiperController piper = PiperFactory.make(piperConfig);
    piper.setNettyServerConfig(nettyServerConfig);
    return piper;
  }

  public byte[] readGetMessageResult(final GetCommandResult getMessageResult) {
    final ByteBuffer byteBuffer = ByteBuffer.allocate(getMessageResult.getBufferTotalSize());
    long storeTimestamp = 0;
    try {
      List<ByteBuffer> messageBufferList = getMessageResult.getCommandBufferList();
      for (ByteBuffer bb : messageBufferList) {



        byteBuffer.put(bb);
        storeTimestamp = bb.getLong(CommandDecoder.STORE_TIMESTAMP_POSITION);
      }
    } finally {
      getMessageResult.release();
    }
    return byteBuffer.array();
  }

  private void createRedisRecord(PiperController piperController) throws Exception {
    RedisCommand command = new RedisCommand();
    command.setType(CommandType.SET.getValue());

    for (int i=0; i<10; i++) {
      command.setKey(new String("key" + i).getBytes());
      command.setValue(new String("value" + i).getBytes());

      piperController.getPiperClientInstance().getPiperClientInterImpl()
              .getRedisFacadeProcessor().getRedisWriter().write(command);
    }
  }

  private void clearRedisRecord(PiperController piperController) throws Exception {
    RedisCommand command = new RedisCommand();
    command.setType(CommandType.DEL.getValue());

    for (int i=0; i<10; i++) {
      command.setKey(new String("key" + i).getBytes());
      command.setValue(new String("value" + i).getBytes());

      piperController.getPiperClientInstance().getPiperClientInterImpl()
              .getRedisFacadeProcessor().getRedisWriter().delete(command);
    }
  }

  private void outputResult(GetCommandResult result) {
    if (result.getStatus() == GetCommandStatus.FOUND) {
      List<ByteBuffer> byteBufferList = result.getCommandBufferList();

      ArrayList<CommandExt> commandExtList = new ArrayList<>();

      byteBufferList.stream().forEach(byteBuffer -> {
        commandExtList.addAll(CommandDecoder.decodes(byteBuffer));
      });

      commandExtList.stream().forEach(commandExt -> {
        RedisCommand redisCommand = deserializer.deserialize(commandExt.getData());
        System.out.println("Write into redis success, redisCommand: " + redisCommand);
      });
    }
  }

  private ListenRedisRequestHeader createListenRedisRequest(int operateType) {
    ListenRedisRequestHeader requestHeader = new ListenRedisRequestHeader();
    requestHeader.setMasterName(masterName);
    requestHeader.setSentinals(sentinels.stream().collect(Collectors.joining(",")));
    requestHeader.setPassword(password);
    requestHeader.setOperateType(operateType);
    return requestHeader;
  }

  private PiperConfig mockPiperConfig() {
    PiperConfig piperConfig = mock(PiperConfig.class);
    when(piperConfig.storePath()).thenReturn(storePath);
    when(piperConfig.getHostName()).thenReturn(NetUtils.getLocalHostname());
    when(piperConfig.group()).thenReturn(group);
    when(piperConfig.piperId()).thenReturn(0);
    when(piperConfig.nameLocation()).thenReturn(nameLocation);
    when(piperConfig.location()).thenReturn(IPUtil.getServerIp() + ":" + PIPER_BIND_PORT);
    return piperConfig;
  }
}
