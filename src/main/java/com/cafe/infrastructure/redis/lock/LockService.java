package com.cafe.infrastructure.redis.lock;

import java.util.concurrent.TimeUnit;

public interface LockService {
    // 분산락 구현체가 제공해야 하는 최소 동작이다.
    // 현재 구현체는 Redisson이지만, 인터페이스를 두면 테스트용/다른 락 구현으로 교체하기 쉽다.

    void lock(String key, long waitTime, long leaseTime, TimeUnit timeUnit);

    void unlock(String key);
}
