package com.cafe.infrastructure.redis;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {
    // RedisTemplate과 Spring Cache용 RedisCacheManager 설정이다.
    // 이 프로젝트에서 Redis는 메뉴 캐시, 인기 메뉴 ZSET, Redisson 분산락까지 여러 용도로 사용된다.

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, JsonMapper jsonMapper) {
        // key는 문자열, value는 JSON으로 저장해 사람이 Redis key를 확인하기 쉽게 한다.
        // StringRedisTemplate은 랭킹 ZSET처럼 문자열 기반 작업에 쓰고, 이 RedisTemplate은 객체 캐시/확장 작업에 사용할 수 있다.
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJacksonJsonRedisSerializer jsonSerializer = new GenericJacksonJsonRedisSerializer(jsonMapper);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, JsonMapper jsonMapper) {
        // cache name별 TTL을 다르게 둔다. 메뉴는 비교적 길게, 인기 메뉴는 짧게 유지한다.
        // 메뉴 정보는 자주 바뀌지 않지만 관리자 수정 시 @CacheEvict로 무효화한다.
        // 인기 메뉴는 주문/취소 이벤트로 계속 변할 수 있어 짧은 TTL을 둔다.
        GenericJacksonJsonRedisSerializer jsonSerializer = new GenericJacksonJsonRedisSerializer(jsonMapper);
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                // prefix를 명시해 일반 Redis key, ZSET key와 Spring Cache key를 구분하기 쉽게 한다.
                .computePrefixWith(cacheName -> "cache:" + cacheName + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration)
                .withInitialCacheConfigurations(Map.of(
                        CacheNames.MENUS, cacheConfiguration.entryTtl(Duration.ofMinutes(5)),
                        CacheNames.POPULAR_MENUS, cacheConfiguration.entryTtl(Duration.ofSeconds(30))
                ))
                .build();
    }
}
