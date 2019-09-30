package com.dianwoda.usercenter.vera.piper.redis;

import com.dianwoda.usercenter.vera.common.redis.command.RedisCommand;
import com.dianwoda.usercenter.vera.piper.PiperController;
import com.dianwoda.usercenter.vera.piper.client.listener.NotifyCommandArriveListener;
import com.dianwoda.usercenter.vera.piper.redis.serializer.RedisCommandDeserializer;
import com.dianwoda.usercenter.vera.piper.redis.serializer.RedisCommandSerializer;
import com.dianwoda.usercenter.vera.piper.longpolling.PullRequestHoldService;
import com.dianwoda.usercenter.vera.store.*;
import com.dianwoda.usercenter.vera.store.io.ObjectSerializer;
import com.dianwoda.usercenter.vera.store.stats.PiperStatsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 接收Redis数据后的操作扩展
 * @author seam
 */
public class DefaultRedisReplicatorListener implements RedisReplicatorListener {
  private Logger logger = LoggerFactory.getLogger(DefaultRedisReplicatorListener.class);

  private CommandStore commandStore;
  private ObjectSerializer<RedisCommand> serializer;

  public DefaultRedisReplicatorListener(CommandStore commandStore) {
    this.commandStore = commandStore;
    this.serializer = new RedisCommandSerializer();
  }

  @Override
  public void receive(RedisCommand command) {
    long beginTimestamp = System.currentTimeMillis();
    byte[] data = serializer.serialize(command);
    if (data == null || data.length == 0) {
      logger.warn("RedisCommand is null, redis command:" + command);
      return;
    }
    PutCommandResult result = commandStore.appendCommand(data);
    long eclipseTimestamp = System.currentTimeMillis() - beginTimestamp;
    if (eclipseTimestamp > 100) {
      logger.warn(String.format("[NOTIFYME]putMessage in lock cost time(ms)=%d, bodyLength=%d AppendMessageResult=%s", eclipseTimestamp,
              command.getValue() == null ? 0 : command.getValue().length, result));

    }
    if (result.getPutCommandStatus() != PutCommandStatus.PUT_OK) {
      logger.error(String.format("[ERROR]putMessage error cost time(ms)=%d, message=%s AppendMessageResult=%s", eclipseTimestamp,
              command, result));
    }
  }

  public static void main(String[] args) {
    PiperController controller = new PiperController(null);
    HashMap<Integer, String> piperLocations = new HashMap<>();
    piperLocations.put(0,  "192.168.102.254:8025");
    List<String> consumers = new ArrayList<String>();
    String targetLocation = "192.168.102.254:8026";
    consumers.add(targetLocation);

    PullRequestHoldService pullRequestHoldService = new PullRequestHoldService(controller);
    NotifyCommandArriveListener commandArrivingListener = new NotifyCommandArriveListener(pullRequestHoldService);
    PiperStatsManager piperStatsManager = new PiperStatsManager("hz-unit1");
    CommandStore storeHandler = new DefaultCommandStore("test", piperStatsManager, commandArrivingListener);
    RedisReplicatorListener listener = new DefaultRedisReplicatorListener(storeHandler);
    RedisCommand command = new RedisCommand();
    command.setType((byte)1);
    command.setKey(new String("key4").getBytes());
    command.setValue(new String("value4").getBytes());

    try {
      listener.receive(command);
    } catch (Exception e) {
    } finally {
    }

    GetCommandResult result = storeHandler.getCommand(targetLocation, 0, 100);
    List<ByteBuffer> list = (List<ByteBuffer>)(result.getCommandBufferList());
    RedisCommandDeserializer deserializer = new RedisCommandDeserializer();
    for (ByteBuffer data : list) {
      RedisCommand redisCommand = deserializer.deserialize(data.array());
      System.out.println(redisCommand);
    }

  }
}
