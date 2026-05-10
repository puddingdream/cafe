# Assignment Checklist

> 기준: 현재 저장소의 코드, README, docs, 테스트 기준

## 필수 요구사항

| 요구사항 | 상태 | 구현 / 문서 |
| --- | --- | --- |
| 커피 메뉴 목록 조회 API | 충족 | `GET /api/menus`, [API Overview](api-overview.md#menu) |
| 포인트 충전 API | 충족 | `POST /api/points/charge`, [API Overview](api-overview.md#point) |
| 커피 주문 / 결제 API | 충족 | `POST /api/orders`, [API Overview](api-overview.md#order) |
| 주문 내역 실시간 전송 | 충족 | Spring Event `AFTER_COMMIT` -> Kafka, [Problem Solving Strategy](problem-solving-strategy.md#주문-이벤트-처리-전략) |
| 최근 7일 인기 메뉴 Top 3 조회 | 충족 | V1 RDB 집계, V2 Redis ZSET, [Design Rationale](design-rationale.md#인기-메뉴-설계) |
| 메뉴별 주문 횟수 정확성 | 충족 | V1은 RDB `PAID` 주문 기준 집계, 취소 주문 제외 테스트 |

## README 필수 첨부 항목

| 요구사항 | 상태 | 문서 |
| --- | --- | --- |
| 설계 내용 - ERD | 충족 | [ERD / Table Overview](erd/schema-overview.md) |
| 설계 내용 - API 명세서 | 충족 | [API Overview](api-overview.md) |
| 설계의 의도 | 충족 | [Design Rationale](design-rationale.md) |
| 선택한 문제해결 전략 및 분석 | 충족 | [Problem Solving Strategy](problem-solving-strategy.md) |
| 기술적 선택 이유 | 충족 | [Technical Decisions](technical-decisions.md) |

## 도전 요구사항

| 요구사항 | 상태 | 구현 / 문서 |
| --- | --- | --- |
| 다수 서버 / 다수 인스턴스 환경 고려 | 충족 | Redis 분산락, Redis Cache, Kafka topic, MySQL 원본 저장소 |
| 동시성 이슈 고려 | 충족 | `@RedisLock`, 지갑 비관락, [Testing / Verification](testing-verification.md#동시성-테스트-요약) |
| 데이터 일관성 고려 | 충족 | 주문/포인트 트랜잭션, V1 RDB 기준선, Spring Event `AFTER_COMMIT` |
| 기능 및 제약사항 테스트 | 충족 | 통합 테스트, 동시성 테스트, 성능 테스트 |

## 주요 테스트 근거

| 구분 | 테스트 |
| --- | --- |
| 포인트 | `PointServiceIntegrationTest`, `PointConcurrencyIntegrationTest` |
| 주문 | `OrderServiceIntegrationTest`, `OrderConcurrencyIntegrationTest` |
| 인기 메뉴 V1 | `OrderStatisticsReaderIntegrationTest` |
| 인기 메뉴 V2 | `PopularMenuRankingServiceIntegrationTest` |
| 메뉴 캐시 | `MenuServiceCacheIntegrationTest`, `MenuCachePerformanceTest` |
| 인기 메뉴 성능 | `PopularMenuPerformanceTest` |

## 성능 검증 문서

| 문서 | 내용 |
| --- | --- |
| [Popular Menu Performance](performance/popular-menu-performance.md) | 인기 메뉴 V1 RDB 집계와 V2 Redis ZSET 비교 |
| [Menu Cache Performance](performance/menu-cache-performance.md) | 카테고리 메뉴 조회 cache miss / hit 비교 |

## 남긴 한계와 확장안

| 항목 | 내용 |
| --- | --- |
| Redis/Kafka 도입 범위 | 과제 규모에서는 오버엔지니어링일 수 있으나, 다중 인스턴스와 이벤트 기반 read model 학습 목적으로 도입 |
| Outbox Pattern | 현재 미적용. DB 커밋 후 Kafka 발행 실패 보강이 필요하면 outbox table과 publisher 추가 |
| 인기 메뉴 V2 | 현재 7일치 일별 ZSET을 조회 시점에 합산. 더 최적화하려면 `popular:menus:rolling:7d` 유지 |
| 포인트 충전 API | 실제 운영에서는 PG 결제 검증 후 내부 충전 호출이 적합 |
