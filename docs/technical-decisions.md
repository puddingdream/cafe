# Technical Decisions

## Redis


| 사용처 | 목적 |
| --- | --- |
| Redis Cache | 카테고리별 메뉴 조회 캐싱 |
| Redis ZSET | 인기 메뉴 V2 read model |
| Redisson | 사용자 단위 분산락 |

메뉴 조회 캐시는 `menus` cache name으로 관리하고 TTL은 5분이다. 인기 메뉴 V1 캐시는 `popularMenus` cache name으로 관리하고 TTL은 30초다. 인기 메뉴 V2는 Redis ZSET에 일별 점수를 저장하고 최근 7일 데이터를 합산한다.

선택 이유:

- 다중 인스턴스 환경에서 로컬 메모리 캐시보다 공유 캐시가 적합함
- 메뉴 조회처럼 변경보다 조회가 많은 데이터에 캐시 효과를 기대할 수 있음
- ZSET은 점수 증가와 랭킹 조회에 적합함
- 분산락을 통해 여러 애플리케이션 인스턴스가 같은 사용자 주문을 동시에 처리하는 상황을 제어할 수 있음

한계:

- Redis는 원본 저장소가 아니므로 장애나 누락 시 RDB 기준 보정이 필요함
- 현재 V2 인기 메뉴는 최근 7일 일별 ZSET을 조회 시점에 합산하므로, 메뉴 종류가 매우 많아지면 비용이 증가할 수 있음

## Redisson

분산락은 Spring Data Redis로 직접 `SET NX`를 구현하지 않고 Redisson을 사용했다.

```text
@RedisLock(key = "'lock:order:member:' + #loginUser.id()", waitTime = 3, leaseTime = 5)
```

## Kafka

Kafka는 주문 내역을 데이터 수집 플랫폼으로 실시간 전송해야 한다는 요구사항과 인기 메뉴 V2 read model 갱신을 위해 사용했다.

| Topic | 설명 |
| --- | --- |
| `cafe.order-paid.v1` | 주문 완료 이벤트 |
| `cafe.order-canceled.v1` | 주문 취소 이벤트 |
| `cafe.order-paid.dlt.v1` | 주문 완료 이벤트 DLT 확장용 |
| `cafe.order-canceled.dlt.v1` | 주문 취소 이벤트 DLT 확장용 |

현재 로컬 Docker 환경에서는 Kafka broker를 1개만 띄우므로 replication factor는 1이다. partition은 3개로 생성해 이벤트 처리 확장 가능성을 남겼다.


선택 이유:

- 주문 완료/취소 이벤트를 주문 트랜잭션과 분리해 비동기 처리 가능
- 인기 메뉴 랭킹처럼 주문 이후 후속 처리가 필요한 기능을 이벤트 기반으로 확장 가능
- 향후 알림, 데이터 분석, 외부 수집 플랫폼 전송 같은 consumer를 추가하기 쉬움

한계:

- 현재 구현은 Outbox Pattern까지는 적용하지 않았음
- DB 커밋 후 Kafka 발행 실패 가능성은 남아 있음
- 운영 수준 보장이 필요하면 `order_outbox` 테이블과 재시도 publisher를 추가하는 편이 적합함

## Spring Event `AFTER_COMMIT`

OrderService가 KafkaTemplate을 직접 호출하지 않고 Spring Event를 먼저 발행하도록 구성했다. Kafka 전달은 `@TransactionalEventListener(phase = AFTER_COMMIT)`에서 처리한다.

선택 이유:

- 주문 서비스가 Kafka 구현체에 직접 의존하지 않음
- DB 트랜잭션이 롤백되면 Kafka 이벤트가 발행되지 않음
- 주문 생성 로직과 이벤트 전달 로직을 분리할 수 있음

```text
OrderService
  -> 주문 저장
  -> 포인트 차감
  -> Spring Event 발행
  -> DB commit 성공
  -> AFTER_COMMIT listener
  -> Kafka publish
```

이 방식은 "DB 커밋 전 Kafka 발행" 문제를 피한다. 다만 "DB 커밋 후 Kafka 발행 실패"까지 완전히 해결하지는 않으므로, 더 강한 보장이 필요하면 Outbox Pattern으로 확장한다.

## JWT / Spring Security

