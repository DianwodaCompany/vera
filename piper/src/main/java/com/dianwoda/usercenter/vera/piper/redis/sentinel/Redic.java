package com.dianwoda.usercenter.vera.piper.redis.sentinel;

import com.dianwoda.usercenter.vera.common.redis.command.RedisCommand;
import com.dianwoda.usercenter.vera.piper.redis.command.CommandType;
import com.dianwoda.usercenter.vera.piper.redis.sentinel.strategy.HashShardingStrategy;
import com.dianwoda.usercenter.vera.piper.redis.sentinel.strategy.ShardingStrategy;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

import java.util.*;

public class Redic extends Jedis {
  protected final Logger log = LoggerFactory.getLogger(Redic.class);
  private List<RedicNode> redicNodes = new ArrayList<RedicNode>();
  private ShardingStrategy shardingStategy = new HashShardingStrategy();
  private boolean readWriteSeparate = false;
  private List<Map> nodeConns;
  private String password;

  public Redic(List<Map> nodeConns, String password) {
    if (nodeConns == null || nodeConns.isEmpty()) {
      log.error("The nodeConns {} for Redic is invalid.", nodeConns);
      throw new IllegalArgumentException("The nodeConnStrs for Redic is invalid.");
    }
    this.password = password;
    this.nodeConns = nodeConns;
    init();
  }

  public Redic(String masterName, List<String> sentinels, String password) {
    Map map =  new HashMap();
    map.put("master", masterName);
    map.put("sentinels", sentinels);
    List<Map> nodeConns = new ArrayList();
    nodeConns.add(map);
    this.password = password;
    this.nodeConns = nodeConns;
    init();
  }

  public void init() {
    for (Map nodeConnMap : nodeConns) {
      this.addNode(nodeConnMap);
    }
    log.info("There are {} nodes.", redicNodes.size());
  }

  public void addNode(Map nodeConnMap) {
    String masterName = (String) nodeConnMap.get("master");
    List<String> sentinels = (List<String>) nodeConnMap.get("sentinels");
    redicNodes.add(new RedicNode(masterName, sentinels, this.password,
            new GenericObjectPoolConfig(), 5, null));
  }

  protected <T> Jedis getWrite(T key) {
    int nodeIndex = shardingStategy.key2node(key, redicNodes.size());
    RedicNode redicNode = redicNodes.get(nodeIndex);
    return redicNode.getMaster().getResource();
  }


  public HostAndPort getRandomMaster() {
    int nodeIndex = shardingStategy.key2node(redicNodes.size());
    RedicNode redicNode = redicNodes.get(nodeIndex);
    return redicNode.getMasterAddr();
  }

  protected <T> Jedis getRead(T key) {
    Jedis jedis = null;
    int nodeIndex = shardingStategy.key2node(key, redicNodes.size());
    RedicNode redicNode = redicNodes.get(nodeIndex);
    if (!readWriteSeparate) {
      jedis = redicNode.getMaster().getResource();
    } else {
      jedis = redicNode.getRoundRobinSlaveRedicNode().getResource();
    }
    return jedis;
  }

  public void write(RedisCommand command) throws Exception {
    CommandType type = CommandType.toEnum(command.getType());
    try {
      switch (type) {
        case SET:
          this.saveValueByKey(command.getKey(), command.getValue(), Long.valueOf(command.getExpiredValue()).intValue());
          break;
        case SET_EX:
          this.setex(command.getKey(), Long.valueOf(command.getExpiredValue()).intValue(), command.getValue());
          break;
        case SET_NX:
          this.setnx(command.getKey(), command.getValue());
          break;
        case INCR:
          this.incr(command.getKey());
          break;
        case DECR:
          this.decr(command.getKey());
          break;
        case SADD:
          this.sadd(command.getKey(), command.getMembers());
          break;
        case HSET:
          this.hset(command.getKey(), command.getField(), command.getValue());
          break;
        case HMSET:
          this.hmset(command.getKey(), command.getFields());
          break;
        case LSET:
          this.lset(command.getKey(), command.getIndex(), command.getValue());
          break;
        case DEL:
          this.del(command.getDelKeys());
          break;
        default:
          break;
      }
    } catch (Throwable e) {
      log.warn("write error, key:" + new String(command.getKey()));
      throw e;
    }
  }

  public void delete(RedisCommand command) throws Exception {
    CommandType type = CommandType.toEnum(command.getType());
    switch (type) {
      case DEL:
        this.del(command.getKey());
        break;
      default:
        break;
    }
  }

  public void saveValueByKey(byte[] key, byte[] value, int expireTime)
          throws Exception {
    Jedis jedis = null;
    try {
      jedis = getWrite(key);
      jedis.set(key, value);
      if (expireTime > 0) {
        jedis.expire(key, expireTime);
      }
    } catch (Exception e) {
      throw e;
    } finally {
      if (jedis != null) {
        jedis.close();
      }
    }
  }

  @Override
  public Long setnx(byte[] key, byte[] value) {
    Jedis jedis = null;
    Long ret = null;
    try {
      jedis = getWrite(key);
      ret = jedis.setnx(key, value);
    } catch (Exception e) {
      throw e;
    } finally {
      if (jedis != null) {
        jedis.close();
      }
    }
    return ret;
  }

  @Override
  public String setex(final byte[] key, final int seconds, final byte[] value) {
    Jedis jedis = null;
    String ret = null;
    try {
      jedis = getWrite(key);
      ret = jedis.setex(key, seconds, value);
    } catch (Exception e) {
      throw e;
    } finally {
      if (jedis != null) {
        jedis.close();
      }
    }
    return ret;
  }

