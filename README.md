# Cafe Order System

> 다수 서버 환경에서도 안정적으로 동작하는 커피숍 주문 시스템  
> Spring Boot 4 · Java 21 · MySQL · Redis · Kafka

이 프로젝트는 커피 메뉴 조회, 포인트 충전, 주문/결제, 최근 7일 인기 메뉴 조회를 구현하는 채용 사전과제 연습 프로젝트입니다.

단순 CRUD 구현보다 다음 질문에 답할 수 있는 구조를 목표로 합니다.

- 다수 서버 / 다수 인스턴스 환경에서도 주문과 포인트 정합성이 깨지지 않는가
- 동시에 같은 사용자가 주문하거나 포인트를 변경해도 잔액이 정확한가
- 인기 메뉴 집계는 RDB 기준 정합성과 Redis 기반 조회 성능 사이에서 어떻게 설계할 것인가
- 선택한 Redis, Kafka, Redisson, 캐시 전략이 과제 규모에서 어떤 장점과 한계를 갖는가

## 로컬 실행

### 1. 환경 변수 준비

`.env.example`을 참고해 `.env`를 구성합니다.

필수 JWT 설정:

```text
JWT_SECRET_KEY=
JWT_ACCESS_TOKEN_VALIDITY_TIME=3600000
JWT_REFRESH_TOKEN_VALIDITY_TIME=1209600000
```

### 2. 로컬 인프라 실행

```bash
docker compose up -d
```

기본 포트:

| Service | Port |
| --- | --- |
| MySQL | `33306` |
| Redis | `6379` |
| Kafka external listener | `9094` |
| Kafka UI | `8989` |

### 3. 애플리케이션 실행

```bash
./gradlew.bat bootRun
```

## 과제 요구사항 대응

