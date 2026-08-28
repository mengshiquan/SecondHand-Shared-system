package com.campus.secondhand.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

/**
 * Redis 缓存工具类：读写统一降级策略
 * <p>
 * 读取失败返回 null（回落 DB），写入失败静默忽略。
 * 缓存只加速、不依赖——Redis 故障时业务不受影响。
 */
@Component
public class RedisCacheUtil {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheUtil.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            return (T) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis 读取失败，回落 DB。key={}", key, e);
            return null;
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.warn("Redis 写入失败。key={}", key, e);
        }
    }

    public void delete(String... keys) {
        try {
            redisTemplate.delete(Arrays.asList(keys));
        } catch (Exception e) {
            log.warn("Redis 删除失败。keys={}", Arrays.toString(keys), e);
        }
    }
}
