package com.cafe.infrastructure.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;

@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    @Lazy
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${spring.data.redis.password:}") String password
    ) {
        // 단일 Redis 서버 기준 RedissonClient를 만든다. 운영에서 cluster/sentinel로 확장 가능하다.
        // 현재 주소는 redis:// 이므로 TLS가 꺼진 Redis/ElastiCache 구성을 전제로 한다.
        // TLS를 켠 ElastiCache를 쓰려면 rediss://와 Spring Data Redis SSL 설정을 함께 맞춰야 한다.
        Config config = new Config();
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port);

        if (StringUtils.hasText(password)) {
            // AUTH 토큰이 설정된 Redis를 사용할 때만 password를 넣는다.
            singleServerConfig.setPassword(password);
        }

        return Redisson.create(config);
    }
}
