package com.dianwoda.usercenter.vera.piper.test;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * redis缓存
 *
 * @author yinxiaolin
 * @since 1.2
 */
public class CacheService {

    private static final Logger LOG = LoggerFactory.getLogger(CacheService.class);

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
      this.redisTemplate = redisTemplate;
    }

    private RedisTemplate<String, Object> redisTemplate;

    public void putCacheWithExpire(String key, Object value, int expire) {
        try {
            redisTemplate.opsForValue().set(key, value, expire, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.error("--- PUT cache exception [key=" + key + ", value=" + value + ", expire=" + expire + "].", e);
        }
    }

    public boolean putAbsentWithExpire(String key, Object value, int expire, TimeUnit timeUnit) {
        try {
            if(redisTemplate.opsForValue().setIfAbsent(key, value)) {
                redisTemplate.expire(key, expire, timeUnit);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOG.error("--- putAbsentWithExpire exception [key=" + key + ", value=" + value + ", expire=" + expire + "].", e);
            return false;
        }
    }

    public void putCache(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            LOG.error("--- PUT cache exception [key=" + key + ", value=" + value + "].", e);
        }
    }

    public Object getCache(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            LOG.error("--- GET cache exception [key=" + key + "].", e);
        }
        return null;
    }

    public void removeCache(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            LOG.error("--- Remove cache exception [key=" + key + "].", e);
        }
    }




    public Long zSetSize(String key) {
        try {
            return redisTemplate.opsForZSet().size(key);
        } catch (Exception e) {
            LOG.error("--- zSetSize exception key={}", key, e);
        }
        return null;
    }


    public void hashPut(String key, Object hashKey, Object value) {
        try {
            redisTemplate.opsForHash().put(key, hashKey, value);
        } catch (Exception e) {
            LOG.error(String.format("--- hashPut exception key=%s hashKey=%s value=%s",
                    key, JSON.toJSONString(hashKey), JSON.toJSONString(value)), e);
        }
    }

    public Object hashGet(String key, Object hashKey) {
        try {
            return redisTemplate.opsForHash().get(key, hashKey);
        } catch (Exception e) {
            LOG.error(String.format("--- hashGet exception key=%s hashKey=%s",
                    key, JSON.toJSONString(hashKey)), e);
        }
        return null;
    }


    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    public List<Object> mget(List<String> keys) {
        try {
            return  this.redisTemplate.opsForValue().multiGet(keys);
        } catch (Exception e) {
            LOG.error("--- redis mget exception keys{} cause:{}", JSON.toJSONString(keys), e);
            return null;
        }
    }
}
