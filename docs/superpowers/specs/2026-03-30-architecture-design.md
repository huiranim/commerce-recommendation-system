# Architecture Design: 실시간 커머스 추천 및 랭킹 시스템

**Date:** 2026-03-30
**Author:** 1인 프로젝트
**Status:** Approved

---

## 1. 프로젝트 개요

### 목적

사용자 행동 이벤트(VIEW, CLICK, PURCHASE)를 실시간으로 수집하고,
이를 비동기로 처리하여 인기 상품 랭킹과 개인화 추천을 제공하는 B2C 백엔드 서비스.

### 핵심 역량 시연 목표

- 이벤트 기반 비동기 아키텍처 (Kafka)
- 실시간 데이터 처리 (Redis Sorted Set)
- 확장성을 고려한 API 설계
- 프로덕션 수준의 설계 결정 (Idempotency, DLQ, N+1 방지, 에러 포맷 통일)

### 제약 조건

- 1인 프로젝트, 약 4주
- AWS 배포 (EC2 + RDS MySQL + ElastiCache Redis)
- Docker 기반 실행 환경 (Docker Compose)
- Kubernetes 제외, 오버엔지니어링 금지

---

## 2. 시스템 아키텍처

### 전체 구성도

```
┌──────────────────────────────────────────────────────────────┐
│                        Client (HTTP)                          │
└────────────────────────────┬─────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────┐
│                        API Server                             │
│  - 이벤트 수신 → Kafka produce                                │
│  - 랭킹/추천 조회 → Redis read → MySQL IN 조회                │
│  - 상품/유저 CRUD → MySQL read/write                          │
└───────────┬─────────────────────────────┬────────────────────┘
            │ produce                     │ read
            ▼                             ▼
┌─────────────────────┐       ┌──────────────────────┐
│       Kafka         │       │        Redis          │
│  user-events        │       │  ranking (SortedSet)  │
│  user-events-dlq    │       │  recommendations      │
└──────────┬──────────┘       │  idempotency keys     │
           │ consume           └──────────────────────┘
           ▼                              ▲
┌──────────────────────────────────────────────────────────────┐
│                     Event Processor                           │
│  - Kafka consume (at-least-once)                              │
│  - eventId 기반 idempotent 처리                               │
│  - 랭킹 점수 계산 → Redis ZINCRBY                             │
│  - 추천 데이터 생성 → Redis ZADD                              │
│  - 처리 실패 → DLQ produce                                    │
└──────────────────────────────────────────────────────────────┘
            │ read (마스터 데이터, read-only)
            ▼
┌──────────────────────┐
│        MySQL         │
│  - users             │
│  - products          │
│  - categories        │
└──────────────────────┘
```

### Maven Multi-Module 프로젝트 구조

```
commerce-recommendation-system/
├── pom.xml                              ← parent pom (공통 의존성 관리)
├── common/                              ← 공유 모듈 (JAR)
│   └── src/main/java/.../common/
│       ├── event/                       ← Kafka 이벤트 스키마
│       └── dto/                         ← 공통 응답 DTO, 에러 포맷
├── api-server/                          ← Spring Boot 앱 (Producer)
│   └── src/main/java/.../api/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       └── kafka/producer/
├── event-processor/                     ← Spring Boot 앱 (Consumer)
│   └── src/main/java/.../processor/
│       ├── kafka/consumer/
│       ├── ranking/
│       ├── recommendation/
│       └── idempotency/
└── docker-compose.yml
```

### 런타임 컨테이너 (Docker Compose)

| 컨테이너 | 이미지 | 포트 | 용도 |
|---|---|---|---|
| api-server | 직접 빌드 | 8080 | HTTP API |
| event-processor | 직접 빌드 | 8081 | Kafka Consumer |
| kafka | confluentinc/cp-kafka | 9092 | 이벤트 브로커 (KRaft 모드) |
| redis | redis:7-alpine | 6379 | 랭킹/추천/Idempotency |
| mysql | mysql:8 | 3306 | 마스터 데이터 |

---

