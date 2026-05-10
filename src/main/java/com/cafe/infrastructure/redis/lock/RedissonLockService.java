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
    // Redisson RLock을 사용하는 LockService 구현체다.
    private final RedissonClient redissonClient;

    public RedissonLockService(@Lazy RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void lock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) {
        // waitTime 동안 락 획득을 시도하고, leaseTime이 지나면 자동 해제되게 한다.
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
        // 현재 스레드가 보유한 락만 해제해 다른 요청의 락을 잘못 풀지 않게 한다.
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("Redis lock released. key={}", key);
        }
    }
}
