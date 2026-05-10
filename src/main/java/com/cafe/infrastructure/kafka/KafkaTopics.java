package com.cafe.infrastructure.kafka;

public final class KafkaTopics {
    public static final String ORDER_PAID = "cafe.order-paid.v1";
    public static final String ORDER_CANCELED = "cafe.order-canceled.v1";
    public static final String ORDER_PAID_DLT = "cafe.order-paid.dlt.v1";
    public static final String ORDER_CANCELED_DLT = "cafe.order-canceled.dlt.v1";
    public static final int DEFAULT_PARTITIONS = 3;
    public static final short DEFAULT_REPLICATION_FACTOR = 1;

    private KafkaTopics() {
    }
}
