package com.cafe.infrastructure.redis.lock;

import java.util.concurrent.TimeUnit;

public interface LockService {
    // 분산락 구현체가 제공해야 하는 최소 동작이다.

    void lock(String key, long waitTime, long leaseTime, TimeUnit timeUnit);

    void unlock(String key);
}