## 3. 이벤트 스키마 (common 모듈)

```java
// common/src/main/java/.../common/event/UserBehaviorEvent.java
public record UserBehaviorEvent(
    String eventId,          // UUID — Idempotency 키
    String schemaVersion,    // "1.0" — 스키마 진화 대비
    String userId,
    String productId,
    EventType eventType,     // VIEW, CLICK, PURCHASE
    String sessionId,
    EventContext context,
    Instant timestamp
) {}

public enum EventType {
    VIEW, CLICK, PURCHASE;

    public int weight() {
        return switch (this) {
            case VIEW     -> 1;
            case CLICK    -> 3;
            case PURCHASE -> 10;
        };
    }
}

public record EventContext(
    String page,      // "home", "search", "category", "product_detail"
    Integer position  // 노출 순위 (nullable)
) {}
```

---

## 4. API 설계

### 4-1. 이벤트 수집

```
POST /api/v1/events
```

**Request Body:**
```json
{
  "userId": "uuid",
  "productId": "uuid",
  "eventType": "PURCHASE",
  "sessionId": "session-uuid",
  "context": {
    "page": "product_detail",
    "position": null
  }
}
```

**Response: 202 Accepted**
```json
{
  "eventId": "generated-uuid",
  "status": "ACCEPTED"
}
```

> eventId를 반환하여 비동기 처리 추적 가능. 202는 "처리 예정"을 의미하므로 200보다 의미적으로 정확함.

---

### 4-2. 랭킹 조회

```
GET /api/v1/rankings/trending?window=1h&limit=20
GET /api/v1/rankings/trending/categories/{categoryId}?window=1h&limit=20
```

**Query Params:**
- `window`: `1h` | `24h` (default: `1h`)
- `limit`: 1~100 (default: 20)

**Response: 200 OK**
```json
{
  "window": "1h",
  "generatedAt": "2026-03-30T12:00:00Z",
  "products": [
    {
      "rank": 1,
      "productId": "uuid",
      "name": "상품명",
      "categoryId": "electronics",
      "price": 99000,
      "score": 142.0
    }
  ]
}
```

**조회 흐름 (N+1 방지):**
```
Redis ZREVRANGE ranking:trending:1h 0 19
    → productId 목록 추출
    → MySQL: SELECT * FROM products WHERE id IN (?, ?, ...) AND status = 'ACTIVE'
    → productId 순서 보존하여 응답 조립
```

---

### 4-3. 추천 조회

```
GET /api/v1/recommendations/users/{userId}?limit=10
GET /api/v1/recommendations/categories/{categoryId}?limit=10
```

**Response: 200 OK**
```json
{
  "userId": "uuid",
  "products": [
    {
      "productId": "uuid",
      "name": "상품명",
      "categoryId": "sports",
      "price": 49000,
      "reason": "RECENT_VIEW_CATEGORY"
    }
  ]
}
```

**reason 값 정의:**

| reason | 의미 |
|---|---|
| `RECENT_VIEW_CATEGORY` | 최근 본 상품의 카테고리 내 인기 상품 |
| `RECENT_PURCHASE_CATEGORY` | 최근 구매한 카테고리 내 인기 상품 |
| `CATEGORY_POPULAR` | 카테고리 전체 인기 상품 (개인화 데이터 없을 때 fallback) |

---

### 4-4. 마스터 데이터 (시드/어드민용)

```
POST /api/v1/products    Body: { name, categoryId, price }
GET  /api/v1/products/{productId}
POST /api/v1/users       Body: { name, email }
GET  /api/v1/users/{userId}
```

---

### 4-5. 통일된 에러 응답 포맷

모든 에러는 아래 포맷으로 반환:

```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "Product not found: uuid",
  "timestamp": "2026-03-30T12:00:00Z",
  "path": "/api/v1/products/uuid"
}
```

**HTTP 상태 코드 규칙:**

