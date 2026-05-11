# API Overview

공통 응답은 `ApiResponse<T>`로 감싼다.

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

인증이 필요한 API는 기본적으로 아래 헤더를 사용한다.

```text
Authorization: Bearer {accessToken}
```

## Auth

| Method | Endpoint | 인증 | 설명 |
| --- | --- | --- | --- |
| `POST` | `/api/auth/signup` | Public | 회원가입 |
| `POST` | `/api/auth/login` | Public | 로그인 / access token, refresh token 발급 |
| `POST` | `/api/auth/reissue` | Public | refresh token 기반 토큰 재발급 |
| `POST` | `/api/auth/logout` | Required | refresh token 로그아웃 |

### `POST /api/auth/signup`

Request:

| Field | Type | Required | 설명 |
| --- | --- | --- | --- |
| `email` | `string` | O | 이메일 |
| `password` | `string` | O | 8자 이상, 영문 + 숫자 포함 |
| `name` | `string` | O | 이름 |
| `phoneNumber` | `string` | O | 전화번호 |

Response data:

| Field | Type | 설명 |
| --- | --- | --- |
| `memberId` | `number` | 생성된 회원 ID |
| `email` | `string` | 이메일 |
| `message` | `string` | 가입 결과 메시지 |

### `POST /api/auth/login`

Request:

| Field | Type | Required | 설명 |
| --- | --- | --- | --- |
| `email` | `string` | O | 이메일 |
| `password` | `string` | O | 비밀번호 |

Response data:

| Field | Type | 설명 |
| --- | --- | --- |
| `tokens.accessToken` | `string` | access token |
| `tokens.refreshToken` | `string` | refresh token |
| `member.id` | `number` | 회원 ID |
| `member.email` | `string` | 이메일 |
| `member.name` | `string` | 이름 |

### `POST /api/auth/reissue`

Request:

| Field | Type | Required | 설명 |
| --- | --- | --- | --- |
| `refreshToken` | `string` | O | refresh token |

Response data:

| Field | Type | 설명 |
| --- | --- | --- |
| `accessToken` | `string` | 새 access token |
| `refreshToken` | `string` | 새 refresh token |

## Menu

| Method | Endpoint | 인증 | 설명 |
| --- | --- | --- | --- |
| `GET` | `/api/menus` | Public | 메뉴 목록 조회 |
| `GET` | `/api/menus/popular` | Public | 최근 7일 인기 메뉴 Top 3 V1, RDB 집계 |
| `GET` | `/api/menus/popular/v2` | Public | 최근 7일 인기 메뉴 Top 3 V2, Redis ZSET |
| `POST` | `/api/admin/menus` | Admin | 메뉴 생성 |
| `PUT` | `/api/admin/menus/{menuId}` | Admin | 메뉴 정보/이미지 수정 |
| `PATCH` | `/api/admin/menus/{menuId}/status/toggle` | Admin | 메뉴 판매 상태 토글 |
| `DELETE` | `/api/admin/menus/{menuId}` | Admin | 메뉴 soft delete |

### `GET /api/menus`

Query:

| Name | Type | Required | 설명 |
| --- | --- | --- | --- |
| `category` | `string` | X | `COFFEE`, `LATTE`, `TEA`, `ADE`, `SMOOTHIE`, `DECAFFEINATED`, `DESSERT` 또는 한글 라벨 |

Response data:

| Field | Type | 설명 |
| --- | --- | --- |
| `id` | `number` | 메뉴 ID |
| `name` | `string` | 메뉴명 |
| `description` | `string` | 메뉴 설명 |
| `imageUrl` | `string` | 메뉴 이미지 URL |
| `price` | `number` | 가격 |
| `category` | `string` | 카테고리 enum |
| `categoryLabel` | `string` | 카테고리 한글명 |
| `status` | `string` | `ACTIVE`, `INACTIVE` |

### `POST /api/admin/menus`

Content-Type:

```text
multipart/form-data
```

Request:

| Field | Type | Required | 설명 |
| --- | --- | --- | --- |
| `name` | `string` | O | 메뉴명 |
| `description` | `string` | O | 메뉴 설명 |
| `price` | `number` | O | 가격, 0보다 커야 함 |
| `category` | `string` | O | 메뉴 카테고리 |
| `imageFile` | `file` | O | 메뉴 이미지 |

Response data는 메뉴 조회 응답과 동일한 형태다.

### `GET /api/menus/popular`

최근 7일간 `PAID` 주문의 `order_items.quantity`를 메뉴별로 합산해 Top 3를 조회한다.

Response data:

| Field | Type | 설명 |
| --- | --- | --- |
| `id` | `number` | 메뉴 ID |
| `name` | `string` | 메뉴명 |
| `description` | `string` | 메뉴 설명 |
| `imageUrl` | `string` | 메뉴 이미지 URL |
| `price` | `number` | 가격 |
| `category` | `string` | 카테고리 enum |
| `categoryLabel` | `string` | 카테고리 한글명 |
| `status` | `string` | 판매 상태 |
| `orderCount` | `number` | 최근 7일 주문 수량 합계 |

### `GET /api/menus/popular/v2`

Kafka 주문 이벤트로 Redis ZSET에 누적된 일별 인기 메뉴 점수를 기준으로 최근 7일 Top 3를 조회한다.

응답 형식은 `GET /api/menus/popular`와 같다.

## Point

| Method | Endpoint | 인증 | 설명 |
| --- | --- | --- | --- |
| `POST` | `/api/points/charge` | Required | 포인트 충전 |

