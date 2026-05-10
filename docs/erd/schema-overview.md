# ERD / Table Overview

## ERD

ERDCloud 임베드:

<iframe width="600" height="336" src="https://www.erdcloud.com/p/yh75isDY4bmFnwM5C" frameborder="0" allowfullscreen></iframe>

GitHub Markdown에서는 iframe이 표시되지 않을 수 있어 직접 링크도 함께 둔다.

- ERDCloud: <https://www.erdcloud.com/p/yh75isDY4bmFnwM5C>
- ERDCloud import DDL: [`cafe-erdcloud.sql`](cafe-erdcloud.sql)

ERD에는 논리적 관계를 표현하기 위해 FK를 표시했다. 실제 애플리케이션 코드는 다중 인스턴스와 도메인 분리 가능성을 고려해 JPA 연관관계 없이 `memberId`, `orderId`, `menuId` 같은 ID 참조 방식으로 구현했다.

## 주요 테이블

| 테이블 | 설명 |
| --- | --- |
| `members` | 회원 정보, 권한, 포인트 지갑 연결 정보 |
| `refresh_tokens` | refresh token 저장소 |
| `menus` | 메뉴 정보, 이미지 URL/key, 카테고리, 판매 상태 |
| `point_wallets` | 사용자별 포인트 잔액 |
| `point_histories` | 포인트 충전/사용/환불 이력 |
| `orders` | 주문 마스터, 주문번호, 주문 상태 |
| `order_items` | 주문 상세, 메뉴명/가격 스냅샷 |

## 테이블별 핵심 컬럼

### `members`

| 컬럼 | 설명 |
| --- | --- |
| `id` | 회원 PK |
| `email` | 로그인 이메일, unique |
| `password` | 암호화된 비밀번호 |
| `name` | 사용자 이름 |
| `phone_number` | 전화번호, unique |
| `role` | `USER`, `ADMIN` |
| `point_wallet_id` | 연결된 포인트 지갑 ID |
| `deleted_at` | soft delete 시각 |

### `menus`

| 컬럼 | 설명 |
| --- | --- |
| `id` | 메뉴 PK |
| `name` | 메뉴명 |
| `description` | 메뉴 설명 |
| `image_url` | 메뉴 이미지 공개 URL |
| `image_key` | Object Storage key |
| `price` | 가격 |
| `status` | `ACTIVE`, `INACTIVE` |
| `category` | `COFFEE`, `LATTE`, `TEA`, `ADE`, `SMOOTHIE`, `DECAFFEINATED`, `DESSERT` |
| `deleted_at` | soft delete 시각 |

### `point_wallets`

| 컬럼 | 설명 |
| --- | --- |
| `id` | 지갑 PK |
| `member_id` | 회원 ID, unique |
| `point` | 현재 잔액 |
| `version` | 낙관락 확장용 version |
| `deleted_at` | soft delete 시각 |

### `point_histories`

| 컬럼 | 설명 |
| --- | --- |
| `id` | 포인트 이력 PK |
| `member_id` | 회원 ID |
| `point_wallet_id` | 지갑 ID |
| `type` | `CHARGE`, `USE`, `REFUND` |
| `point` | 거래 포인트 |
| `after_point` | 거래 후 잔액 |

### `orders`

| 컬럼 | 설명 |
| --- | --- |
| `id` | 주문 PK |
| `order_number` | 사용자 노출용 주문번호, unique |
| `member_id` | 주문자 회원 ID |
| `total_amount` | 주문 총액 |
| `status` | `PAID`, `CANCELED` |
| `ordered_at` | 주문 시각 |
| `deleted_at` | soft delete 시각 |

### `order_items`

| 컬럼 | 설명 |
| --- | --- |
| `id` | 주문 상세 PK |
| `order_id` | 주문 ID |
| `menu_id` | 메뉴 ID |
| `menu_name` | 주문 시점 메뉴명 스냅샷 |
| `menu_price` | 주문 시점 메뉴 가격 스냅샷 |
| `quantity` | 수량 |
| `total_price` | 상세 총액 |

## 인덱스

| 테이블 | 인덱스 | 목적 |
| --- | --- | --- |
| `orders` | `idx_orders_member_ordered_at(member_id, ordered_at)` | 내 주문 목록 조회 |
| `orders` | `idx_orders_status_ordered_at(status, ordered_at)` | 최근 7일 인기 메뉴 집계 대상 주문 필터 |
| `order_items` | `idx_order_items_order_id(order_id)` | 주문 상세 조회 |
| `order_items` | `idx_order_items_menu_id(menu_id)` | 메뉴별 주문 집계 |
| `point_histories` | `idx_point_histories_member_id(member_id)` | 회원별 포인트 이력 조회 확장 |
| `point_histories` | `idx_point_histories_wallet_id(point_wallet_id)` | 지갑별 포인트 이력 조회 확장 |