| 상황 | 상태 코드 | code 예시 |
|---|---|---|
| 요청 필드 누락/형식 오류 | 400 | `INVALID_REQUEST` |
| 리소스 없음 | 404 | `PRODUCT_NOT_FOUND`, `USER_NOT_FOUND` |
| 서버 내부 오류 | 500 | `INTERNAL_SERVER_ERROR` |
| Kafka produce 실패 | 503 | `EVENT_PUBLISH_FAILED` |

---

## 5. MySQL 데이터 모델

```sql
CREATE TABLE categories (
    id          VARCHAR(50)  PRIMARY KEY,   -- "electronics", "sports"
    name        VARCHAR(100) NOT NULL,
    parent_id   VARCHAR(50)  NULL,          -- 1단계 depth만 사용 (MVP)
    FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE TABLE users (
    id          VARCHAR(36)  PRIMARY KEY,   -- UUID
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id          VARCHAR(36)   PRIMARY KEY,  -- UUID
    name        VARCHAR(255)  NOT NULL,
    category_id VARCHAR(50)   NOT NULL,
    price       DECIMAL(10,2) NOT NULL,
    status      ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    INDEX idx_category_status (category_id, status)  -- Event Processor + 랭킹 조회 최적화
);
```

> **Event Processor의 MySQL 접근 원칙:** `products` 테이블을 읽기 전용으로만 사용.
> 쓰기는 API Server에서만 수행. 코드 레벨에서 `@Transactional(readOnly = true)`로 강제.

---

## 6. Redis 데이터 구조

### 6-1. 랭킹 (Sorted Set)

| Key 패턴 | 자료구조 | TTL | 비고 |
|---|---|---|---|
| `ranking:trending:1h` | Sorted Set | 없음 | 전체 1시간 인기 |
| `ranking:trending:24h` | Sorted Set | 없음 | 전체 24시간 인기 |
| `ranking:trending:category:{categoryId}:1h` | Sorted Set | 없음 | 카테고리별 인기 |

**점수 계산 (MVP):**
```
ZINCRBY ranking:trending:1h {weight} {productId}
# weight: VIEW=1, CLICK=3, PURCHASE=10
```

**Time Decay 확장 포인트:**
> MVP에서는 단순 누산 방식 사용. 확장 시 스케줄러로 주기적 key 재생성하거나
> score에 `weight × e^(-λ × age_in_hours)` (λ=0.1) 적용하여 시간 감쇄 구현 가능.

### 6-2. 추천 (Sorted Set)

| Key 패턴 | 자료구조 | TTL | Score 기준 |
|---|---|---|---|
| `recommendation:user:{userId}` | Sorted Set | 1시간 | relevance score |
| `recommendation:category:{categoryId}` | Sorted Set | 6시간 | trending score |

```
ZADD recommendation:user:{userId} {relevance_score} {productId}
EXPIRE recommendation:user:{userId} 3600
ZREVRANGE recommendation:user:{userId} 0 9  -- top 10 조회
```

> List 대신 Sorted Set을 선택한 이유: 동일 상품 중복 방지, ZADD가 기존 점수를 자동 갱신하므로
> 재계산 시 별도 삭제 없이 덮어쓰기 가능.

### 6-3. Idempotency Keys

| Key 패턴 | 자료구조 | TTL |
|---|---|---|
| `processed:event:{eventId}` | String | 24시간 |

---

## 7. 이벤트 처리 흐름 (Event Processor)

### 7-1. Kafka 이벤트 처리 (At-Least-Once + Idempotency)

