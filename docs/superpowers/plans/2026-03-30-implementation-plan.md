# Commerce Recommendation System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 사용자 행동 이벤트를 Kafka로 비동기 처리하여 실시간 인기 상품 랭킹과 개인화 추천을 제공하는 B2C 백엔드 시스템을 구축한다.

**Architecture:** Maven multi-module (common / api-server / event-processor) + Kafka 이벤트 파이프라인 + Redis Sorted Set 기반 랭킹/추천 + MySQL 마스터 데이터. API Server는 HTTP 요청을 받아 Kafka로 produce하고, Event Processor는 consume하여 Redis에 랭킹/추천 데이터를 쓴다.

**Tech Stack:** Java 21, Spring Boot 3.2.3, Maven multi-module, Spring Kafka (JSON), Spring Data JPA (Hibernate), Spring Data Redis (Lettuce / StringRedisTemplate), Caffeine cache, MySQL 8, Redis 7, Kafka (cp-kafka), Docker Compose, JUnit 5, Mockito

---

## File Map

```
commerce-recommendation-system/
├── pom.xml                                          [CREATE] parent pom
├── docker-compose.yml                               [CREATE]
├── .env.example                                     [CREATE]
├── common/
│   ├── pom.xml                                      [CREATE]
│   └── src/main/java/com/commerce/common/
│       ├── event/UserBehaviorEvent.java             [CREATE]
│       ├── event/EventType.java                     [CREATE]
│       ├── event/EventContext.java                  [CREATE]
│       └── dto/ErrorResponse.java                   [CREATE]
├── api-server/
│   ├── pom.xml                                      [CREATE]
│   ├── Dockerfile                                   [CREATE]
│   └── src/
│       ├── main/
│       │   ├── java/com/commerce/api/
│       │   │   ├── ApiServerApplication.java        [CREATE]
│       │   │   ├── config/KafkaProducerConfig.java  [CREATE]
│       │   │   ├── config/RedisConfig.java          [CREATE]
│       │   │   ├── domain/Category.java             [CREATE]
│       │   │   ├── domain/Product.java              [CREATE]
│       │   │   ├── domain/ProductStatus.java        [CREATE]
│       │   │   ├── domain/User.java                 [CREATE]
│       │   │   ├── repository/CategoryRepository.java [CREATE]
│       │   │   ├── repository/ProductRepository.java  [CREATE]
│       │   │   ├── repository/UserRepository.java     [CREATE]
│       │   │   ├── exception/ErrorCode.java         [CREATE]
│       │   │   ├── exception/BusinessException.java [CREATE]
│       │   │   ├── exception/GlobalExceptionHandler.java [CREATE]
│       │   │   ├── kafka/EventProducer.java         [CREATE]
│       │   │   ├── dto/EventRequest.java            [CREATE]
│       │   │   ├── dto/EventResponse.java           [CREATE]
│       │   │   ├── dto/ProductRequest.java          [CREATE]
│       │   │   ├── dto/ProductResponse.java         [CREATE]
│       │   │   ├── dto/UserRequest.java             [CREATE]
│       │   │   ├── dto/UserResponse.java            [CREATE]
│       │   │   ├── dto/RankingResponse.java         [CREATE]
│       │   │   ├── dto/RecommendationResponse.java  [CREATE]
│       │   │   ├── service/EventService.java        [CREATE]
│       │   │   ├── service/ProductService.java      [CREATE]
│       │   │   ├── service/UserService.java         [CREATE]
│       │   │   ├── service/RankingService.java      [CREATE]
│       │   │   ├── service/RecommendationService.java [CREATE]
│       │   │   ├── controller/EventController.java  [CREATE]
│       │   │   ├── controller/ProductController.java [CREATE]
│       │   │   ├── controller/UserController.java   [CREATE]
│       │   │   ├── controller/RankingController.java [CREATE]
│       │   │   └── controller/RecommendationController.java [CREATE]
│       │   └── resources/
│       │       ├── application.yml                  [CREATE]
│       │       └── schema.sql                       [CREATE]
│       └── test/java/com/commerce/api/
│           ├── service/EventServiceTest.java        [CREATE]
│           ├── service/RankingServiceTest.java      [CREATE]
│           ├── service/RecommendationServiceTest.java [CREATE]
│           ├── controller/EventControllerTest.java  [CREATE]
│           └── controller/RankingControllerTest.java [CREATE]
├── event-processor/
│   ├── pom.xml                                      [CREATE]
│   ├── Dockerfile                                   [CREATE]
│   └── src/
│       ├── main/
│       │   ├── java/com/commerce/processor/
│       │   │   ├── EventProcessorApplication.java   [CREATE]
│       │   │   ├── config/KafkaConsumerConfig.java  [CREATE]
│       │   │   ├── config/RedisConfig.java          [CREATE]
│       │   │   ├── config/CacheConfig.java          [CREATE]
│       │   │   ├── repository/ProductCategoryRepository.java [CREATE]
│       │   │   ├── idempotency/IdempotencyService.java [CREATE]
│       │   │   ├── ranking/RankingService.java      [CREATE]
│       │   │   ├── recommendation/RecommendationService.java [CREATE]
│       │   │   ├── kafka/EventConsumer.java         [CREATE]
│       │   │   └── kafka/DlqProducer.java           [CREATE]
│       │   └── resources/application.yml            [CREATE]
│       └── test/java/com/commerce/processor/
│           ├── idempotency/IdempotencyServiceTest.java [CREATE]
│           ├── ranking/RankingServiceTest.java      [CREATE]
│           └── kafka/EventConsumerTest.java         [CREATE]
```

---

## Phase 1: Project Foundation (Week 1)

### Task 1: Maven Multi-Module 구조 생성

**Files:**
- Create: `pom.xml`
- Create: `common/pom.xml`
- Create: `api-server/pom.xml`
- Create: `event-processor/pom.xml`

- [ ] **Step 1: parent pom.xml 작성**

```xml
<!-- pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.3</version>
        <relativePath/>
    </parent>

    <groupId>com.commerce</groupId>
    <artifactId>commerce-recommendation-system</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>common</module>
        <module>api-server</module>
        <module>event-processor</module>
    </modules>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.commerce</groupId>
                <artifactId>common</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

- [ ] **Step 2: common/pom.xml 작성**

```xml
<!-- common/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.commerce</groupId>
        <artifactId>commerce-recommendation-system</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>common</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: api-server/pom.xml 작성**

```xml
<!-- api-server/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.commerce</groupId>
        <artifactId>commerce-recommendation-system</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>api-server</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.commerce</groupId>
            <artifactId>common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: event-processor/pom.xml 작성**

```xml
<!-- event-processor/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.commerce</groupId>
        <artifactId>commerce-recommendation-system</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>event-processor</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.commerce</groupId>
            <artifactId>common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 5: 빌드 확인**

```bash
cd /path/to/commerce-recommendation-system
mvn clean compile -q
```

Expected: `BUILD SUCCESS` (소스 없으므로 컴파일 스킵, 구조만 검증)

- [ ] **Step 6: Commit**

```bash
git init
git add pom.xml common/pom.xml api-server/pom.xml event-processor/pom.xml
git commit -m "feat: initialize maven multi-module project structure"
```

---

### Task 2: Docker Compose 환경 구성

**Files:**
- Create: `docker-compose.yml`
- Create: `.env.example`

- [ ] **Step 1: docker-compose.yml 작성**

```yaml
# docker-compose.yml
version: '3.8'

services:
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_LISTENERS: PLAINTEXT://kafka:29092,CONTROLLER://kafka:9093,PLAINTEXT_HOST://0.0.0.0:9092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk

  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: commerce
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    volumes:
      - mysql_data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru

  api-server:
    build: ./api-server
    ports:
      - "8080:8080"
    depends_on:
      - kafka
      - mysql
      - redis
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/commerce?useSSL=false&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: ${MYSQL_USER}
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      SPRING_REDIS_HOST: redis

  event-processor:
    build: ./event-processor
    ports:
      - "8081:8081"
    depends_on:
      - kafka
      - mysql
      - redis
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/commerce?useSSL=false&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: ${MYSQL_USER}
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      SPRING_REDIS_HOST: redis

volumes:
  mysql_data:
```

