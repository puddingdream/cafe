# Popular Menu Performance Test

## 목적

인기 메뉴 조회 방식을 비교한다.

- V1: RDB 기준 집계 조회
- V2: Redis ZSET 기준 조회

이 테스트는 "V2가 항상 더 빠르다"를 증명하기 위한 테스트가 아니다.
두 방식의 비용 구조가 어떻게 다른지 확인하기 위한 로컬 성능 비교다.

## 실행 환경

- Local Docker MySQL
- Local Docker Redis
- Local Docker Kafka
- Spring Boot test context

## 테스트 데이터

- 메뉴: 20개
- 주문: 5,000건
- 주문 아이템: 15,000건
- warm-up: 5회
- 측정 반복: 50회

## 실행 명령

```bash
./gradlew.bat test --tests "com.cafe.performance.PopularMenuPerformanceTest" -Dperformance=true --rerun-tasks
```

일반 테스트에서는 성능 테스트가 실행되지 않는다.

## 측정 결과

| target | avg ms | min ms | max ms | p95 ms |
|---|---:|---:|---:|---:|
| V1 RDB | 27.280 | 23.708 | 72.770 | 29.505 |
| V2 Redis ZSET | 11.258 | 9.531 | 22.086 | 14.015 |

## 해석

V1은 최근 7일 주문 아이템을 기준으로 RDB에서 조인, 그룹화, 합산, 정렬을 수행한다.
따라서 주문 아이템 row 수가 증가할수록 집계 비용이 커진다.

V2는 주문 시점에 Redis ZSET에 메뉴별 점수를 누적해 둔다.
조회 시에는 최근 7일치 ZSET을 읽어 메뉴별 점수를 합산하고, Top3 메뉴 정보를 DB에서 조회해 응답을 만든다.
따라서 조회 비용은 주문 row 수보다 메뉴 종류 수에 더 큰 영향을 받는다.

이번 로컬 테스트에서는 V2가 V1보다 낮은 응답 시간을 보였다.
다만 현재 V2는 `rolling:7d` 합산 ZSET을 따로 유지하지 않고, 조회 시점에 7일치 일별 ZSET을 합산한다.
따라서 메뉴 수가 매우 많아지면 V2 조회 비용도 증가할 수 있다.

## 한계

- 로컬 Docker 환경 기준 결과다.
- 실제 운영 DB/Redis/Kafka 네트워크 환경과 다를 수 있다.
- 테스트 데이터는 실제 사용자 트래픽 분포를 완전히 반영하지 않는다.
- V2는 최종 Top3 완성 응답을 별도 캐싱하지 않는다.
- 현재 V2는 최적화된 랭킹 구조가 아니라 이벤트 기반 read model 실험 구현에 가깝다.

## 확장안

조회 성능을 더 높이려면 다음 구조로 확장할 수 있다.

```text
popular:menus:rolling:7d
```

주문/취소 이벤트가 발생할 때 rolling ZSET도 함께 갱신하고,
스케줄러가 RDB 기준 집계 결과로 rolling ZSET을 주기적으로 재생성한다.

이렇게 하면 조회 시점에는 다음처럼 Top3만 바로 조회할 수 있다.

```text
reverseRangeWithScores("popular:menus:rolling:7d", 0, 2)
```

이 구조는 조회 비용을 줄이지만, rolling ZSET 보정 스케줄러와 정합성 관리가 추가로 필요하다.