```
이벤트 수신 (at-least-once 보장)
    │
    ▼
Redis SETNX processed:event:{eventId} "1" EX 86400
    │
    ├─ 반환 1 (신규) ─────────────────────────────────────┐
    │                                                      │
    └─ 반환 0 (중복) → skip + DEBUG 로그                  │
                                                           ▼
                                            이벤트 타입별 처리
                                                │
                                    ┌───────────┼───────────┐
                                    ▼           ▼           ▼
                                  VIEW       CLICK      PURCHASE
                                    │           │           │
                              weight=1    weight=3    weight=10
                                    └───────────┴───────────┘
                                                │
                                    ZINCRBY ranking:trending:1h
                                    ZINCRBY ranking:trending:24h
                                    ZINCRBY ranking:trending:category:{id}:1h
                                                │
                                            PURCHASE만
                                                │
                                    product 카테고리 조회
                                    (Caffeine local cache, TTL 30분)
                                                │
                                    ┌───────────┴───────────┐
                                    ▼                       ▼
                        ZADD recommendation:user:{userId}  ZADD recommendation:category:{categoryId}
                        score = category trending score    score = category trending score
                        (RECENT_PURCHASE_CATEGORY)         (CATEGORY_POPULAR)

# relevance_score 계산 방식:
# recommendation:user:{userId} 의 score = 해당 카테고리의 ranking:trending:category:{id}:1h score
# (별도 ML 없이 카테고리 인기도를 개인화 추천의 score로 재활용)
```

### 7-2. DLQ 처리

```
처리 실패 (예외 발생)
    │
    ├─ Redis: DEL processed:event:{eventId}   ← 재처리 가능하도록 복구
    └─ Kafka produce: user-events-dlq

DlqConsumer (@KafkaListener)
    └─ ERROR 로그 기록 (eventId, 실패 사유 포함)
    // TODO: 자동 재처리 스케줄러
    // TODO: 알림 연동 (Slack, 이메일)
```

### 7-3. MySQL 캐시 전략 (Event Processor)

```java
// Caffeine local cache — 네트워크 없이 JVM 내 조회
// 상품 카테고리는 자주 변하지 않는 마스터 데이터
@Cacheable("productCategory")
@Transactional(readOnly = true)
public String getCategoryId(String productId) {
    return productRepository.findCategoryIdById(productId);
}

// CacheConfig: maximumSize=1000, expireAfterWrite=30min
```

> Redis 캐시 대신 Caffeine을 선택한 이유:
> Event Processor 인스턴스가 1개이므로 캐시 일관성 문제 없음.
> 네트워크 레이턴시 없이 JVM 메모리에서 즉시 조회 가능.

---

## 8. AWS 배포 구성

| 컴포넌트 | AWS 서비스 | 비고 |
|---|---|---|
| API Server | EC2 (t3.small) | Docker 컨테이너 실행 |
| Event Processor | EC2 (t3.small) | Docker 컨테이너 실행 |
| Kafka | EC2 (t3.small) | Self-hosted (비용 절감) |
| MySQL | RDS MySQL 8.0 (db.t3.micro) | |
| Redis | ElastiCache Redis 7 (cache.t3.micro) | |

> Confluent Cloud free tier 대신 Self-hosted Kafka를 선택하는 경우
> KRaft 모드로 ZooKeeper 없이 EC2 1대에서 단독 운영 가능 (데모 수준).

---

## 9. 범위 정의

### MVP 포함

- 이벤트 수집 API (eventId 반환)
- Kafka 기반 비동기 이벤트 파이프라인
- 실시간 인기 상품 랭킹 (1h, 24h)
- 카테고리별 랭킹
- 개인화 추천 (rule-based, Redis Sorted Set)
- 카테고리 기반 추천 (fallback)
- DLQ + Idempotency
- 통일된 에러 응답 포맷
- Docker Compose 로컬 환경
- AWS 배포

### MVP 제외 (확장 포인트)

- Time Decay 랭킹 (점수 감쇄 스케줄러)
- ML 기반 추천 모델
- DLQ 자동 재처리
- 인증/인가 (JWT 등)
- A/B 테스팅
- 검색 (Elasticsearch)
- Kubernetes

---

## 10. 4주 구현 계획

| 주차 | 목표 |
|---|---|
| 1주차 | Maven multi-module 구조 생성, Docker Compose 환경, seed data, 마스터 데이터 CRUD |
| 2주차 | 이벤트 수집 API → Kafka produce → Consumer 파이프라인, Idempotency, DLQ |
| 3주차 | 랭킹 시스템 (Redis ZINCRBY), 추천 시스템 (Redis ZADD), 조회 API |
| 4주차 | AWS 배포, 통합 테스트, README/문서화, 데모 시나리오 정리 |