- [ ] **Step 2: .env.example 작성**

```bash
# .env.example — 복사하여 .env로 사용. .env는 .gitignore에 추가할 것
MYSQL_ROOT_PASSWORD=rootpassword
MYSQL_USER=commerce
MYSQL_PASSWORD=commerce123
```

- [ ] **Step 3: .gitignore 생성**

```
# .gitignore
.env
target/
*.class
*.jar
.idea/
*.iml
```

- [ ] **Step 4: Commit**

```bash
git add docker-compose.yml .env.example .gitignore
git commit -m "feat: add docker-compose environment with kafka, mysql, redis"
```

---

### Task 3: Common 모듈 — 이벤트 스키마

**Files:**
- Create: `common/src/main/java/com/commerce/common/event/EventType.java`
- Create: `common/src/main/java/com/commerce/common/event/EventContext.java`
- Create: `common/src/main/java/com/commerce/common/event/UserBehaviorEvent.java`
- Create: `common/src/main/java/com/commerce/common/dto/ErrorResponse.java`

- [ ] **Step 1: EventType.java 작성**

```java
// common/src/main/java/com/commerce/common/event/EventType.java
package com.commerce.common.event;

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
```

- [ ] **Step 2: EventContext.java 작성**

```java
// common/src/main/java/com/commerce/common/event/EventContext.java
package com.commerce.common.event;

public record EventContext(
    String page,
    Integer position
) {}
```

- [ ] **Step 3: UserBehaviorEvent.java 작성**

```java
// common/src/main/java/com/commerce/common/event/UserBehaviorEvent.java
package com.commerce.common.event;

import java.time.Instant;

public record UserBehaviorEvent(
    String eventId,
    String schemaVersion,
    String userId,
    String productId,
    EventType eventType,
    String sessionId,
    EventContext context,
    Instant timestamp
) {}
```

- [ ] **Step 4: ErrorResponse.java 작성**

```java
// common/src/main/java/com/commerce/common/dto/ErrorResponse.java
package com.commerce.common.dto;

import java.time.Instant;

public record ErrorResponse(
    String code,
    String message,
    Instant timestamp,
    String path
) {}
```

- [ ] **Step 5: 빌드 확인**

```bash
mvn clean compile -pl common -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add common/src/
git commit -m "feat: add common module with event schema and error response DTO"
```

---

### Task 4: API Server — 도메인 엔티티 + 스키마

**Files:**
- Create: `api-server/src/main/java/com/commerce/api/domain/Category.java`
- Create: `api-server/src/main/java/com/commerce/api/domain/ProductStatus.java`
- Create: `api-server/src/main/java/com/commerce/api/domain/Product.java`
- Create: `api-server/src/main/java/com/commerce/api/domain/User.java`
- Create: `api-server/src/main/resources/schema.sql`
- Create: `api-server/src/main/resources/application.yml`
- Create: `api-server/src/main/java/com/commerce/api/ApiServerApplication.java`

- [ ] **Step 1: ApiServerApplication.java 작성**

```java
// api-server/src/main/java/com/commerce/api/ApiServerApplication.java
package com.commerce.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiServerApplication.class, args);
    }
}
```

- [ ] **Step 2: application.yml 작성**

```yaml
# api-server/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/commerce?useSSL=false&allowPublicKeyRetrieval=true
    username: commerce
    password: commerce123
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
    show-sql: false
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  data:
    redis:
      host: localhost
      port: 6379

server:
  port: 8080

kafka:
  topic:
    user-events: user-events
```

- [ ] **Step 3: schema.sql 작성**

```sql
-- api-server/src/main/resources/schema.sql
CREATE TABLE IF NOT EXISTS categories (
    id         VARCHAR(50)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    parent_id  VARCHAR(50)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS users (
    id         VARCHAR(36)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_email (email)
);

CREATE TABLE IF NOT EXISTS products (
    id          VARCHAR(36)   NOT NULL,
    name        VARCHAR(255)  NOT NULL,
    category_id VARCHAR(50)   NOT NULL,
    price       DECIMAL(10,2) NOT NULL,
    status      VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id),
    INDEX idx_category_status (category_id, status)
);
```

- [ ] **Step 4: Category.java 작성**

```java
// api-server/src/main/java/com/commerce/api/domain/Category.java
package com.commerce.api.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    protected Category() {}

    public Category(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Category getParent() { return parent; }
}
```

- [ ] **Step 5: ProductStatus.java 작성**

```java
// api-server/src/main/java/com/commerce/api/domain/ProductStatus.java
package com.commerce.api.domain;

public enum ProductStatus {
    ACTIVE, INACTIVE
}
```

- [ ] **Step 6: Product.java 작성**

```java
// api-server/src/main/java/com/commerce/api/domain/Product.java
package com.commerce.api.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.ACTIVE;

    @CreationTimestamp
    private LocalDateTime createdAt;

    protected Product() {}

    public Product(String name, Category category, BigDecimal price) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.category = category;
        this.price = price;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public ProductStatus getStatus() { return status; }
}
```

- [ ] **Step 7: User.java 작성**

```java
// api-server/src/main/java/com/commerce/api/domain/User.java
package com.commerce.api.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @CreationTimestamp
    private LocalDateTime createdAt;

    protected User() {}

    public User(String name, String email) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
}
```

- [ ] **Step 8: Commit**

```bash
git add api-server/src/main/
git commit -m "feat: add domain entities, schema.sql, and application config for api-server"
```

---

### Task 5: API Server — Repository + Exception + CRUD

**Files:**
- Create: `api-server/src/main/java/com/commerce/api/repository/CategoryRepository.java`
- Create: `api-server/src/main/java/com/commerce/api/repository/ProductRepository.java`
- Create: `api-server/src/main/java/com/commerce/api/repository/UserRepository.java`
- Create: `api-server/src/main/java/com/commerce/api/exception/ErrorCode.java`
- Create: `api-server/src/main/java/com/commerce/api/exception/BusinessException.java`
- Create: `api-server/src/main/java/com/commerce/api/exception/GlobalExceptionHandler.java`
- Create: `api-server/src/main/java/com/commerce/api/service/ProductService.java`
- Create: `api-server/src/main/java/com/commerce/api/service/UserService.java`
- Create: `api-server/src/main/java/com/commerce/api/dto/ProductRequest.java`
- Create: `api-server/src/main/java/com/commerce/api/dto/ProductResponse.java`
- Create: `api-server/src/main/java/com/commerce/api/dto/UserRequest.java`
- Create: `api-server/src/main/java/com/commerce/api/dto/UserResponse.java`
- Create: `api-server/src/main/java/com/commerce/api/controller/ProductController.java`
- Create: `api-server/src/main/java/com/commerce/api/controller/UserController.java`
- Test: `api-server/src/test/java/com/commerce/api/service/ProductServiceTest.java`

- [ ] **Step 1: Repository 인터페이스 3개 작성**

```java
// api-server/src/main/java/com/commerce/api/repository/CategoryRepository.java
package com.commerce.api.repository;

import com.commerce.api.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, String> {}
```

```java
// api-server/src/main/java/com/commerce/api/repository/ProductRepository.java
package com.commerce.api.repository;

import com.commerce.api.domain.Product;
import com.commerce.api.domain.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findAllByIdInAndStatus(List<String> ids, ProductStatus status);
}
```

```java
// api-server/src/main/java/com/commerce/api/repository/UserRepository.java
package com.commerce.api.repository;

import com.commerce.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {}
```

- [ ] **Step 2: ErrorCode + BusinessException 작성**

```java
// api-server/src/main/java/com/commerce/api/exception/ErrorCode.java
package com.commerce.api.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Category not found"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),
    EVENT_PUBLISH_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "Event publish failed"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getDefaultMessage() { return defaultMessage; }
}
```

```java
// api-server/src/main/java/com/commerce/api/exception/BusinessException.java
package com.commerce.api.exception;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() { return errorCode; }
}
```

