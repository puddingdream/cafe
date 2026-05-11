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
    // Redis 기반이라 동일 JVM 안의 synchronized와 달리 여러 애플리케이션 인스턴스 사이에서도 같은 key로 락을 공유한다.
    private final RedissonClient redissonClient;

    public RedissonLockService(@Lazy RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void lock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) {
        // waitTime 동안 락 획득을 시도하고, leaseTime이 지나면 자동 해제되게 한다.
        // leaseTime은 장애 상황에서 락이 영원히 남는 것을 막는 안전장치지만, 너무 짧으면 작업 중 락이 풀릴 수 있다.
        RLock lock = redissonClient.getLock(key);
        try {
            boolean acquired = leaseTime < 0
                    ? lock.tryLock(waitTime, timeUnit)
                    : lock.tryLock(waitTime, leaseTime, timeUnit);

            if (!acquired) {
                // 같은 key의 작업이 이미 진행 중이면 즉시 실패시켜 중복 주문/중복 차감 가능성을 줄인다.
                throw new LockException("Redis lock acquisition failed. key=" + key);
            }
            log.debug("Redis lock acquired. key={}", key);
        } catch (InterruptedException exception) {
            // 인터럽트 상태를 복원해야 상위 실행 환경이 취소 요청을 인지할 수 있다.
            Thread.currentThread().interrupt();
            throw new LockException("Redis lock acquisition interrupted. key=" + key, exception);
        }
    }

    @Override
    public void unlock(String key) {
        // 현재 스레드가 보유한 락만 해제해 다른 요청의 락을 잘못 풀지 않게 한다.
        // leaseTime 만료 후 다른 요청이 같은 key의 락을 얻었을 수도 있으므로 isHeldByCurrentThread를 확인한다.
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("Redis lock released. key={}", key);
        }
    }
}
