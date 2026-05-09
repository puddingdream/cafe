package com.cafe.domain.order.support;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class OrderNumberGenerator {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String generate() {
        int randomNumber = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "ORD-" + LocalDateTime.now().format(FORMATTER) + "-" + randomNumber;
    }
}