- [ ] **Step 3: GlobalExceptionHandler 작성**

```java
// api-server/src/main/java/com/commerce/api/exception/GlobalExceptionHandler.java
package com.commerce.api.exception;

import com.commerce.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        return ResponseEntity
            .status(ex.getErrorCode().getHttpStatus())
            .body(new ErrorResponse(ex.getErrorCode().name(), ex.getMessage(),
                Instant.now(), req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                           HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .findFirst().orElse("Validation failed");
        return ResponseEntity.badRequest()
            .body(new ErrorResponse(ErrorCode.INVALID_REQUEST.name(), message,
                Instant.now(), req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest req) {
        return ResponseEntity.internalServerError()
            .body(new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR.name(), ex.getMessage(),
                Instant.now(), req.getRequestURI()));
    }
}
```

- [ ] **Step 4: Product/User DTO + Service + Controller 작성**

```java
// api-server/src/main/java/com/commerce/api/dto/ProductRequest.java
package com.commerce.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank String name,
    @NotBlank String categoryId,
    @NotNull @Positive BigDecimal price
) {}
```

```java
// api-server/src/main/java/com/commerce/api/dto/ProductResponse.java
package com.commerce.api.dto;

import com.commerce.api.domain.Product;
import java.math.BigDecimal;

public record ProductResponse(
    String productId,
    String name,
    String categoryId,
    BigDecimal price,
    String status
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(),
            p.getCategory().getId(), p.getPrice(), p.getStatus().name());
    }
}
```

```java
// api-server/src/main/java/com/commerce/api/dto/UserRequest.java
package com.commerce.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRequest(@NotBlank String name, @NotBlank @Email String email) {}
```

```java
// api-server/src/main/java/com/commerce/api/dto/UserResponse.java
package com.commerce.api.dto;

import com.commerce.api.domain.User;

public record UserResponse(String userId, String name, String email) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail());
    }
}
```

```java
// api-server/src/main/java/com/commerce/api/service/ProductService.java
package com.commerce.api.service;

import com.commerce.api.domain.Product;
import com.commerce.api.exception.BusinessException;
import com.commerce.api.exception.ErrorCode;
import com.commerce.api.repository.CategoryRepository;
import com.commerce.api.repository.ProductRepository;
import com.commerce.api.dto.ProductRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Product create(ProductRequest req) {
        var category = categoryRepository.findById(req.categoryId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, req.categoryId()));
        return productRepository.save(new Product(req.name(), category, req.price()));
    }

    public Product findById(String id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, id));
    }
}
```

```java
// api-server/src/main/java/com/commerce/api/service/UserService.java
package com.commerce.api.service;

import com.commerce.api.domain.User;
import com.commerce.api.dto.UserRequest;
import com.commerce.api.exception.BusinessException;
import com.commerce.api.exception.ErrorCode;
import com.commerce.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User create(UserRequest req) {
        return userRepository.save(new User(req.name(), req.email()));
    }

    public User findById(String id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, id));
    }
}
```

```java
// api-server/src/main/java/com/commerce/api/controller/ProductController.java
package com.commerce.api.controller;

import com.commerce.api.dto.ProductRequest;
import com.commerce.api.dto.ProductResponse;
import com.commerce.api.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest req) {
        return ProductResponse.from(productService.create(req));
    }

    @GetMapping("/{productId}")
    public ProductResponse findById(@PathVariable String productId) {
        return ProductResponse.from(productService.findById(productId));
    }
}
```

```java
// api-server/src/main/java/com/commerce/api/controller/UserController.java
package com.commerce.api.controller;

import com.commerce.api.dto.UserRequest;
import com.commerce.api.dto.UserResponse;
import com.commerce.api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequest req) {
        return UserResponse.from(userService.create(req));
    }

    @GetMapping("/{userId}")
    public UserResponse findById(@PathVariable String userId) {
        return UserResponse.from(userService.findById(userId));
    }
}
```

- [ ] **Step 5: ProductService 단위 테스트 작성**

```java
// api-server/src/test/java/com/commerce/api/service/ProductServiceTest.java
package com.commerce.api.service;

import com.commerce.api.domain.Category;
import com.commerce.api.domain.Product;
import com.commerce.api.dto.ProductRequest;
import com.commerce.api.exception.BusinessException;
import com.commerce.api.repository.CategoryRepository;
import com.commerce.api.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;
    @InjectMocks ProductService productService;

    @Test
    void create_성공() {
        var category = new Category("electronics", "전자제품");
        var req = new ProductRequest("노트북", "electronics", BigDecimal.valueOf(1200000));
        var product = new Product("노트북", category, BigDecimal.valueOf(1200000));

        when(categoryRepository.findById("electronics")).thenReturn(Optional.of(category));
        when(productRepository.save(any())).thenReturn(product);

        var result = productService.create(req);

        assertThat(result.getName()).isEqualTo("노트북");
        assertThat(result.getCategory().getId()).isEqualTo("electronics");
    }

    @Test
    void create_존재하지_않는_카테고리_예외() {
        var req = new ProductRequest("노트북", "unknown", BigDecimal.valueOf(1000));
        when(categoryRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(req))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("unknown");
    }

    @Test
    void findById_없는_상품_예외() {
        when(productRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById("bad-id"))
            .isInstanceOf(BusinessException.class);
    }
}
```

- [ ] **Step 6: 테스트 실행**

```bash
mvn test -pl api-server -Dtest=ProductServiceTest -q
```

Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 7: Commit**

```bash
git add api-server/src/
git commit -m "feat: add product/user CRUD with unified exception handling"
```

---

## Phase 2: 이벤트 파이프라인 (Week 2)

### Task 6: API Server — Kafka Producer + Event API

**Files:**
- Create: `api-server/src/main/java/com/commerce/api/config/KafkaProducerConfig.java`
- Create: `api-server/src/main/java/com/commerce/api/config/RedisConfig.java`
- Create: `api-server/src/main/java/com/commerce/api/kafka/EventProducer.java`
- Create: `api-server/src/main/java/com/commerce/api/dto/EventRequest.java`
- Create: `api-server/src/main/java/com/commerce/api/dto/EventResponse.java`
- Create: `api-server/src/main/java/com/commerce/api/service/EventService.java`
- Create: `api-server/src/main/java/com/commerce/api/controller/EventController.java`
- Test: `api-server/src/test/java/com/commerce/api/service/EventServiceTest.java`
- Test: `api-server/src/test/java/com/commerce/api/controller/EventControllerTest.java`

- [ ] **Step 1: KafkaProducerConfig.java 작성**

```java
// api-server/src/main/java/com/commerce/api/config/KafkaProducerConfig.java
package com.commerce.api.config;

import com.commerce.common.event.UserBehaviorEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, UserBehaviorEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, UserBehaviorEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

- [ ] **Step 2: RedisConfig.java 작성 (api-server)**

```java
// api-server/src/main/java/com/commerce/api/config/RedisConfig.java
package com.commerce.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
```

- [ ] **Step 3: EventProducer.java 작성**

```java
// api-server/src/main/java/com/commerce/api/kafka/EventProducer.java
package com.commerce.api.kafka;

import com.commerce.api.exception.BusinessException;
import com.commerce.api.exception.ErrorCode;
import com.commerce.common.event.UserBehaviorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {

    private static final Logger log = LoggerFactory.getLogger(EventProducer.class);

    private final KafkaTemplate<String, UserBehaviorEvent> kafkaTemplate;

    @Value("${kafka.topic.user-events}")
    private String topic;

    public EventProducer(KafkaTemplate<String, UserBehaviorEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(UserBehaviorEvent event) {
        kafkaTemplate.send(topic, event.userId(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send event: {}, error: {}", event.eventId(), ex.getMessage());
                    throw new BusinessException(ErrorCode.EVENT_PUBLISH_FAILED, event.eventId());
                }
                log.debug("Event sent: {}", event.eventId());
            });
    }
}
```

- [ ] **Step 4: EventRequest + EventResponse DTO 작성**

```java
// api-server/src/main/java/com/commerce/api/dto/EventRequest.java
package com.commerce.api.dto;

