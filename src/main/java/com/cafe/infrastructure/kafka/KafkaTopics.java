package com.cafe.infrastructure.kafka;

public final class KafkaTopics {
    // Kafka topic 이름을 한 곳에서 관리해 producer/consumer가 같은 값을 사용하게 한다.
    public static final String ORDER_PAID = "cafe.order-paid.v1";
    public static final String ORDER_CANCELED = "cafe.order-canceled.v1";
    public static final String ORDER_PAID_DLT = "cafe.order-paid.dlt.v1";
    public static final String ORDER_CANCELED_DLT = "cafe.order-canceled.dlt.v1";
    public static final int DEFAULT_PARTITIONS = 3;
    public static final short DEFAULT_REPLICATION_FACTOR = 1;

    private KafkaTopics() {
    }
}
