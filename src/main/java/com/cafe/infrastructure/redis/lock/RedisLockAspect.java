package com.cafe.infrastructure.redis.lock;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RedisLockAspect {
    // @RedisLock이 붙은 메서드 실행 전후로 락 획득/해제를 처리한다.
    private final LockService lockService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public RedisLockAspect(@Qualifier("redissonLockService") LockService lockService) {
        this.lockService = lockService;
    }

    @Around("@annotation(redisLock)")
    public Object around(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {
        // 트랜잭션보다 바깥에서 락을 잡아 같은 사용자 요청이 동시에 트랜잭션에 진입하지 않게 한다.
        String key = resolveKey(joinPoint, redisLock.key());

        lockService.lock(key, redisLock.waitTime(), redisLock.leaseTime(), redisLock.timeUnit());
        try {
            return joinPoint.proceed();
        } finally {
            lockService.unlock(key);
        }
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        // SpEL 표현식에서 #loginUser.id(), #p0 같은 파라미터 참조를 사용할 수 있게 컨텍스트를 구성한다.
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);

        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int index = 0; index < args.length; index++) {
            context.setVariable("p" + index, args[index]);
            context.setVariable("a" + index, args[index]);
            if (parameterNames != null && parameterNames.length > index) {
                context.setVariable(parameterNames[index], args[index]);
            }
        }

        String key = parser.parseExpression(keyExpression).getValue(context, String.class);
        if (!StringUtils.hasText(key)) {
            throw new LockException("Redis lock key must not be blank.");
        }
        return key;
    }
}