import com.commerce.common.event.EventContext;
import com.commerce.common.event.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EventRequest(
    @NotBlank String userId,
    @NotBlank String productId,
    @NotNull EventType eventType,
    String sessionId,
    EventContext context
) {}
```

```java
// api-server/src/main/java/com/commerce/api/dto/EventResponse.java
package com.commerce.api.dto;

public record EventResponse(String eventId, String status) {}
```

- [ ] **Step 5: EventService.java 작성**

```java
// api-server/src/main/java/com/commerce/api/service/EventService.java
package com.commerce.api.service;

import com.commerce.api.dto.EventRequest;
import com.commerce.api.dto.EventResponse;
import com.commerce.api.kafka.EventProducer;
import com.commerce.common.event.UserBehaviorEvent;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

@Service
public class EventService {

    private final EventProducer eventProducer;

    public EventService(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public EventResponse publish(EventRequest req) {
        String eventId = UUID.randomUUID().toString();
        var event = new UserBehaviorEvent(
            eventId, "1.0",
            req.userId(), req.productId(), req.eventType(),
            req.sessionId(), req.context(), Instant.now()
        );
        eventProducer.send(event);
        return new EventResponse(eventId, "ACCEPTED");
    }
}
```

- [ ] **Step 6: EventController.java 작성**

```java
// api-server/src/main/java/com/commerce/api/controller/EventController.java
package com.commerce.api.controller;

import com.commerce.api.dto.EventRequest;
import com.commerce.api.dto.EventResponse;
import com.commerce.api.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EventResponse publish(@Valid @RequestBody EventRequest req) {
        return eventService.publish(req);
    }
}
```

- [ ] **Step 7: EventService 단위 테스트 작성**

```java
// api-server/src/test/java/com/commerce/api/service/EventServiceTest.java
package com.commerce.api.service;

import com.commerce.api.dto.EventRequest;
import com.commerce.api.kafka.EventProducer;
import com.commerce.common.event.EventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import com.commerce.common.event.UserBehaviorEvent;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EventProducer eventProducer;
    @InjectMocks EventService eventService;

    @Test
    void publish_eventId_반환() {
        var req = new EventRequest("user-1", "product-1", EventType.VIEW, "session-1", null);
        doNothing().when(eventProducer).send(any());

        var response = eventService.publish(req);

        assertThat(response.eventId()).isNotBlank();
        assertThat(response.status()).isEqualTo("ACCEPTED");
    }

    @Test
    void publish_이벤트_내용_검증() {
        var req = new EventRequest("user-1", "product-1", EventType.PURCHASE, "session-1", null);
        var captor = ArgumentCaptor.forClass(UserBehaviorEvent.class);
        doNothing().when(eventProducer).send(captor.capture());

        eventService.publish(req);

        var captured = captor.getValue();
        assertThat(captured.userId()).isEqualTo("user-1");
        assertThat(captured.productId()).isEqualTo("product-1");
        assertThat(captured.eventType()).isEqualTo(EventType.PURCHASE);
        assertThat(captured.schemaVersion()).isEqualTo("1.0");
    }
}
```

- [ ] **Step 8: 테스트 실행**

```bash
mvn test -pl api-server -Dtest=EventServiceTest -q
```

Expected: `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 9: Commit**

```bash
git add api-server/src/
git commit -m "feat: add event collection API with kafka producer (POST /api/v1/events)"
```

---

### Task 7: Event Processor — 기본 구조 + Idempotency

**Files:**
- Create: `event-processor/src/main/java/com/commerce/processor/EventProcessorApplication.java`
- Create: `event-processor/src/main/java/com/commerce/processor/config/KafkaConsumerConfig.java`
- Create: `event-processor/src/main/java/com/commerce/processor/config/RedisConfig.java`
- Create: `event-processor/src/main/java/com/commerce/processor/config/CacheConfig.java`
- Create: `event-processor/src/main/java/com/commerce/processor/idempotency/IdempotencyService.java`
- Create: `event-processor/src/main/resources/application.yml`
- Test: `event-processor/src/test/java/com/commerce/processor/idempotency/IdempotencyServiceTest.java`

- [ ] **Step 1: EventProcessorApplication.java 작성**

```java
// event-processor/src/main/java/com/commerce/processor/EventProcessorApplication.java
package com.commerce.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class EventProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventProcessorApplication.class, args);
    }
}
```

- [ ] **Step 2: application.yml 작성 (event-processor)**

```yaml
# event-processor/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/commerce?useSSL=false&allowPublicKeyRetrieval=true
    username: commerce
    password: commerce123
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  kafka:
    bootstrap-servers: localhost:9092
  data:
    redis:
      host: localhost
      port: 6379

server:
  port: 8081

kafka:
  topic:
    user-events: user-events
    dlq: user-events-dlq
```

- [ ] **Step 3: KafkaConsumerConfig.java 작성**

```java
// event-processor/src/main/java/com/commerce/processor/config/KafkaConsumerConfig.java
package com.commerce.processor.config;

import com.commerce.common.event.UserBehaviorEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, UserBehaviorEvent> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "event-processor-group");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(config,
            new StringDeserializer(),
            new JsonDeserializer<>(UserBehaviorEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserBehaviorEvent> kafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, UserBehaviorEvent>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    // DLQ용 KafkaTemplate
    @Bean
    public KafkaTemplate<String, UserBehaviorEvent> dlqKafkaTemplate() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config));
    }
}
```

- [ ] **Step 4: RedisConfig.java 작성 (event-processor)**

```java
// event-processor/src/main/java/com/commerce/processor/config/RedisConfig.java
package com.commerce.processor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
```

- [ ] **Step 5: CacheConfig.java 작성**

```java
// event-processor/src/main/java/com/commerce/processor/config/CacheConfig.java
package com.commerce.processor.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("productCategory");
        manager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
        );
        return manager;
    }
}
```

- [ ] **Step 6: IdempotencyService 테스트 먼저 작성**

```java
// event-processor/src/test/java/com/commerce/processor/idempotency/IdempotencyServiceTest.java
package com.commerce.processor.idempotency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @InjectMocks IdempotencyService idempotencyService;

    @Test
    void 신규_이벤트_처리_허용() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("processed:event:evt-1"), eq("1"), any())).thenReturn(true);

        assertThat(idempotencyService.tryMarkProcessed("evt-1")).isTrue();
    }

    @Test
    void 중복_이벤트_거부() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("processed:event:evt-1"), eq("1"), any())).thenReturn(false);

        assertThat(idempotencyService.tryMarkProcessed("evt-1")).isFalse();
    }

    @Test
    void unmark_삭제_호출() {
        idempotencyService.unmarkProcessed("evt-1");
        verify(redisTemplate).delete("processed:event:evt-1");
    }
}
```

- [ ] **Step 7: 테스트 실행 — 실패 확인**

```bash
mvn test -pl event-processor -Dtest=IdempotencyServiceTest -q 2>&1 | tail -5
```

Expected: `COMPILATION ERROR` 또는 `ClassNotFoundException` (IdempotencyService 미존재)

- [ ] **Step 8: IdempotencyService.java 구현**

```java
// event-processor/src/main/java/com/commerce/processor/idempotency/IdempotencyService.java
package com.commerce.processor.idempotency;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class IdempotencyService {

    private static final String KEY_PREFIX = "processed:event:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryMarkProcessed(String eventId) {
        Boolean result = redisTemplate.opsForValue()
            .setIfAbsent(KEY_PREFIX + eventId, "1", TTL);
        return Boolean.TRUE.equals(result);
    }

    public void unmarkProcessed(String eventId) {
        redisTemplate.delete(KEY_PREFIX + eventId);
    }
}
```

- [ ] **Step 9: 테스트 재실행 — 통과 확인**

```bash
mvn test -pl event-processor -Dtest=IdempotencyServiceTest -q
```

Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 10: Commit**

```bash
git add event-processor/src/
git commit -m "feat: add event-processor base structure with idempotency service"
```

