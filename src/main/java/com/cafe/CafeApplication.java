package com.cafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class CafeApplication {

    public static void main(String[] args) {
        // Spring Boot 애플리케이션의 진입점이다.
        SpringApplication.run(CafeApplication.class, args);
    }

}
