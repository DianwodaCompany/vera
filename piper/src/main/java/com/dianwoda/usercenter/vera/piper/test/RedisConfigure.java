package com.dianwoda.usercenter.vera.piper.test;



import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * @author seam
 * just for test
 */
public class RedisConfigure {

  public static CacheManager cacheManager;
  String masterName = "mymaster";
  String sentinels = "127.0.0.1:26379";
  String password = "foobared";

  public RedisConfigure() {
    JedisPoolConfig poolConfig = new JedisPoolConfig();

    RedisSentinelConfiguration config = new RedisSentinelConfiguration();
    config.master(masterName);
    config.setSentinels(createSentinels(sentinels));
    JedisConnectionFactory connectionFactory =  new JedisConnectionFactory(config, poolConfig);
    connectionFactory = applyProperties(connectionFactory);
    connectionFactory.afterPropertiesSet();
    RedisConfigure.cacheManager = new CacheManager(redisTemplate(connectionFactory));

  }

  protected final JedisConnectionFactory applyProperties(
          JedisConnectionFactory factory) {
    factory.setPassword(password);
    return factory;
  }


  private RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new JdkSerializationRedisSerializer());
    template.setValueSerializer(new JdkSerializationRedisSerializer());
    template.setHashValueSerializer(new JdkSerializationRedisSerializer());
    template.afterPropertiesSet();
    return template;
  }


  private List<RedisNode> createSentinels(String sentinels) {
    List<RedisNode> nodes = new ArrayList<RedisNode>();
    for (String node : StringUtils
            .commaDelimitedListToStringArray(sentinels)) {
      try {
        String[] parts = StringUtils.split(node, ":");
        Assert.state(parts.length == 2, "Must be defined as 'host:port'");
        nodes.add(new RedisNode(parts[0], Integer.valueOf(parts[1])));
      }
      catch (RuntimeException ex) {
        throw new IllegalStateException(
                "Invalid redis sentinel " + "property '" + node + "'", ex);
      }
    }
    return nodes;
  }
}