인증/인가는 JWT와 Spring Security 기반으로 구현했다.

사용 방식:

- 로그인 성공 시 access token과 refresh token 발급
- access token에는 `memberId`, `role`, `issuer`, `issuedAt`, `expiration`, `jti` 포함
- refresh token은 DB에 저장해 재발급과 로그아웃에 사용
- 서버는 세션을 사용하지 않고 `SessionCreationPolicy.STATELESS`로 동작
- Controller에서는 `@LoginUser LoginUserInfoDto`로 인증 사용자 정보를 받음

선택 이유:

- API 서버를 stateless하게 유지할 수 있음
- 다중 인스턴스 환경에서 서버 세션 공유 문제가 없음
- 관리자 메뉴 API와 일반 사용자 API의 권한 분리를 구현할 수 있음

현재 refresh token은 RDB에 저장한다. 이후 Redis를 활용하면 refresh token TTL 관리나 token blacklist를 더 자연스럽게 확장할 수 있다.

## S3-compatible Object Storage

메뉴 이미지는 DB에 직접 저장하지 않고 Object Storage에 저장하는 구조로 만들었다.

| 구성요소 | 역할 |
| --- | --- |
| `ObjectStorageClient` | 업로드/삭제 인터페이스 |
| `S3ObjectStorageClient` | AWS S3 또는 S3-compatible storage 실제 구현 |
| `DisabledObjectStorageClient` | storage 미설정 시 명확히 실패시키는 fallback |
| `MediaStorageProperties` | bucket, region, endpoint, 용량, 확장자 설정 |

선택 이유:

- 이미지 바이너리를 DB에 저장하지 않아 DB 크기와 백업 비용을 줄일 수 있음
- AWS S3뿐 아니라 endpoint 설정을 통해 S3-compatible storage로 확장 가능
- storage가 꺼져 있을 때 앱 부팅은 가능하지만, 업로드 시 명확한 예외를 던져 설정 누락을 빠르게 알 수 있음

현재 메뉴 이미지는 최대 10MB, 확장자는 `jpg`, `jpeg`, `png`, `webp`를 허용한다.

## Spring AI / OpenAI

AI 메뉴 추천은 과제 필수 요구사항이 아닌 부가 기능으로 구현했다. 현재는 RAG나 fine-tuning을 적용하지 않고, 서버가 선별한 실제 판매중 메뉴 후보를 프롬프트에 넣는 방식이다.

흐름:

```text
사용자 취향 입력
  -> Redis 인기 메뉴 + 최신 판매중 메뉴 후보 선정
  -> 온도/예산 조건으로 후보 필터링
  -> OpenAI 호출
  -> AI가 반환한 menuId를 서버에서 검증
  -> DB 메뉴 정보와 AI 추천 사유를 조합해 응답
```

선택 이유:

- 메뉴 수가 많지 않아 벡터DB 기반 RAG보다 단순한 후보 프롬프트 방식이 적합함
- AI가 없는 메뉴명이나 가격을 만들지 못하도록 최종 메뉴 정보는 DB 값을 사용함
- OpenAI 설정이 없으면 `SPRING_AI_MODEL_CHAT=none`으로 앱 부팅이 가능하게 함

한계:

- 외부 AI 호출이므로 일반 API보다 응답 시간이 길 수 있음
- 메뉴/영양/알레르기 문서가 많아지면 RAG로 확장하는 편이 적합함

## Docker Compose

로컬 인프라는 Docker Compose로 구성했다.

| Service | Port | 설명 |
| --- | --- | --- |
| MySQL | `33306:3306` | 원본 DB |
| Redis | `6379:6379` | 캐시, ZSET, 분산락 |
| Kafka | `9094` | 로컬 애플리케이션 접속용 listener |
| Kafka UI | `8989` | topic/message 확인 |

선택 이유:

- 과제 검증에 필요한 MySQL, Redis, Kafka를 로컬에서 재현 가능
- AWS RDS, ElastiCache, MSK 없이도 동시성/이벤트/캐시 구조를 테스트 가능
- 실행 환경을 문서화하기 쉬움

운영 환경으로 확장한다면 MySQL은 RDS, Redis는 ElastiCache, Kafka는 MSK 또는 별도 Kafka cluster로 대체할 수 있다.
