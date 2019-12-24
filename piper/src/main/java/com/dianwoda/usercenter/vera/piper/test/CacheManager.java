package com.dianwoda.usercenter.vera.piper.test;

import com.google.common.collect.Lists;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * redis缓存, 对业务key附加前缀<br>
 * 异地多活:应用拆分后gw/unit各一份
 *
 * @author yinxiaolin
 * @since 1.2
 */
public class CacheManager {
  private CacheService cacheService;

  public CacheManager(RedisTemplate<String, Object> redisTemplate) {
    this.cacheService = new CacheService(redisTemplate);
  }

  public String buildRedisKey(String key) {
    return String.format("%s_%s", this.getPrefix(), key);
  }

  public List<String> batchBuildRedisKey(List<String> keys) {
    List<String> results = Lists.newArrayListWithCapacity(keys.size());
    for (String key : keys) {
      results.add(buildRedisKey(key));
    }
    return results;
  }

  public Object get(String key) {
    return cacheService.getCache(this.buildRedisKey(key));
  }

  public void put(String key, Object value) {
    cacheService.putCache(this.buildRedisKey(key), value);
  }

  /**
   * 写入redis
   *
   * @param key
   * @param value
   * @param timeout 过期时间(秒)
   */
  public void put(String key, Object value, int timeout) {
    cacheService.putCacheWithExpire(this.buildRedisKey(key), value, timeout);
  }

  public void del(String key) {
    cacheService.removeCache(this.buildRedisKey(key));
  }


  public void hashPut(String key, Object hashKey, Object value) {
    cacheService.hashPut(this.buildRedisKey(key), hashKey, value);
  }

  public Object hashGet(String key, Object hashKey) {
    return cacheService.hashGet(this.buildRedisKey(key), hashKey);
  }


  public List<Object> mget(List<String> keys) {
    List<String> buildKeys = batchBuildRedisKey(keys);
    return cacheService.mget(buildKeys);
  }

  public boolean putAbsentWithExpire(String key, Object value, int expire, TimeUnit timeUnit) {
    return cacheService.putAbsentWithExpire(buildRedisKey(key), value, expire, timeUnit);
  }

  private String getPrefix() {
    return "rider_contract_config";
  }
}
