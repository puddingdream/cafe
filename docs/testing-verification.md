# Testing / Verification

## 수동 테스트

수동 API 테스트는 Postman Collection으로 정리했다.

- Collection: [`postman/cafe-api-manual-test.postman_collection.json`](postman/cafe-api-manual-test.postman_collection.json)

확인한 주요 흐름:

```text
Login User
Login Admin
Get COFFEE Menus
Get LATTE Menus
Charge Point
Create Order
Popular Menus V1
Popular Menus V2
Cancel Order
Popular Menus V2 재확인
```

로그인 요청 이후 access token을 collection variable에 저장해 보호 API에서 자동으로 사용하도록 구성했다.

## 자동 테스트

현재 테스트 범위:

| 테스트 | 목적 |
| --- | --- |
| `PointServiceIntegrationTest` | 포인트 충전/사용/환불, 잔액 부족 검증 |
| `OrderServiceIntegrationTest` | 주문 생성, 포인트 차감, 주문 취소 환불, 판매중지 메뉴 주문 실패 검증 |
| `OrderStatisticsReaderIntegrationTest` | V1 RDB 인기 메뉴 집계가 `PAID` 주문만 합산하는지 검증 |
| `PopularMenuRankingServiceIntegrationTest` | V2 Redis ZSET 점수 증가/감소 검증 |
| `MenuServiceCacheIntegrationTest` | 메뉴 조회 캐시 저장/무효화 검증 |
| `PointConcurrencyIntegrationTest` | 같은 지갑 동시 충전/차감 정합성 검증 |
| `OrderConcurrencyIntegrationTest` | 같은 사용자 동시 주문 초과 결제 방지, 서로 다른 사용자 동시 주문 성공 검증 |

전체 테스트 실행:

```bash
./gradlew.bat test
```

동시성 테스트만 실행:

```bash
./gradlew.bat test --tests "com.cafe.concurrency.*"
```

## 동시성 테스트 요약

### 포인트 동시성

동시 차감:

```text
초기 포인트: 20,000
동시 요청: 20개
각 요청: usePoint 1,000
기대 결과: 모든 요청 성공, 최종 잔액 0
```

동시 충전:

```text
초기 포인트: 0
동시 요청: 10개
각 요청: charge 1,000
기대 결과: 모든 요청 성공, 최종 잔액 10,000
```

검증 의미:

- `PointWalletRepository.findWithLockByMemberId()`의 비관락이 같은 지갑 row 변경을 직렬화한다.
- 동시 차감 상황에서도 lost update 없이 최종 잔액이 정확하다.
- 동시 충전 상황에서도 각 충전 요청이 누락되지 않고 누적된다.

### 주문 동시성

같은 사용자 동시 주문:

```text
초기 포인트: 10,000
메뉴 가격: 10,000
동시 주문 요청: 8개
각 요청: 같은 사용자, 같은 메뉴 1개 주문
기대 결과: 성공 주문 1개, 최종 잔액 0, PAID 주문 1개
```

서로 다른 사용자 동시 주문:

```text
사용자 수: 8명
각 사용자 초기 포인트: 10,000
메뉴 가격: 10,000
동시 주문 요청: 8개
각 요청: 서로 다른 사용자, 같은 메뉴 1개 주문
기대 결과: 모든 주문 성공, 각 사용자 최종 잔액 0, 각 사용자 PAID 주문 1개
```

검증 의미:

- `@RedisLock(key = 'lock:order:member:' + memberId)`가 같은 사용자의 동시 주문을 사용자 단위로 제어한다.
- 포인트 차감은 지갑 비관락으로 한 번 더 보호된다.
- 서로 다른 사용자의 주문은 같은 사용자 락에 묶이지 않아 병렬 성공이 가능하다.

## 성능 테스트

성능 테스트는 기본 `test` 실행에서 제외하고, `-Dperformance=true`가 있을 때만 실행한다.

### 인기 메뉴 V1/V2 비교

상세 문서:

- [`performance/popular-menu-performance.md`](performance/popular-menu-performance.md)

실행:

```bash
./gradlew.bat test --tests "com.cafe.performance.PopularMenuPerformanceTest" -Dperformance=true --rerun-tasks
```

대표 결과:

| target | avg ms | p95 ms |
| --- | ---:| ---:|
| V1 RDB | 27.280 | 29.505 |
| V2 Redis ZSET | 11.258 | 14.015 |

해석:

- V1은 주문 아이템 row 수에 비례해 집계 비용이 증가한다.
- V2는 주문 row 수보다 메뉴 종류 수에 더 큰 영향을 받는다.
- 현재 V2는 `rolling:7d` 합산 ZSET이 없으므로 최적화된 랭킹 구조는 아니다.

### 메뉴 캐시 비교

상세 문서:

- [`performance/menu-cache-performance.md`](performance/menu-cache-performance.md)

실행:

```bash
./gradlew.bat test --tests "com.cafe.performance.MenuCachePerformanceTest" -Dperformance=true --rerun-tasks
```

대표 결과:

| target | avg ms | p95 ms |
| --- | ---:| ---:|
| Cache miss | 7.478 | 11.129 |
| Cache hit | 1.260 | 1.680 |

해석:

- 메뉴 조회는 변경보다 조회가 잦은 데이터이므로 카테고리 기준 캐싱 효과를 기대할 수 있다.
- 메뉴 생성/수정/상태 변경/삭제 시 `menus` 캐시를 전체 evict한다.