  @Override
  public String get(String key) {
    Jedis jedis = null;
    String ret = null;
    try {
      jedis = getRead(key);
      ret = jedis.get(key);

    } catch (Exception e) {
      throw e;
    } finally {
      if (jedis != null) {
        jedis.close();
      }
    }
    return ret;
  }


  @Override
  public Long expire(String key, int seconds) {
    Jedis jedis = null;
    Long ret = null;
    try {
      jedis = getWrite(key);
      ret = jedis.expire(key, seconds);
    } catch (Exception e) {
      throw e;
    } finally {
      if (jedis != null) {
        jedis.close();
      }
    }
    return ret;
  }

  @Override
  public Long expireAt(String key, long unixTime) {
    Jedis jedis = null;
    Long ret = null;
    try {
      jedis = getWrite(key);
      ret = jedis.expireAt(key, unixTime);
    } catch (Exception e) {
      throw e;
    } finally {
      if (jedis != null) {
        jedis.close();
      }
    }
    return ret;
  }


  @Override
  public Long decr(byte[] key) {
    Jedis jedis = null;
    Long ret = null;
    try {
      jedis = getWrite(key);
      ret = jedis.decr(key);
    } catch (Exception e) {
      throw e;
    } finally {
      if (jedis != null) {
        jedis.close();
      }
    }
    return ret;
  }


  @Override
  public Long incr(byte[] key) {
    Jedis jedis = null;
    Long ret = null;
    try {
      jedis = getWrite(key);
      ret = jedis.incr(key);
    } catch (Exception e) {
      throw e;
    } finally {
      if (jedis != null) {
        jedis.close();
      }
    }
    return ret;
  }


  /**
   * 返回指定key序列值
   * @param key
   * @return
   */
  @Override
  public Long sadd(byte[] key, byte[][] members){
    Jedis jedis = null;
    Long ret = null;
    try {
      jedis = getWrite(key);
      ret = jedis.sadd(key, members);
    } catch (Exception e) {
      throw e;
    } finally {
      if (jedis != null) {
        jedis.close();
      }
    }
    return ret;
  }


  @Override
  public Long hset(byte[] key, byte[] field, byte[] value) {
    Jedis jedis = null;
    Long ret = null;
    try {
      jedis = getWrite(key);
      ret = jedis.hset(key, field, value);
    } catch (Exception e) {
      throw e;
    } finally {
      if (jedis != null) {
        jedis.close();
      }
    }
    return ret;
  }

  @Override
  public byte[] hget(byte[] key, byte[] field) {
    Jedis jedis = null;
    byte[] ret = null;
    try {
      jedis = getWrite(key);
      ret = jedis.hget(key, field);
    } catch (Exception e) {
      throw e;
    } finally {
      if (jedis != null) {
        jedis.close();
      }
    }
    return ret;
  }

  @Override
  public String hmset(byte[] key, Map<byte[], byte[]> hash) {
    Jedis jedis = null;
    String ret = null;
    try {
      jedis = getWrite(key);
      ret = jedis.hmset(key, hash);
    } catch (Exception e) {
      throw e;
    } finally {
      if (jedis != null) {
        jedis.close();
      }
    }
    return ret;
  }

  @Override
  public String lset(final byte[] key, final long index, final byte[] value) {
    Jedis jedis = null;
    String ret = null;
    try {
      jedis = getWrite(key);
      ret = jedis.lset(key, index, value);
    } catch (Exception e) {
      throw e;
    } finally {
      if (jedis != null) {
        jedis.close();
      }
    }
    return ret;
  }

  @Override
  public Long del(byte[] key) {
    Jedis jedis = null;
    Long ret = null;
    try {
      jedis = getWrite(key);
      ret = jedis.del(key);
    } catch (Exception e) {
      throw e;
    } finally {
      if (jedis != null) {
        jedis.close();
      }
    }
    return ret;
  }

  @Override
  public Long del(byte[]... key) {
    Jedis jedis = null;
    Long ret = null;
    try {
      jedis = getWrite(key);
      ret = jedis.del(key);
    } catch (Exception e) {
      throw e;
    } finally {
      if (jedis != null) {
        jedis.close();
      }
    }
    return ret;
  }

  @Override
  public void close() {

    if (redicNodes != null) {
      redicNodes.stream().forEach(node -> {
        node.shutdown();
      });
    }
  }


  public List<RedicNode> getRedicNodes() {
    return redicNodes;
  }

  public void setRedicNodes(List<RedicNode> redicNodes) {
    this.redicNodes = redicNodes;
  }

  public boolean isReadWriteSeparate() {
    return readWriteSeparate;
  }

  public void setReadWriteSeparate(boolean readWriteSeparate) {
    this.readWriteSeparate = readWriteSeparate;
  }


  public static void main(String args[]) throws Exception {

    Map map =  new HashMap();
    map.put("master", "mymaster");

    List<String> sentinels = new ArrayList();
    sentinels.add("127.0.0.1:26379");

    map.put("sentinels", sentinels);
    List<Map> redisClients = new ArrayList();
    redisClients.add(map);
    Redic redic = new Redic("mymaster", sentinels, "foobared");
    redic.saveValueByKey("test".getBytes(), "test_value".getBytes(), 0);
    System.out.println(redic.get("test"));

    redic.del("test".getBytes());
    System.out.println(redic.get("test"));
    List<RedicNode> nodes = redic.getRedicNodes();

    while(true) {

      for(RedicNode node : nodes) {
        node.printJedisInfos();
      }

      Thread.sleep(3500);
    }

  }
}