---

### Task 8: Event Processor — Ranking + Recommendation + Consumer

**Files:**
- Create: `event-processor/src/main/java/com/commerce/processor/repository/ProductCategoryRepository.java`
- Create: `event-processor/src/main/java/com/commerce/processor/ranking/RankingService.java`
- Create: `event-processor/src/main/java/com/commerce/processor/recommendation/RecommendationService.java`
- Create: `event-processor/src/main/java/com/commerce/processor/kafka/DlqProducer.java`
- Create: `event-processor/src/main/java/com/commerce/processor/kafka/EventConsumer.java`
- Test: `event-processor/src/test/java/com/commerce/processor/ranking/RankingServiceTest.java`
- Test: `event-processor/src/test/java/com/commerce/processor/kafka/EventConsumerTest.java`

- [ ] **Step 1: ProductCategoryRepository 작성 (read-only)**

```java
// event-processor/src/main/java/com/commerce/processor/repository/ProductCategoryRepository.java
package com.commerce.processor.repository;

import jakarta.persistence.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class ProductCategoryRepository {

    @PersistenceContext
    private EntityManager em;

    @Cacheable("productCategory")
    public String getCategoryId(String productId) {
        String jpql = "SELECT p.categoryId FROM ProductProjection p WHERE p.id = :id";
        try {
            return em.createQuery(jpql, String.class)
                .setParameter("id", productId)
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
```

> **주의:** event-processor는 api-server와 같은 MySQL을 공유하지만 JPA 엔티티를 별도로 관리한다.
> `Product` 엔티티를 공유 모듈에 두는 대신, 최소한의 projection용 엔티티를 직접 정의한다.

- [ ] **Step 2: ProductProjection 엔티티 추가**

```java
// event-processor/src/main/java/com/commerce/processor/repository/ProductProjection.java
package com.commerce.processor.repository;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class ProductProjection {

    @Id
    private String id;

    @Column(name = "category_id")
    private String categoryId;

    protected ProductProjection() {}

    public String getId() { return id; }
    public String getCategoryId() { return categoryId; }
}
```

- [ ] **Step 3: RankingService 테스트 먼저 작성**

```java
// event-processor/src/test/java/com/commerce/processor/ranking/RankingServiceTest.java
package com.commerce.processor.ranking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ZSetOperations<String, String> zSetOps;
    @InjectMocks RankingService rankingService;

    @Test
    void incrementScore_세_개_키에_점수_추가() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);

        rankingService.incrementScore("product-1", "electronics", 10);

        verify(zSetOps).incrementScore("ranking:trending:1h", "product-1", 10.0);
        verify(zSetOps).incrementScore("ranking:trending:24h", "product-1", 10.0);
        verify(zSetOps).incrementScore("ranking:trending:category:electronics:1h", "product-1", 10.0);
    }

    @Test
    void getCategoryScore_Redis_점수_반환() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.score("ranking:trending:category:electronics:1h", "product-1")).thenReturn(42.0);

        double score = rankingService.getCategoryScore("product-1", "electronics");

        assertThat(score).isEqualTo(42.0);
    }
}
```

(import 추가 필요: `import static org.assertj.core.api.Assertions.assertThat;`)

- [ ] **Step 4: 테스트 실패 확인 후 RankingService 구현**

```java
// event-processor/src/main/java/com/commerce/processor/ranking/RankingService.java
package com.commerce.processor.ranking;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RankingService {

    private static final String GLOBAL_PREFIX = "ranking:trending:";
    private static final String CATEGORY_PREFIX = "ranking:trending:category:";

    private final StringRedisTemplate redisTemplate;

    public RankingService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void incrementScore(String productId, String categoryId, int weight) {
        redisTemplate.opsForZSet().incrementScore(GLOBAL_PREFIX + "1h", productId, weight);
        redisTemplate.opsForZSet().incrementScore(GLOBAL_PREFIX + "24h", productId, weight);
        if (categoryId != null) {
            redisTemplate.opsForZSet().incrementScore(CATEGORY_PREFIX + categoryId + ":1h", productId, weight);
        }
    }

    public double getCategoryScore(String productId, String categoryId) {
        if (categoryId == null) return 0.0;
        Double score = redisTemplate.opsForZSet()
            .score(CATEGORY_PREFIX + categoryId + ":1h", productId);
        return score != null ? score : 0.0;
    }
}
```

- [ ] **Step 5: RankingService 테스트 실행**

```bash
mvn test -pl event-processor -Dtest=RankingServiceTest -q
```

Expected: `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 6: RecommendationService 작성**

```java
// event-processor/src/main/java/com/commerce/processor/recommendation/RecommendationService.java
package com.commerce.processor.recommendation;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class RecommendationService {

    private static final String USER_PREFIX = "recommendation:user:";
    private static final String CATEGORY_PREFIX = "recommendation:category:";
    private static final Duration USER_TTL = Duration.ofHours(1);
    private static final Duration CATEGORY_TTL = Duration.ofHours(6);

    private final StringRedisTemplate redisTemplate;

    public RecommendationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateUserRecommendation(String userId, String productId, double score) {
        String key = USER_PREFIX + userId;
        redisTemplate.opsForZSet().add(key, productId, score);
        redisTemplate.expire(key, USER_TTL);
    }

    public void updateCategoryRecommendation(String categoryId, String productId, double score) {
        String key = CATEGORY_PREFIX + categoryId;
        redisTemplate.opsForZSet().add(key, productId, score);
        redisTemplate.expire(key, CATEGORY_TTL);
    }
}
```

- [ ] **Step 7: DlqProducer 작성**

```java
// event-processor/src/main/java/com/commerce/processor/kafka/DlqProducer.java
package com.commerce.processor.kafka;

