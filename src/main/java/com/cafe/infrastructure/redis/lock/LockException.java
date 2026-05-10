package com.cafe.infrastructure.redis.lock;

public class LockException extends RuntimeException {
    // Redis 분산락 획득/해제 과정에서 발생하는 인프라 예외다.

    public LockException(String message) {
        super(message);
    }

    public LockException(String message, Throwable cause) {
        super(message, cause);
    }
}
