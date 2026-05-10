package com.cafe.infrastructure.redis.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component("redissonLockService")
public class RedissonLockService implements LockService {
    private final RedissonClient redissonClient;

    public RedissonLockService(@Lazy RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void lock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) {
        RLock lock = redissonClient.getLock(key);
        try {
            boolean acquired = leaseTime < 0
                    ? lock.tryLock(waitTime, timeUnit)
                    : lock.tryLock(waitTime, leaseTime, timeUnit);

            if (!acquired) {
                throw new LockException("Redis lock acquisition failed. key=" + key);
            }
            log.debug("Redis lock acquired. key={}", key);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LockException("Redis lock acquisition interrupted. key=" + key, exception);
        }
    }

    @Override
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("Redis lock released. key={}", key);
        }
    }
}