import com.commerce.common.event.UserBehaviorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DlqProducer {

    private static final Logger log = LoggerFactory.getLogger(DlqProducer.class);

    private final KafkaTemplate<String, UserBehaviorEvent> kafkaTemplate;

    @Value("${kafka.topic.dlq}")
    private String dlqTopic;

    public DlqProducer(KafkaTemplate<String, UserBehaviorEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(UserBehaviorEvent event) {
        log.error("Sending event to DLQ: {}", event.eventId());
        kafkaTemplate.send(dlqTopic, event.userId(), event);
    }
}
```

- [ ] **Step 8: EventConsumer 테스트 먼저 작성**

```java
// event-processor/src/test/java/com/commerce/processor/kafka/EventConsumerTest.java
package com.commerce.processor.kafka;

import com.commerce.common.event.*;
import com.commerce.processor.idempotency.IdempotencyService;
import com.commerce.processor.ranking.RankingService;
import com.commerce.processor.recommendation.RecommendationService;
import com.commerce.processor.repository.ProductCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventConsumerTest {

    @Mock IdempotencyService idempotencyService;
    @Mock ProductCategoryRepository productCategoryRepository;
    @Mock RankingService rankingService;
    @Mock RecommendationService recommendationService;
    @Mock DlqProducer dlqProducer;
    @InjectMocks EventConsumer eventConsumer;

    private UserBehaviorEvent viewEvent() {
        return new UserBehaviorEvent("evt-1", "1.0", "user-1", "product-1",
            EventType.VIEW, "session-1", null, Instant.now());
    }

    private UserBehaviorEvent purchaseEvent() {
        return new UserBehaviorEvent("evt-2", "1.0", "user-1", "product-1",
            EventType.PURCHASE, "session-1", null, Instant.now());
    }

    @Test
    void VIEW_이벤트_랭킹_점수_증가() {
        var event = viewEvent();
        when(idempotencyService.tryMarkProcessed("evt-1")).thenReturn(true);
        when(productCategoryRepository.getCategoryId("product-1")).thenReturn("electronics");

        eventConsumer.consume(event);

        verify(rankingService).incrementScore("product-1", "electronics", 1);
        verify(recommendationService, never()).updateUserRecommendation(any(), any(), anyDouble());
    }

    @Test
    void PURCHASE_이벤트_추천도_업데이트() {
        var event = purchaseEvent();
        when(idempotencyService.tryMarkProcessed("evt-2")).thenReturn(true);
        when(productCategoryRepository.getCategoryId("product-1")).thenReturn("electronics");
        when(rankingService.getCategoryScore("product-1", "electronics")).thenReturn(15.0);

        eventConsumer.consume(event);

        verify(rankingService).incrementScore("product-1", "electronics", 10);
        verify(recommendationService).updateUserRecommendation("user-1", "product-1", 15.0);
        verify(recommendationService).updateCategoryRecommendation("electronics", "product-1", 15.0);
    }

    @Test
    void 중복_이벤트_무시() {
        var event = viewEvent();
        when(idempotencyService.tryMarkProcessed("evt-1")).thenReturn(false);

        eventConsumer.consume(event);

        verifyNoInteractions(rankingService, recommendationService, productCategoryRepository);
    }

    @Test
    void 처리_실패시_DLQ_전송() {
        var event = viewEvent();
        when(idempotencyService.tryMarkProcessed("evt-1")).thenReturn(true);
        when(productCategoryRepository.getCategoryId("product-1")).thenThrow(new RuntimeException("DB error"));

        eventConsumer.consume(event);

        verify(idempotencyService).unmarkProcessed("evt-1");
        verify(dlqProducer).send(event);
    }
}
```

- [ ] **Step 9: 테스트 실패 확인 후 EventConsumer 구현**

```java
// event-processor/src/main/java/com/commerce/processor/kafka/EventConsumer.java
package com.commerce.processor.kafka;

import com.commerce.common.event.EventType;
import com.commerce.common.event.UserBehaviorEvent;
import com.commerce.processor.idempotency.IdempotencyService;
import com.commerce.processor.ranking.RankingService;
import com.commerce.processor.recommendation.RecommendationService;
import com.commerce.processor.repository.ProductCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    private final IdempotencyService idempotencyService;
    private final ProductCategoryRepository productCategoryRepository;
    private final RankingService rankingService;
    private final RecommendationService recommendationService;
    private final DlqProducer dlqProducer;

    public EventConsumer(IdempotencyService idempotencyService,
                         ProductCategoryRepository productCategoryRepository,
                         RankingService rankingService,
                         RecommendationService recommendationService,
                         DlqProducer dlqProducer) {
        this.idempotencyService = idempotencyService;
        this.productCategoryRepository = productCategoryRepository;
        this.rankingService = rankingService;
        this.recommendationService = recommendationService;
        this.dlqProducer = dlqProducer;
    }

    @KafkaListener(topics = "${kafka.topic.user-events}", groupId = "event-processor-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consume(UserBehaviorEvent event) {
        if (!idempotencyService.tryMarkProcessed(event.eventId())) {
            log.debug("Duplicate event skipped: {}", event.eventId());
            return;
        }
        try {
            process(event);
        } catch (Exception e) {
            log.error("Failed to process event: {}, error: {}", event.eventId(), e.getMessage(), e);
            idempotencyService.unmarkProcessed(event.eventId());
            dlqProducer.send(event);
        }
    }

    private void process(UserBehaviorEvent event) {
        String categoryId = productCategoryRepository.getCategoryId(event.productId());
        rankingService.incrementScore(event.productId(), categoryId, event.eventType().weight());

        if (event.eventType() == EventType.PURCHASE) {
            double score = rankingService.getCategoryScore(event.productId(), categoryId);
            recommendationService.updateUserRecommendation(event.userId(), event.productId(), score);
            if (categoryId != null) {
                recommendationService.updateCategoryRecommendation(categoryId, event.productId(), score);
            }
        }
    }
}
```

- [ ] **Step 10: 전체 테스트 실행**

```bash
mvn test -pl event-processor -q
```

Expected: `Tests run: 7, Failures: 0, Errors: 0`

- [ ] **Step 11: Commit**

```bash
git add event-processor/src/
git commit -m "feat: implement event consumer with ranking, recommendation, DLQ, and idempotency"
```

---

## Phase 3: 랭킹 + 추천 조회 API (Week 3)

### Task 9: API Server — 랭킹 조회 API

**Files:**
- Create: `api-server/src/main/java/com/commerce/api/dto/RankingResponse.java`
- Create: `api-server/src/main/java/com/commerce/api/service/RankingService.java`
- Create: `api-server/src/main/java/com/commerce/api/controller/RankingController.java`
- Test: `api-server/src/test/java/com/commerce/api/service/RankingServiceTest.java`
- Test: `api-server/src/test/java/com/commerce/api/controller/RankingControllerTest.java`

- [ ] **Step 1: RankingResponse DTO 작성**

```java
// api-server/src/main/java/com/commerce/api/dto/RankingResponse.java
package com.commerce.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RankingResponse(
    String window,
    Instant generatedAt,
    List<RankingItem> products
) {
    public record RankingItem(
        int rank,
        String productId,
        String name,
        String categoryId,
        BigDecimal price,
        Double score
    ) {}
}
```

- [ ] **Step 2: RankingService 테스트 먼저 작성**

```java
// api-server/src/test/java/com/commerce/api/service/RankingServiceTest.java
package com.commerce.api.service;

import com.commerce.api.domain.Category;
import com.commerce.api.domain.Product;
import com.commerce.api.domain.ProductStatus;
import com.commerce.api.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ZSetOperations<String, String> zSetOps;
    @Mock ProductRepository productRepository;
    @InjectMocks RankingService rankingService;

    @Test
    void trending_조회_상품정보_포함() {
        var category = new Category("electronics", "전자제품");
        var product = new Product("노트북", category, BigDecimal.valueOf(1200000));

        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        tuples.add(ZSetOperations.TypedTuple.of(product.getId(), 42.0));

        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.reverseRangeWithScores("ranking:trending:1h", 0, 19))
            .thenReturn(tuples);
        when(productRepository.findAllByIdInAndStatus(List.of(product.getId()), ProductStatus.ACTIVE))
            .thenReturn(List.of(product));

        var response = rankingService.getTrending("1h", 20);

        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).rank()).isEqualTo(1);
        assertThat(response.products().get(0).score()).isEqualTo(42.0);
    }

    @Test
    void trending_Redis_비어있으면_빈_목록() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.reverseRangeWithScores(any(), anyLong(), anyLong())).thenReturn(Set.of());

        var response = rankingService.getTrending("1h", 20);

        assertThat(response.products()).isEmpty();
    }
}
```

- [ ] **Step 3: RankingService 구현**

```java
// api-server/src/main/java/com/commerce/api/service/RankingService.java
package com.commerce.api.service;

import com.commerce.api.domain.ProductStatus;
import com.commerce.api.dto.RankingResponse;
import com.commerce.api.dto.RankingResponse.RankingItem;
import com.commerce.api.repository.ProductRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RankingService {

    private static final String GLOBAL_PREFIX = "ranking:trending:";
    private static final String CATEGORY_PREFIX = "ranking:trending:category:";

    private final StringRedisTemplate redisTemplate;
    private final ProductRepository productRepository;

    public RankingService(StringRedisTemplate redisTemplate, ProductRepository productRepository) {
        this.redisTemplate = redisTemplate;
        this.productRepository = productRepository;
    }

    public RankingResponse getTrending(String window, int limit) {
        return getByKey(GLOBAL_PREFIX + window, window, limit);
    }

    public RankingResponse getTrendingByCategory(String categoryId, String window, int limit) {
        return getByKey(CATEGORY_PREFIX + categoryId + ":" + window, window, limit);
    }

    private RankingResponse getByKey(String key, String window, int limit) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
            redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);

        if (tuples == null || tuples.isEmpty()) {
            return new RankingResponse(window, Instant.now(), List.of());
        }

        List<String> productIds = tuples.stream()
            .map(ZSetOperations.TypedTuple::getValue)
            .filter(Objects::nonNull)
            .toList();

        var productMap = productRepository
            .findAllByIdInAndStatus(productIds, ProductStatus.ACTIVE)
            .stream()
            .collect(Collectors.toMap(p -> p.getId(), Function.identity()));

        List<RankingItem> items = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String productId = tuple.getValue();
            var product = productMap.get(productId);
            if (product != null) {
                items.add(new RankingItem(rank++, productId, product.getName(),
                    product.getCategory().getId(), product.getPrice(), tuple.getScore()));
            }
        }
        return new RankingResponse(window, Instant.now(), items);
    }
}
```

- [ ] **Step 4: RankingController 작성**

```java
// api-server/src/main/java/com/commerce/api/controller/RankingController.java
package com.commerce.api.controller;

