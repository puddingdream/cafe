package com.cafe.domain.order.support;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class OrderNumberGenerator {
    // 사용자에게 노출할 주문번호를 생성한다. DB PK와 별도로 UX용 식별자를 둔다.
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String generate() {
        // 초 단위 시간 + 6자리 랜덤 숫자로 사람이 읽을 수 있는 주문번호를 만든다.
        int randomNumber = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "ORD-" + LocalDateTime.now().format(FORMATTER) + "-" + randomNumber;
    }
}
