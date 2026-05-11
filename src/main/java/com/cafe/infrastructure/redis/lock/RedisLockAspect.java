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
    // 주문 생성처럼 같은 사용자에 대한 동시 요청을 한 인스턴스뿐 아니라 여러 인스턴스 사이에서도 직렬화한다.
    private final LockService lockService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public RedisLockAspect(@Qualifier("redissonLockService") LockService lockService) {
        this.lockService = lockService;
    }

    @Around("@annotation(redisLock)")
    public Object around(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {
        // 트랜잭션보다 바깥에서 락을 잡아 같은 사용자 요청이 동시에 트랜잭션에 진입하지 않게 한다.
        // @Order를 높게 둔 이유도 락 획득 후 비즈니스 로직/트랜잭션이 시작되는 흐름을 만들기 위해서다.
        String key = resolveKey(joinPoint, redisLock.key());

        lockService.lock(key, redisLock.waitTime(), redisLock.leaseTime(), redisLock.timeUnit());
        try {
            return joinPoint.proceed();
        } finally {
            // 비즈니스 예외가 발생해도 현재 스레드가 잡은 락은 반드시 해제한다.
            lockService.unlock(key);
        }
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        // SpEL 표현식에서 #loginUser.id(), #p0 같은 파라미터 참조를 사용할 수 있게 컨텍스트를 구성한다.
        // - #p0, #a0: 컴파일 옵션과 무관하게 첫 번째 인자를 참조할 수 있는 안전한 별칭
        // - #loginUser: -parameters 옵션으로 보존된 실제 파라미터명을 사용할 수 있는 별칭
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
            // 빈 key로 락을 잡으면 서로 다른 작업이 같은 락을 공유하거나 unlock 대상이 불명확해진다.
            throw new LockException("Redis lock key must not be blank.");
        }
        return key;
    }
}