import com.commerce.api.dto.RankingResponse;
import com.commerce.api.service.RankingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rankings")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/trending")
    public RankingResponse getTrending(
            @RequestParam(defaultValue = "1h") String window,
            @RequestParam(defaultValue = "20") int limit) {
        return rankingService.getTrending(window, limit);
    }

    @GetMapping("/trending/categories/{categoryId}")
    public RankingResponse getTrendingByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "1h") String window,
            @RequestParam(defaultValue = "20") int limit) {
        return rankingService.getTrendingByCategory(categoryId, window, limit);
    }
}
```

- [ ] **Step 5: 테스트 실행**

```bash
mvn test -pl api-server -Dtest=RankingServiceTest -q
```

Expected: `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```bash
git add api-server/src/
git commit -m "feat: add trending ranking API (GET /api/v1/rankings/trending)"
```

---

### Task 10: API Server — 추천 조회 API

**Files:**
- Create: `api-server/src/main/java/com/commerce/api/dto/RecommendationResponse.java`
- Create: `api-server/src/main/java/com/commerce/api/service/RecommendationService.java`
- Create: `api-server/src/main/java/com/commerce/api/controller/RecommendationController.java`
- Test: `api-server/src/test/java/com/commerce/api/service/RecommendationServiceTest.java`

- [ ] **Step 1: RecommendationResponse DTO 작성**

```java
// api-server/src/main/java/com/commerce/api/dto/RecommendationResponse.java
package com.commerce.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecommendationResponse(
    String userId,
    List<RecommendationItem> products
) {
    public record RecommendationItem(
        String productId,
        String name,
        String categoryId,
        BigDecimal price,
        String reason
    ) {}
}
```

- [ ] **Step 2: RecommendationService 테스트 먼저 작성**

```java
// api-server/src/test/java/com/commerce/api/service/RecommendationServiceTest.java
package com.commerce.api.service;

import com.commerce.api.domain.Category;
import com.commerce.api.domain.Product;
import com.commerce.api.domain.ProductStatus;
import com.commerce.api.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ZSetOperations<String, String> zSetOps;
    @Mock ProductRepository productRepository;
    @InjectMocks RecommendationService recommendationService;

    @Test
    void 유저_추천_반환() {
        var category = new Category("sports", "스포츠");
        var product = new Product("운동화", category, BigDecimal.valueOf(89000));

        Set<String> productIds = new LinkedHashSet<>(List.of(product.getId()));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.reverseRange("recommendation:user:user-1", 0, 9)).thenReturn(productIds);
        when(productRepository.findAllByIdInAndStatus(List.of(product.getId()), ProductStatus.ACTIVE))
            .thenReturn(List.of(product));

        var response = recommendationService.getUserRecommendations("user-1", 10);

        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).reason()).isEqualTo("RECENT_PURCHASE_CATEGORY");
    }

    @Test
    void 유저_추천_없으면_빈_목록() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.reverseRange("recommendation:user:user-1", 0, 9)).thenReturn(Set.of());

        var response = recommendationService.getUserRecommendations("user-1", 10);

        assertThat(response.products()).isEmpty();
    }

    @Test
    void 카테고리_추천_반환() {
        var category = new Category("sports", "스포츠");
        var product = new Product("농구공", category, BigDecimal.valueOf(45000));

        Set<String> productIds = new LinkedHashSet<>(List.of(product.getId()));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.reverseRange("recommendation:category:sports", 0, 9)).thenReturn(productIds);
        when(productRepository.findAllByIdInAndStatus(List.of(product.getId()), ProductStatus.ACTIVE))
            .thenReturn(List.of(product));

        var response = recommendationService.getCategoryRecommendations("sports", 10);

        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).reason()).isEqualTo("CATEGORY_POPULAR");
    }
}
```

- [ ] **Step 3: RecommendationService 구현**

```java
// api-server/src/main/java/com/commerce/api/service/RecommendationService.java
package com.commerce.api.service;

import com.commerce.api.domain.ProductStatus;
import com.commerce.api.dto.RecommendationResponse;
import com.commerce.api.dto.RecommendationResponse.RecommendationItem;
import com.commerce.api.repository.ProductRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final String USER_PREFIX = "recommendation:user:";
    private static final String CATEGORY_PREFIX = "recommendation:category:";

    private final StringRedisTemplate redisTemplate;
    private final ProductRepository productRepository;

    public RecommendationService(StringRedisTemplate redisTemplate, ProductRepository productRepository) {
        this.redisTemplate = redisTemplate;
        this.productRepository = productRepository;
    }

    public RecommendationResponse getUserRecommendations(String userId, int limit) {
        Set<String> productIds = redisTemplate.opsForZSet()
            .reverseRange(USER_PREFIX + userId, 0, limit - 1);
        return buildResponse(userId, productIds, "RECENT_PURCHASE_CATEGORY");
    }

    public RecommendationResponse getCategoryRecommendations(String categoryId, int limit) {
        Set<String> productIds = redisTemplate.opsForZSet()
            .reverseRange(CATEGORY_PREFIX + categoryId, 0, limit - 1);
        return buildResponse(null, productIds, "CATEGORY_POPULAR");
    }

    private RecommendationResponse buildResponse(String userId, Set<String> productIds, String reason) {
        if (productIds == null || productIds.isEmpty()) {
            return new RecommendationResponse(userId, List.of());
        }
        var productMap = productRepository
            .findAllByIdInAndStatus(new ArrayList<>(productIds), ProductStatus.ACTIVE)
            .stream()
            .collect(Collectors.toMap(p -> p.getId(), Function.identity()));

        List<RecommendationItem> items = productIds.stream()
            .map(id -> {
                var product = productMap.get(id);
                if (product == null) return null;
                return new RecommendationItem(id, product.getName(),
                    product.getCategory().getId(), product.getPrice(), reason);
            })
            .filter(Objects::nonNull)
            .toList();

        return new RecommendationResponse(userId, items);
    }
}
```

- [ ] **Step 4: RecommendationController 작성**

```java
// api-server/src/main/java/com/commerce/api/controller/RecommendationController.java
package com.commerce.api.controller;

import com.commerce.api.dto.RecommendationResponse;
import com.commerce.api.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/users/{userId}")
    public RecommendationResponse getUserRecommendations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int limit) {
        return recommendationService.getUserRecommendations(userId, limit);
    }

    @GetMapping("/categories/{categoryId}")
    public RecommendationResponse getCategoryRecommendations(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "10") int limit) {
        return recommendationService.getCategoryRecommendations(categoryId, limit);
    }
}
```

- [ ] **Step 5: 테스트 실행**

```bash
mvn test -pl api-server -Dtest=RecommendationServiceTest -q
```

Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 6: 전체 모듈 테스트 통과 확인**

```bash
mvn test -q
```

Expected: 전체 테스트 `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add api-server/src/
git commit -m "feat: add recommendation API (GET /api/v1/recommendations)"
```

---

## Phase 4: 패키징 + 배포 준비 (Week 4)

### Task 11: Dockerfile 작성

**Files:**
- Create: `api-server/Dockerfile`
- Create: `event-processor/Dockerfile`

- [ ] **Step 1: api-server/Dockerfile 작성**

```dockerfile
# api-server/Dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/api-server-1.0.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: event-processor/Dockerfile 작성**

```dockerfile
# event-processor/Dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/event-processor-1.0.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 3: 빌드 + Docker 이미지 확인**

```bash
mvn clean package -q -DskipTests
docker build -t api-server:local api-server/
docker build -t event-processor:local event-processor/
docker images | grep -E "api-server|event-processor"
```

