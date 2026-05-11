package com.cafe.infrastructure.redis.lock;

public class LockException extends RuntimeException {
    // Redis 분산락 획득/해제 과정에서 발생하는 인프라 예외다.
    // 현재는 공통 500 예외로 처리되지만, 필요하면 별도 ErrorCode로 409/429 응답을 줄 수 있다.

    public LockException(String message) {
        super(message);
    }

    public LockException(String message, Throwable cause) {
        super(message, cause);
    }
}
