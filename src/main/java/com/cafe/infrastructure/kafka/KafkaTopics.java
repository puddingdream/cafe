package com.cafe.infrastructure.kafka;

public final class KafkaTopics {
    // Kafka topic 이름을 한 곳에서 관리해 producer/consumer가 같은 값을 사용하게 한다.
    // v1 suffix는 이벤트 스키마가 바뀔 때 새 토픽(v2)으로 분리할 수 있게 남긴 버전 표기다.
    public static final String ORDER_PAID = "cafe.order-paid.v1";
    public static final String ORDER_CANCELED = "cafe.order-canceled.v1";
    public static final String ORDER_PAID_DLT = "cafe.order-paid.dlt.v1";
    public static final String ORDER_CANCELED_DLT = "cafe.order-canceled.dlt.v1";

    // 로컬/과제 환경은 단일 Kafka 브로커를 기준으로 replication factor 1을 사용한다.
    // 운영에서 broker를 3대 이상 구성한다면 replication factor도 같이 올리는 편이 좋다.
    public static final int DEFAULT_PARTITIONS = 3;
    public static final short DEFAULT_REPLICATION_FACTOR = 1;

    private KafkaTopics() {
    }
}
