package com.campus.secondhand.util;

import cn.hutool.core.util.IdUtil;
import com.campus.secondhand.common.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.function.Supplier;

/**
 * Redis 分布式锁工具类
 * <p>
 * 实现：SETNX（获取锁）+ 唯一 token + Lua 原子释放（防误删他人锁）+ 自旋等待
 * <p>
 * 重要：如果锁内操作带 @Transactional，锁释放会早于事务提交导致锁失效。
 * 此时应使用 TransactionTemplate 编程式事务，确保事务提交在 unlock 之前。
 */
@Component
public class RedisLockUtil {

    private final StringRedisTemplate redisTemplate;

    /** Lua 脚本：仅当 token 匹配时才删除 key，保证"比对+删除"原子执行 */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
            Long.class);

    public RedisLockUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试获取锁（非阻塞）
     * @return 持有者 token（成功）或 null（失败）
     */
    public String tryLock(String key, Duration ttl) {
        String token = IdUtil.fastSimpleUUID();
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(ok) ? token : null;
    }

    /**
     * 释放锁（仅当仍由自己持有时才删除）
     */
    public boolean unlock(String key, String token) {
        Long result = redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
        return result != null && result > 0;
    }

    /**
     * 带锁执行：自旋等待 waitTime，超时抛业务异常。
     * <p>
     * Redis 故障时异常直接向上抛出，由调用方决定降级策略。
     * 如果 action 内部需要事务，请使用 TransactionTemplate 编程式事务。
     */
    public <T> T executeWithLock(String key, Duration ttl, Duration waitTime, Supplier<T> action) {
        String token = null;
        long deadline = System.currentTimeMillis() + waitTime.toMillis();
        try {
            while (token == null) {
                token = tryLock(key, ttl);
                if (token == null) {
                    if (System.currentTimeMillis() > deadline) {
                        throw new BusinessException("当前操作人数较多，请稍后再试");
                    }
                    Thread.sleep(50);
                }
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("请求被中断，请重试");
        } finally {
            if (token != null) {
                unlock(key, token);
            }
        }
    }
}
