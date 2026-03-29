# Commerce Recommendation System — Design Notes

## 1. Project Overview

### Why this project

데이터 엔지니어링 경험(Kafka, Redis 등)은 있었지만 사용자 요청을 직접 처리하고 비즈니스 문제를 해결하는 B2C 백엔드 경험이 부족하다고 느꼈다.

이 프로젝트는 다음을 목표로 시작했다:

- 이벤트 기반 아키텍처 이해
- 실시간 데이터 처리 경험
- “데이터 → 사용자 가치” 연결 경험

---

## 2. System Summary (한눈에 보기)

- API Server: 이벤트 수집 + 조회 API
- Event Processor: Kafka Consumer + 랭킹/추천 계산
- Kafka: 이벤트 브로커
- Redis: 랭킹/추천 (Serving Layer)
- MySQL: 마스터 데이터

👉 핵심: **비동기 이벤트 기반 + Redis 서빙 구조**

---

## 3. Key Design Decisions

### 3.1 서비스 분리 (API Server vs Event Processor)

**문제**
- 이벤트 처리 로직이 API에 붙으면 응답 지연 발생

**선택**
- Producer / Consumer 분리 (2개의 애플리케이션)

**이유**
- 응답 속도 보장
- 비동기 처리 구조 명확화

---

### 3.2 Redis vs MySQL 역할 분리

**문제**
- 모든 데이터를 DB에서 조회하면 성능 문제

**선택**
- Redis: 조회용 (랭킹/추천)
- MySQL: 정합성 데이터

**결과**
- 읽기 성능 확보 + 데이터 일관성 유지

---

### 3.3 Redis 자료구조 선택 (Sorted Set)

**후보**
- List / String(JSON) / Sorted Set

**선택 이유**
- 정렬 필요
- 중복 방지
- score 기반 업데이트 가능

---

### 3.4 Time Decay 적용 여부

**고민**
- 랭킹 품질 향상을 위해 필요

**문제**
- Redis는 시간에 따라 자동 감쇄 불가
- 구현 시 복잡도 증가

**결정**
- MVP: 단순 누산
- 확장: Time Decay

👉 핵심 트레이드오프 경험

---

### 3.5 Idempotency 처리

**문제**
- Kafka at-least-once → 중복 이벤트 발생

**해결**
- Redis SETNX 기반 중복 방지

---

### 3.6 캐시 전략 (Caffeine)

**문제**
- Event Processor → MySQL 조회 빈번

**선택**
- Redis 대신 Local Cache

**이유**
- 단일 인스턴스
- 네트워크 비용 제거

---

### 3.7 DLQ 도입 범위

**선택**
- DLQ topic + 로그까지만 구현

**이유**
- 운영 고려는 하되 과도한 구현 방지

---

## 4. What I Learned

### 기술적인 부분

- 이벤트 기반 아키텍처의 구조적 장점
- Redis를 “데이터 저장소”가 아니라 “서빙 레이어”로 사용하는 방식
- Kafka의 at-least-once와 idempotency 필요성

---

### 설계 관점

- “좋은 설계”는 기술이 아니라 **선택의 이유**
- 모든 문제를 완벽하게 해결하려고 하면 오버엔지니어링이 된다
- MVP에서는 **버리는 것도 설계**

---

## 5. Trade-offs Summary

| 항목 | 선택 | 포기한 것 |
|------|------|----------|
| Time Decay | 미적용 | 랭킹 정확도 |
| Kubernetes | 미사용 | 운영 확장성 |
| ML 추천 | 미구현 | 추천 품질 |
| DLQ 재처리 | 미구현 | 자동 복구 |

---

## 6. Interview Story

### 6.1 왜 이 프로젝트를 했는가

데이터 처리 경험은 있었지만  
“사용자에게 직접 가치를 제공하는 백엔드 시스템” 경험이 부족했다.

그래서 이벤트 기반으로 데이터를 처리하고  
실시간으로 사용자에게 결과를 제공하는 구조를 직접 설계하고자 했다.

---

### 6.2 가장 고민했던 설계

👉 Time Decay 적용 여부

- 이론적으로는 필요
- 하지만 Redis 구조상 자연스럽지 않음

결과:
- MVP에서는 제외
- 확장 포인트로 남김

👉 현실적인 설계 판단 경험

---

### 6.3 가장 아쉬운 점

- 실제 트래픽 환경 부재
- 랭킹 고도화(Time Decay) 미구현
- 멀티 인스턴스 환경 미검증

---

## 7. How to Improve

- Time Decay 스케줄링 도입
- 멀티 Consumer 환경 확장
- 추천 로직 고도화 (ML)
- 장애 대응 자동화 (DLQ retry)

---

## 8. One-line Summary

이 프로젝트는  
**“기술 구현”이 아니라 “설계와 선택의 경험”이었다.**
~