| 요구사항 | 구현 상태 | 주요 구현 | 문서 |
| --- | --- | --- | --- |
| 설계 내용(ERD, API 명세서) | 구현 | ERDCloud, API 표 | [ERD](docs/erd/schema-overview.md), [API](docs/api-overview.md) |
| 설계의 의도 | 구현 | 도메인 책임 분리, 메뉴/포인트/주문/인기 메뉴 설계 | [설계 의도](docs/design-rationale.md) |
| 문제 해결 전략 및 분석 | 구현 | 동시성, 일관성, 다중 인스턴스, 이벤트 전략 | [문제 해결 전략](docs/problem-solving-strategy.md) |
| 기술적 선택 이유 | 구현 | MySQL, Redis, Redisson, Kafka, Security, Storage | [기술 선택](docs/technical-decisions.md) |
| 커피 메뉴 목록 조회 API | 구현 | `GET /api/menus` | [API](docs/api-overview.md#menu) |
| 포인트 충전 API | 구현 | `POST /api/points/charge` | [API](docs/api-overview.md#point) |
| 커피 주문 / 결제 API | 구현 | `POST /api/orders` | [API](docs/api-overview.md#order) |
| 주문 내역 실시간 전송 | 구현 | Spring Event `AFTER_COMMIT` -> Kafka | [문제 해결 전략](docs/problem-solving-strategy.md#주문-이벤트-처리-전략) |
| 최근 7일 인기 메뉴 Top 3 조회 | 구현 | V1 RDB 집계, V2 Kafka + Redis ZSET | [설계 의도](docs/design-rationale.md#인기-메뉴-설계) |
| 동시성 / 데이터 일관성 테스트 | 구현 | 통합 테스트, 동시성 테스트, 성능 테스트 | [테스트/검증](docs/testing-verification.md) |

## 문서

README는 제출용 요약만 담고, 상세 내용은 문서로 분리했습니다.

| 문서 | 설명 |
| --- | --- |
| [ERD / Table Overview](docs/erd/schema-overview.md) | ERD 링크, 테이블 설명, 주요 인덱스 |
| [Assignment Checklist](docs/assignment-checklist.md) | 과제 요구사항 충족 여부와 근거 |
| [API Overview](docs/api-overview.md) | Auth, Menu, Point, Order API 명세 |
| [Design Rationale](docs/design-rationale.md) | 메뉴, 포인트, 주문, 인기 메뉴 설계 의도 |
| [Problem Solving Strategy](docs/problem-solving-strategy.md) | 동시성, 일관성, 이벤트, 캐시, 다중 인스턴스 전략 |
| [Technical Decisions](docs/technical-decisions.md) | 기술 선택 이유와 한계 |
| [Testing / Verification](docs/testing-verification.md) | 수동 테스트, 통합 테스트, 동시성 테스트, 성능 테스트 요약 |
| [Trade-offs / Limitations](docs/tradeoffs-limitations.md) | 오버엔지니어링, Redis/Kafka 한계, Outbox 확장안 |
| [Popular Menu Performance](docs/performance/popular-menu-performance.md) | 인기 메뉴 V1/V2 성능 비교 |
| [Menu Cache Performance](docs/performance/menu-cache-performance.md) | 메뉴 캐시 성능 비교 |
| [Postman Collection](docs/postman/cafe-api-manual-test.postman_collection.json) | 수동 API 테스트 collection |

## 기술 스택

### Backend

- Java 21
- Spring Boot 4.0.6
- Spring WebMVC
- Spring Security
- Spring Data JPA
- Spring Data Redis
- Spring for Apache Kafka
- Validation
- Lombok
- Gradle

### Database / Cache / Messaging

- MySQL 8.4
- Redis
- Redisson
- Kafka
- Kafka UI

### Storage / Docs

- S3-compatible Object Storage 구조
- Postman Collection
- ERDCloud

## 시스템 아키텍처

```text
Client
  |
  | REST API
  v
Spring Boot Application
  |
  |-- MySQL
  |     - members
  |     - menus
  |     - point_wallets
  |     - point_histories
  |     - orders
  |     - order_items
  |
  |-- Redis
  |     - menu cache
  |     - popular menu ZSET
  |     - Redisson distributed lock
  |
  |-- Kafka
        - order paid event
        - order canceled event
```

기본 원칙은 `MySQL을 원본 저장소`로 두고, Redis와 Kafka는 조회 최적화와 이벤트 기반 read model 갱신에 사용하는 것입니다.

## 테스트

전체 테스트:

```bash
./gradlew.bat test
```

동시성 테스트:

```bash
./gradlew.bat test --tests "com.cafe.concurrency.*"
```

성능 테스트:

```bash
./gradlew.bat test --tests "com.cafe.performance.PopularMenuPerformanceTest" -Dperformance=true --rerun-tasks
./gradlew.bat test --tests "com.cafe.performance.MenuCachePerformanceTest" -Dperformance=true --rerun-tasks
```

자세한 테스트 결과와 해석은 [Testing / Verification](docs/testing-verification.md)을 참고합니다.

## 핵심 설계 요약

- 주문과 포인트의 최종 원본은 MySQL에 둡니다.
- 같은 사용자의 주문 생성/취소는 Redis 분산락으로 먼저 직렬화합니다.
- 포인트 지갑 row는 RDB 비관락으로 한 번 더 보호합니다.
- 주문 이벤트는 DB 커밋 이후 Kafka로 발행합니다.
- 인기 메뉴는 V1 RDB 집계와 V2 Redis ZSET read model을 함께 제공합니다.
- 메뉴 조회는 카테고리별 Redis Cache를 적용합니다.
- JPA 연관관계는 최소화하고 ID 참조 방식으로 도메인 경계를 느슨하게 유지합니다.

자세한 설계 이유는 [Design Rationale](docs/design-rationale.md)와 [Problem Solving Strategy](docs/problem-solving-strategy.md)를 참고합니다.

## AWS EC2, RDS, S3, MSK, ElastiCache 연결 사진
<img width="1538" height="218" alt="Image" src="https://github.com/user-attachments/assets/0fd255ec-8172-43e4-ac89-5abd09cee863" />

<img width="1666" height="221" alt="Image" src="https://github.com/user-attachments/assets/538d9372-aadd-4687-91f0-220aa5e5e04e" />

<img width="1925" height="468" alt="Image" src="https://github.com/user-attachments/assets/4879b1fc-bbca-4641-a085-837c2f0f9a1d" />

<img width="1700" height="981" alt="Image" src="https://github.com/user-attachments/assets/1c0fb934-c4f9-463c-b1a3-826011d19c4a" />

CICD + ALB 까지 하려고헀는데 시간부족으로 연결실패