### `POST /api/points/charge`

Request:

| Field | Type | Required | 설명 |
| --- | --- | --- | --- |
| `chargePoint` | `number` | O | 충전 포인트, 0보다 커야 함 |

Response data:

| Field | Type | 설명 |
| --- | --- | --- |
| `memberId` | `number` | 회원 ID |
| `chargePoint` | `number` | 충전 포인트 |
| `afterPoint` | `number` | 충전 후 잔액 |

현재 과제에서는 포인트 충전 API를 직접 제공한다. 실제 결제 서비스에서는 사용자가 임의로 포인트를 충전하는 API를 직접 호출하지 않고, PortOne 같은 PG 결제 검증 이후 서버 내부에서 포인트 충전을 호출하는 구조가 더 적합하다.

## AI

AI 추천은 과제 필수 요구사항이 아닌 부가 기능이다. 실제 판매중 메뉴 후보만 OpenAI에 전달하고, AI가 반환한 `menuId`를 서버에서 다시 검증한다.

| Method | Endpoint | 인증 | 설명 |
| --- | --- | --- | --- |
| `POST` | `/api/ai/coffee-recommendations` | Required | 취향 기반 메뉴 추천 |

Request:

| Field | Type | Required | 설명 |
| --- | --- | --- | --- |
| `preference` | `string` | O | 원하는 맛, 상황, 기분 |
| `temperaturePreference` | `string` | X | 예: `ICE`, `HOT`, `상관없음` |
| `caffeinePreference` | `string` | X | 예: `디카페인 선호`, `상관없음` |
| `maxPrice` | `number` | X | 최대 예산 |

Response data:

| Field | Type | 설명 |
| --- | --- | --- |
| `answerType` | `string` | `RECOMMENDATION`, `OUT_OF_SCOPE` |
| `message` | `string` | 추천 요약 또는 범위 밖 요청 안내 |
| `recommendations` | `array` | 서버 검증을 통과한 추천 메뉴 |
| `menuCount` | `number` | AI 후보로 전달한 메뉴 수 |
| `recommendedAt` | `datetime` | 추천 생성 시각 |

## Order

| Method | Endpoint | 인증 | 설명 |
| --- | --- | --- | --- |
| `POST` | `/api/orders` | Required | 주문 생성 / 포인트 결제 |
| `GET` | `/api/orders/{orderNumber}` | Required | 주문번호 기준 단건 조회 |
| `GET` | `/api/orders` | Required | 내 주문 목록 조회 |
| `PATCH` | `/api/orders/{orderNumber}/cancel` | Required | 주문 취소 / 포인트 환불 |

### `POST /api/orders`

Request:

```json
{
  "items": [
    {
      "menuId": 1,
      "quantity": 2
    }
  ]
}
```

Request fields:

| Field | Type | Required | 설명 |
| --- | --- | --- | --- |
| `items` | `array` | O | 주문 항목 |
| `items[].menuId` | `number` | O | 메뉴 ID |
| `items[].quantity` | `number` | O | 수량, 0보다 커야 함 |

Response data:

| Field | Type | 설명 |
| --- | --- | --- |
| `orderId` | `number` | 주문 ID |
| `orderNumber` | `string` | 주문번호, `ORD-yyyyMMddHHmmss-random6` |
| `memberId` | `number` | 회원 ID |
| `totalAmount` | `number` | 주문 총액 |
| `usedPoint` | `number` | 사용 포인트 |
| `afterPoint` | `number` | 결제 후 포인트 |
| `status` | `string` | `PAID`, `CANCELED` |
| `orderedAt` | `datetime` | 주문 시각 |
| `items` | `array` | 주문 상세 |

Order item response:

| Field | Type | 설명 |
| --- | --- | --- |
| `menuId` | `number` | 메뉴 ID |
| `menuName` | `string` | 주문 시점 메뉴명 스냅샷 |
| `menuPrice` | `number` | 주문 시점 메뉴 가격 스냅샷 |
| `quantity` | `number` | 수량 |
| `totalPrice` | `number` | 항목 총액 |

### `GET /api/orders`

Query:

| Name | Type | Required | Default | 설명 |
| --- | --- | --- | --- | --- |
| `page` | `number` | X | `0` | 페이지 번호 |
| `size` | `number` | X | `20` | 페이지 크기, 최대 `50` |

Response data:

| Field | Type | 설명 |
| --- | --- | --- |
| `orders` | `array` | 주문 목록 |
| `page` | `number` | 현재 페이지 |
| `size` | `number` | 페이지 크기 |
| `hasNext` | `boolean` | 다음 페이지 존재 여부 |

### `GET /api/orders/{orderNumber}`

주문번호 기준으로 내 주문 단건을 조회한다.

Response data는 주문 생성 응답에서 `usedPoint`, `afterPoint`를 제외한 주문 상세 형태다.

### `PATCH /api/orders/{orderNumber}/cancel`

주문을 취소하고 결제에 사용한 포인트를 환불한다.

Response data:

| Field | Type | 설명 |
| --- | --- | --- |
| `orderId` | `number` | 주문 ID |
| `orderNumber` | `string` | 주문번호 |
| `memberId` | `number` | 회원 ID |
| `refundPoint` | `number` | 환불 포인트 |
| `afterPoint` | `number` | 환불 후 포인트 |
| `status` | `string` | 취소 후 주문 상태 |
| `orderedAt` | `datetime` | 주문 시각 |
| `items` | `array` | 주문 상세 |
