# Commerce Recommendation System

실시간 사용자 행동 이벤트를 기반으로 인기 상품 랭킹과 개인화 추천을 제공하는 B2C 백엔드 시스템입니다.

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [시스템 아키텍처](#시스템-아키텍처)
- [기술 스택](#기술-스택)
- [모듈 구조](#모듈-구조)
- [API 명세](#api-명세)
- [데이터 흐름](#데이터-흐름)
- [Redis 키 설계](#redis-키-설계)
- [로컬 실행 방법](#로컬-실행-방법)
- [테스트 실행](#테스트-실행)
- [AWS 배포](#aws-배포)

---

## 프로젝트 개요

사용자가 상품을 조회(VIEW), 클릭(CLICK), 구매(PURCHASE)하면 행동 이벤트가 Kafka로 비동기 처리되어 Redis에 랭킹 및 추천 데이터가 실시간으로 반영됩니다.

**핵심 기능:**
- 사용자 행동 이벤트 수집 (비동기, fire-and-forget)
- 실시간 인기 상품 랭킹 (1h / 24h 윈도우, 전체 / 카테고리별)
- 구매 기반 개인화 추천 (유저별 / 카테고리별)
- 중복 이벤트 방지 (Redis Idempotency)
- 처리 실패 이벤트 DLQ(Dead Letter Queue) 보관

---

## 시스템 아키텍처

```
클라이언트
    │
    ▼
┌─────────────┐        ┌─────────────────┐
│  api-server  │──────▶│     Kafka        │
│  (port 8080) │       │  user-events     │
└──────┬───────┘       └────────┬─────────┘
       │                        │
       │ MySQL (master data)     ▼
       │ Redis (read ranking) ┌─────────────────┐
       │                      │ event-processor  │
       ▼                      │   (port 8081)    │
┌─────────────┐               └──────┬───────────┘
│    MySQL     │◀──────────────────── │ (product lookup)
└─────────────┘               ┌──────▼───────────┐
                               │     Redis         │
                               │  ranking / recs   │
                               └──────────────────┘
```

**api-server**: HTTP 요청 수신 → Kafka produce → MySQL/Redis 조회 응답

**event-processor**: Kafka consume → 멱등성 검사 → Redis 랭킹/추천 갱신 → 실패 시 DLQ 전송

---

## 기술 스택

| 영역 | 기술 |
|---|---|
| 언어 / 프레임워크 | Java 21, Spring Boot 3.2.3 |
| 빌드 | Maven (멀티모듈) |
| 메시지 브로커 | Apache Kafka (KRaft 모드, ZooKeeper 없음) |
| 캐시 / 랭킹 | Redis 7 (Sorted Set) |
| 데이터베이스 | MySQL 8 |
| 로컬 캐시 | Caffeine (카테고리 조회) |
| 테스트 | JUnit 5, Mockito |
| 컨테이너 | Docker, Docker Compose |
| 배포 | AWS EC2 + RDS + ElastiCache |

---

## 모듈 구조

```
commerce-recommendation-system/
├── common/                          # 공유 모듈
│   └── src/main/java/com/commerce/common/
│       ├── event/UserBehaviorEvent.java   # Kafka 이벤트 스키마
│       ├── event/EventType.java           # VIEW(1), CLICK(3), PURCHASE(10)
│       ├── event/EventContext.java        # 이벤트 컨텍스트 (page, position)
│       └── dto/ErrorResponse.java         # 공통 에러 응답
│
├── api-server/                      # HTTP API 서버 (port 8080)
│   └── src/main/java/com/commerce/api/
│       ├── domain/                        # JPA 엔티티 (Category, Product, User)
│       ├── repository/                    # Spring Data JPA
│       ├── service/                       # 비즈니스 로직
│       ├── controller/                    # REST 컨트롤러
│       ├── kafka/EventProducer.java       # Kafka 이벤트 발행
│       ├── config/                        # Kafka, Redis 설정
│       └── exception/                     # 통합 예외 처리
│
├── event-processor/                 # Kafka Consumer 서버 (port 8081)
│   └── src/main/java/com/commerce/processor/
│       ├── kafka/EventConsumer.java       # Kafka 이벤트 소비
│       ├── kafka/DlqProducer.java         # DLQ 전송
│       ├── idempotency/IdempotencyService # Redis 멱등성 검사
│       ├── ranking/RankingService.java    # Redis ZSet 랭킹 갱신
│       ├── recommendation/               # Redis ZSet 추천 갱신
│       ├── repository/ProductCategoryRepository # Caffeine 캐시 + JPA
│       └── config/                        # Kafka, Redis, Caffeine 설정
│
├── docker-compose.yml               # 로컬 개발 환경
├── docker-compose.prod.yml          # AWS EC2 배포 환경
└── .env.example                     # 환경 변수 샘플
```

---

## API 명세

### 이벤트 수집

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/v1/events` | 사용자 행동 이벤트 발행 |

**Request Body:**
```json
{
  "userId": "user-001",
  "productId": "prod-001",
  "eventType": "PURCHASE",
  "sessionId": "session-abc",
  "context": {
    "page": "product_detail",
    "position": 1
  }
}
```

**Response (202 Accepted):**
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ACCEPTED"
}
```

### 랭킹 조회

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/v1/rankings/trending` | 전체 인기 상품 랭킹 |
| GET | `/api/v1/rankings/trending/categories/{categoryId}` | 카테고리별 인기 상품 랭킹 |

**Query Parameters:** `window=1h` (또는 `24h`), `limit=20` (최대 100)

**Response (200 OK):**
```json
{
  "window": "1h",
  "generatedAt": "2026-03-30T10:00:00Z",
  "products": [
    {
      "rank": 1,
      "productId": "prod-001",
      "name": "LG그램 17인치",
      "categoryId": "laptop",
      "price": 1890000,
      "score": 42.0
    }
  ]
}
```

### 추천 조회

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/v1/recommendations/users/{userId}` | 유저 개인화 추천 |
| GET | `/api/v1/recommendations/categories/{categoryId}` | 카테고리 인기 추천 |

**Query Parameters:** `limit=10` (최대 100)

**Response (200 OK):**
```json
{
  "userId": "user-001",
  "products": [
    {
      "productId": "prod-005",
      "name": "MacBook Pro M3",
      "categoryId": "laptop",
      "price": 2890000,
      "reason": "RECENT_PURCHASE_CATEGORY"
    }
  ]
}
```

### 상품 / 유저 관리

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/v1/products` | 상품 등록 |
| GET | `/api/v1/products/{productId}` | 상품 조회 |
| POST | `/api/v1/users` | 유저 등록 |
| GET | `/api/v1/users/{userId}` | 유저 조회 |

### 에러 응답 형식

모든 에러는 동일한 구조로 반환됩니다:

```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "prod-999",
  "timestamp": "2026-03-30T10:00:00Z",
  "path": "/api/v1/products/prod-999"
}
```

---

## 데이터 흐름

### 이벤트 처리 파이프라인

```
1. POST /api/v1/events
       │
       ▼
2. EventService.publish()
   → UUID eventId 생성
   → UserBehaviorEvent 구성 (schemaVersion: "1.0")
       │
       ▼
3. EventProducer.send()
   → kafkaTemplate.send(topic="user-events", key=userId, value=event)
   → 비동기 fire-and-forget (202 즉시 반환)
       │
       ▼ (Kafka consumer group: event-processor-group)
4. EventConsumer.consume()
   → IdempotencyService.tryMarkProcessed(eventId)
     - Redis SETNX "processed:event:{eventId}" TTL 24h
     - 중복이면 즉시 skip
       │
       ▼
5. process(event)
   → ProductCategoryRepository.getCategoryId(productId)
     - Caffeine 캐시 (maxSize=1000, TTL=30min)
     - 미스 시 MySQL 조회
       │
       ▼
6. RankingService.incrementScore(productId, categoryId, weight)
   → ZINCRBY ranking:trending:1h   {productId} {weight}
   → ZINCRBY ranking:trending:24h  {productId} {weight}
   → ZINCRBY ranking:trending:category:{categoryId}:1h {productId} {weight}
       │
       ▼ (PURCHASE 이벤트만)
7. RecommendationService
   → ZADD recommendation:user:{userId} {score} {productId}  (TTL: 1h sliding)
   → ZADD recommendation:category:{categoryId} {score} {productId}  (TTL: 6h sliding)
       │
       ▼ (예외 발생 시)
8. IdempotencyService.unmarkProcessed(eventId)  ← 롤백
   DlqProducer.send(event) → user-events-dlq
```

### 이벤트 가중치

| EventType | 가중치 |
|---|---|
| VIEW | 1 |
| CLICK | 3 |
| PURCHASE | 10 |

### 랭킹 조회 흐름 (N+1 방지)

```
GET /api/v1/rankings/trending
       │
       ▼
Redis ZREVRANGEBYSCORE ranking:trending:1h 0 19 WITHSCORES
→ productId 목록 추출
       │
       ▼
MySQL: SELECT ... FROM products WHERE id IN (...) AND status = 'ACTIVE'
       (단일 IN 쿼리, @EntityGraph로 category 즉시 로딩)
       │
       ▼
RankingResponse 조립 후 반환
```

---

## Redis 키 설계

| 키 패턴 | 타입 | 용도 | TTL |
|---|---|---|---|
| `ranking:trending:1h` | ZSet | 전체 1시간 랭킹 | 없음 (ZINCRBY) |
| `ranking:trending:24h` | ZSet | 전체 24시간 랭킹 | 없음 |
| `ranking:trending:category:{id}:1h` | ZSet | 카테고리별 1시간 랭킹 | 없음 |
| `recommendation:user:{userId}` | ZSet | 유저 추천 목록 | 1h (슬라이딩) |
| `recommendation:category:{categoryId}` | ZSet | 카테고리 추천 목록 | 6h (슬라이딩) |
| `processed:event:{eventId}` | String | 이벤트 중복 처리 방지 | 24h |

---

## 로컬 실행 방법

### 사전 요구사항

- Java 21+
- Maven 3.8+
- Docker Desktop

### 1단계: 환경 변수 설정

```bash
cp .env.example .env
# .env 파일 내용 (기본값 그대로 사용 가능)
# MYSQL_ROOT_PASSWORD=rootpassword
# MYSQL_USER=commerce
# MYSQL_PASSWORD=commerce123
```

### 2단계: 인프라 실행

```bash
docker-compose up -d kafka mysql redis
```

MySQL이 준비될 때까지 약 20초 대기합니다.

```bash
# MySQL 준비 확인
docker-compose logs mysql | grep "ready for connections"
```

### 3단계: api-server 실행

```bash
cd api-server
mvn spring-boot:run
```

시작 시 `schema.sql`로 테이블 생성, `data.sql`로 시드 데이터가 자동으로 삽입됩니다.

### 4단계: event-processor 실행 (새 터미널)

```bash
cd event-processor
mvn spring-boot:run
```

### 5단계: 동작 확인

```bash
# 상품 조회
curl http://localhost:8080/api/v1/products/prod-001

# 이벤트 발행
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-001",
    "productId": "prod-001",
    "eventType": "PURCHASE",
    "sessionId": "test-session",
    "context": {"page": "product_detail", "position": 1}
  }'

# 랭킹 조회 (이벤트 처리 후 약 1~2초 대기)
curl "http://localhost:8080/api/v1/rankings/trending?window=1h"

# 유저 추천 조회
curl "http://localhost:8080/api/v1/recommendations/users/user-001"
```

### 시드 데이터

`data.sql`에 아래 데이터가 포함되어 있습니다.

**카테고리:** electronics, sports, fashion, laptop, phone, running

**상품:**
| ID | 이름 | 카테고리 | 가격 |
|---|---|---|---|
| prod-001 | LG그램 17인치 | laptop | 1,890,000 |
| prod-002 | 삼성 갤럭시 S24 | phone | 1,200,000 |
| prod-003 | 나이키 페가수스 41 | running | 169,000 |
| prod-004 | 아디다스 울트라부스트 | running | 189,000 |
| prod-005 | MacBook Pro M3 | laptop | 2,890,000 |
| prod-006 | 아이폰 15 Pro | phone | 1,550,000 |

**유저:** user-001(김철수), user-002(이영희), user-003(박민준)

---

## 테스트 실행

```bash
# 전체 테스트
mvn test

# 모듈별 테스트
mvn test -pl api-server
mvn test -pl event-processor

# 특정 테스트 클래스
mvn test -pl api-server -Dtest=ProductServiceTest
mvn test -pl event-processor -Dtest=EventConsumerTest
```

**테스트 목록 (총 19개):**

| 모듈 | 테스트 클래스 | 테스트 수 |
|---|---|---|
| api-server | ProductServiceTest | 3 |
| api-server | EventServiceTest | 2 |
| api-server | RankingServiceTest | 2 |
| api-server | RecommendationServiceTest | 3 |
| event-processor | IdempotencyServiceTest | 3 |
| event-processor | RankingServiceTest | 2 |
| event-processor | EventConsumerTest | 4 |

모든 테스트는 Mockito 기반 순수 단위 테스트로 외부 의존성(DB, Redis, Kafka) 없이 실행됩니다.

---

## AWS 배포

### 인프라 구성

| 서비스 | 용도 |
|---|---|
| EC2 (t3.small) | Kafka + api-server + event-processor 실행 |
| RDS MySQL 8.0 | 마스터 데이터 (카테고리, 상품, 유저) |
| ElastiCache Redis 7 | 랭킹/추천 데이터 |

### 배포 순서

**1. RDS 준비**
```bash
# RDS MySQL 생성 후 schema.sql 실행
mysql -h {RDS_ENDPOINT} -u {USER} -p commerce < api-server/src/main/resources/schema.sql
mysql -h {RDS_ENDPOINT} -u {USER} -p commerce < api-server/src/main/resources/data.sql
```

**2. EC2 준비**
```bash
# Docker 설치
sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo usermod -aG docker ec2-user

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

**3. 애플리케이션 빌드 및 전송**
```bash
# 로컬에서 JAR 빌드
mvn clean package -DskipTests

# Docker 이미지 빌드
docker build -t api-server:latest api-server/
docker build -t event-processor:latest event-processor/

# EC2로 이미지 전송 (scp 또는 ECR 사용)
docker save api-server:latest | ssh ec2-user@{EC2_IP} "docker load"
docker save event-processor:latest | ssh ec2-user@{EC2_IP} "docker load"
```

**4. EC2에서 실행**
```bash
# .env 파일 설정
cat > .env << EOF
MYSQL_USER=commerce
MYSQL_PASSWORD=your_password
EC2_PUBLIC_IP=$(curl -s ifconfig.me)
RDS_ENDPOINT=your-rds-endpoint.rds.amazonaws.com
ELASTICACHE_ENDPOINT=your-cache.cache.amazonaws.com
API_SERVER_IMAGE=api-server:latest
EVENT_PROCESSOR_IMAGE=event-processor:latest
EOF

# 실행
docker-compose -f docker-compose.prod.yml up -d
```

### 포트 / 보안 그룹 설정

| 포트 | 용도 |
|---|---|
| 8080 | api-server HTTP |
| 9092 | Kafka (외부 접근 필요 시) |
| 3306 | RDS (EC2 → RDS, 공개 차단) |
| 6379 | ElastiCache (EC2 → Redis, 공개 차단) |

---

## 프로젝트 구조 설계 결정

### 왜 Maven 멀티모듈인가?
`common` 모듈에 Kafka 이벤트 스키마를 한 곳에서 관리하여 producer(api-server)와 consumer(event-processor) 간 스키마 불일치를 컴파일 타임에 방지합니다.

### 왜 Kafka KRaft 모드인가?
ZooKeeper 의존성을 제거하여 단일 EC2에서 운영 가능한 단순한 구성을 유지합니다. (Kafka 3.3+ 정식 지원)

### 왜 Redis Sorted Set인가?
`ZINCRBY`로 점수를 원자적으로 증가시키고 `ZREVRANGE`로 상위 N개를 O(log N)에 조회합니다. 별도 집계 없이 실시간 랭킹이 가능합니다.

### 왜 fire-and-forget 이벤트 API인가?
이벤트 수집 API가 Kafka 응답을 기다리면 브로커 장애 시 사용자 응답이 지연됩니다. `202 Accepted`를 즉시 반환하고 처리 실패는 DLQ로 격리합니다.
