# Menu Cache Performance Test

## 목적

카테고리별 메뉴 조회에 Redis Cache를 적용했을 때 cache miss와 cache hit의 응답 시간 차이를 확인한다.

메뉴 정보는 주문보다 변경 빈도가 낮고, 사용자는 카테고리 단위로 반복 조회할 가능성이 높다.
따라서 카테고리별 메뉴 조회는 캐싱 효과를 기대할 수 있는 조회로 판단했다.

## 실행 환경

- Local Docker MySQL
- Local Docker Redis
- Spring Boot test context

## 테스트 데이터

- 카테고리: COFFEE
- 메뉴: 200개
- warm-up: 5회
- 측정 반복: 100회

## 실행 명령

```bash
./gradlew.bat test --tests "com.cafe.performance.MenuCachePerformanceTest" -Dperformance=true --rerun-tasks
```

일반 테스트에서는 성능 테스트가 실행되지 않는다.

## 측정 결과

| target | avg ms | min ms | max ms | p95 ms |
|---|---:|---:|---:|---:|
| Cache miss | 7.478 | 1.501 | 86.320 | 11.129 |
| Cache hit | 1.260 | 0.944 | 2.730 | 1.680 |

## 해석

Cache miss는 DB 조회, 응답 DTO 조립, Redis cache write 비용을 포함한다.
Cache hit는 Redis Cache에 저장된 카테고리 메뉴 응답을 읽는 비용이다.

이번 로컬 테스트에서는 cache hit가 cache miss보다 낮은 응답 시간을 보였다.
메뉴 조회는 변경보다 조회가 잦은 데이터이므로, 카테고리 기준 캐싱은 과제 규모에서도 설계 이유를 설명하기 쉽다.

## 한계

- 로컬 Docker 환경 기준 결과다.
- 실제 운영 환경에서는 DB/Redis 네트워크 지연과 데이터 크기에 따라 결과가 달라질 수 있다.
- 현재 메뉴 생성, 수정, 상태 변경, 삭제 시 `menus` 캐시를 전체 evict한다.
- 메뉴 수가 매우 커지거나 카테고리 수가 많아지면 캐시 key 정책과 evict 범위를 더 세밀하게 나눌 수 있다.
