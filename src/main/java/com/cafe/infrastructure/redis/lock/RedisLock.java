package com.cafe.infrastructure.redis.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisLock {
    // 메서드에 붙여 Redis 분산락을 AOP로 적용한다.
    // 예: @RedisLock(key = "'lock:order:member:' + #loginUser.id()")

    // SpEL 표현식이다. 메서드 파라미터 이름(#loginUser) 또는 #p0/#a0 형태를 사용할 수 있다.
    String key();

    // 락을 얻기 위해 대기할 최대 시간이다. 이 시간 안에 못 얻으면 LockException을 던진다.
    long waitTime() default 5;

    // 락을 얻은 뒤 자동 해제될 시간이다. 서버 장애로 unlock이 호출되지 않아도 영구 락을 방지한다.
    long leaseTime() default 10;

    // waitTime/leaseTime의 시간 단위다.
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
