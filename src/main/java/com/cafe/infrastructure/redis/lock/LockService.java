package com.cafe.infrastructure.redis.lock;

import java.util.concurrent.TimeUnit;

public interface LockService {

    void lock(String key, long waitTime, long leaseTime, TimeUnit timeUnit);

    void unlock(String key);
}