Expected: 두 이미지 목록 출력

- [ ] **Step 4: Commit**

```bash
git add api-server/Dockerfile event-processor/Dockerfile
git commit -m "feat: add Dockerfiles for api-server and event-processor"
```

---

### Task 12: Seed Data + 통합 검증

**Files:**
- Create: `api-server/src/main/resources/data.sql`

- [ ] **Step 1: data.sql 작성**

```sql
-- api-server/src/main/resources/data.sql
-- 카테고리 시드
INSERT IGNORE INTO categories (id, name, parent_id) VALUES
    ('electronics', '전자제품', NULL),
    ('sports', '스포츠', NULL),
    ('fashion', '패션', NULL),
    ('laptop', '노트북', 'electronics'),
    ('phone', '스마트폰', 'electronics'),
    ('running', '러닝용품', 'sports');

-- 상품 시드
INSERT IGNORE INTO products (id, name, category_id, price, status) VALUES
    ('prod-001', 'LG그램 17인치', 'laptop', 1890000, 'ACTIVE'),
    ('prod-002', '삼성 갤럭시 S24', 'phone', 1200000, 'ACTIVE'),
    ('prod-003', '나이키 페가수스 41', 'running', 169000, 'ACTIVE'),
    ('prod-004', '아디다스 울트라부스트', 'running', 189000, 'ACTIVE'),
    ('prod-005', 'MacBook Pro M3', 'laptop', 2890000, 'ACTIVE'),
    ('prod-006', '아이폰 15 Pro', 'phone', 1550000, 'ACTIVE');

-- 유저 시드
INSERT IGNORE INTO users (id, name, email) VALUES
    ('user-001', '김철수', 'kim@example.com'),
    ('user-002', '이영희', 'lee@example.com'),
    ('user-003', '박민준', 'park@example.com');
```

- [ ] **Step 2: application.yml에 data.sql 설정 추가**

`api-server/src/main/resources/application.yml`의 `spring.sql.init` 섹션 수정:

```yaml
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql
```

- [ ] **Step 3: Docker Compose로 로컬 전체 스택 실행 및 검증**

```bash
# .env 파일 생성
cp .env.example .env

# 인프라만 먼저 실행 (앱 제외)
docker-compose up -d kafka mysql redis

# MySQL 준비 대기 (약 20초)
sleep 20

# api-server 실행 (로컬에서 직접)
cd api-server && mvn spring-boot:run &
sleep 15

# 헬스체크 — 상품 목록 조회
curl -s http://localhost:8080/api/v1/products/prod-001 | python3 -m json.tool

# 이벤트 발행 테스트
curl -s -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-001","productId":"prod-001","eventType":"PURCHASE","sessionId":"test-session","context":{"page":"product_detail","position":null}}' \
  | python3 -m json.tool
```

Expected:
- 상품 조회: `{"productId":"prod-001","name":"LG그램 17인치",...}`
- 이벤트 발행: `{"eventId":"...","status":"ACCEPTED"}`

- [ ] **Step 4: Event Processor 실행 및 랭킹 데이터 확인**

```bash
# event-processor 실행
cd event-processor && mvn spring-boot:run &
sleep 10

# 이벤트 추가 발행 (10개)
for i in {1..10}; do
  curl -s -X POST http://localhost:8080/api/v1/events \
    -H "Content-Type: application/json" \
    -d "{\"userId\":\"user-00$((i%3+1))\",\"productId\":\"prod-00$((i%6+1))\",\"eventType\":\"CLICK\",\"sessionId\":\"s-$i\",\"context\":null}" > /dev/null
done

sleep 3

# 랭킹 조회
curl -s http://localhost:8080/api/v1/rankings/trending | python3 -m json.tool
```

Expected: 랭킹 응답에 상품 목록과 score 포함

- [ ] **Step 5: Commit**

```bash
git add api-server/src/main/resources/data.sql api-server/src/main/resources/application.yml
git commit -m "feat: add seed data and verify end-to-end event pipeline"
```

---

### Task 13: AWS 배포 준비

**Files:**
- Create: `docker-compose.prod.yml`

- [ ] **Step 1: docker-compose.prod.yml 작성**

```yaml
# docker-compose.prod.yml — AWS EC2에서 실행. RDS/ElastiCache는 외부 서비스 사용.
version: '3.8'

services:
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_LISTENERS: PLAINTEXT://kafka:29092,CONTROLLER://kafka:9093,PLAINTEXT_HOST://0.0.0.0:9092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://${EC2_PUBLIC_IP}:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
    restart: unless-stopped

  api-server:
    image: ${API_SERVER_IMAGE}
    ports:
      - "8080:8080"
    depends_on:
      - kafka
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://${RDS_ENDPOINT}:3306/commerce?useSSL=true
      SPRING_DATASOURCE_USERNAME: ${MYSQL_USER}
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      SPRING_DATA_REDIS_HOST: ${ELASTICACHE_ENDPOINT}
    restart: unless-stopped

  event-processor:
    image: ${EVENT_PROCESSOR_IMAGE}
    depends_on:
      - kafka
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://${RDS_ENDPOINT}:3306/commerce?useSSL=true
      SPRING_DATASOURCE_USERNAME: ${MYSQL_USER}
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      SPRING_DATA_REDIS_HOST: ${ELASTICACHE_ENDPOINT}
    restart: unless-stopped
```

> **AWS 배포 순서:**
> 1. RDS MySQL 생성 → schema.sql 실행 → data.sql 실행
> 2. ElastiCache Redis 생성
> 3. EC2 (t3.small) 생성 + Docker 설치
> 4. `mvn clean package -DskipTests` → Docker 이미지 빌드 → ECR 또는 직접 전송
> 5. `.env` 설정 후 `docker-compose -f docker-compose.prod.yml up -d`

- [ ] **Step 2: .env.example에 prod 변수 추가**

```bash
# 기존 항목 아래 추가
EC2_PUBLIC_IP=<your-ec2-ip>
RDS_ENDPOINT=<your-rds-endpoint>
ELASTICACHE_ENDPOINT=<your-elasticache-endpoint>
API_SERVER_IMAGE=api-server:latest
EVENT_PROCESSOR_IMAGE=event-processor:latest
```

- [ ] **Step 3: Commit**

```bash
git add docker-compose.prod.yml .env.example
git commit -m "feat: add production docker-compose for AWS deployment"
```

---

## Self-Review 체크리스트

스펙 섹션별 커버리지:

| 스펙 항목 | 구현 Task |
|---|---|
| Maven multi-module (common/api-server/event-processor) | Task 1 |
| Docker Compose (6 컨테이너) | Task 2 |
| UserBehaviorEvent 스키마 (eventId, schemaVersion 등) | Task 3 |
| 도메인 엔티티 (Category, Product, User) + schema.sql | Task 4 |
| ErrorCode + GlobalExceptionHandler + 통일 에러 포맷 | Task 5 |
| POST /api/v1/events → 202 + eventId 반환 | Task 6 |
| Kafka Producer (KafkaProducerConfig + EventProducer) | Task 6 |
| Kafka Consumer (KafkaConsumerConfig) | Task 7 |
| Idempotency (Redis SETNX + unmark on failure) | Task 7 |
| RankingService (ZINCRBY 3개 키) | Task 8 |
| RecommendationService (ZADD user + category) | Task 8 |
| DLQ (user-events-dlq + DlqProducer) | Task 8 |
| Caffeine cache (ProductCategoryRepository) | Task 7, 8 |
| GET /api/v1/rankings/trending (N+1 방지 IN 쿼리) | Task 9 |
| GET /api/v1/rankings/trending/categories/{id} | Task 9 |
| GET /api/v1/recommendations/users/{id} | Task 10 |
| GET /api/v1/recommendations/categories/{id} | Task 10 |
| reason 세분화 (RECENT_PURCHASE_CATEGORY, CATEGORY_POPULAR) | Task 10 |
| Dockerfile (api-server + event-processor) | Task 11 |
| Seed data | Task 12 |
| AWS prod docker-compose | Task 13 |
