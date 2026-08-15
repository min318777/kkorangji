# 꼬랑지 — 반려동물 테마 소셜 플랫폼

반려동물을 사랑하는 사람들을 위한 커뮤니티 플랫폼입니다.  
일상공유(자랑) 게시글, 실종 반려동물 신고, 댓글, 좋아요, 실시간 알림 기능을 제공합니다.

---
## 아키텍처

<img width="850" height="476" alt="Image" src="https://github.com/user-attachments/assets/d5088a3d-93fd-4446-a8ea-cd063a08adaf" />

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.4.5 |
| Database | MySQL + JPA/Hibernate + QueryDSL |
| Cache | Redis |
| 인증 | OAuth2 (Google) + JWT |
| 파일 스토리지 | AWS S3 (Presigned URL) |
| 실시간 알림 | SSE (Server-Sent Events) |
| 비동기 처리 | Spring ApplicationEvent + @Async |
| DB 마이그레이션 | Flyway |
| 모니터링 | Prometheus + Grafana + AWS CloudWatch |
| API 문서 | Springdoc OpenAPI (Swagger) |
| 컨테이너 | Docker + docker-compose |
| 성능 테스트 | k6 |

---

## 주요 기능

- **반려동물 자랑 게시글** — 반려동물 사진과 함께 게시글 작성, 좋아요, 댓글, 인기글
- **반려동물 실종 신고** — 실종 위치(위도/경도), 반려동물 특징 등록, 내 위치기반 게시글 조회
- **댓글 시스템** — 댓글 + 대댓글
- **좋아요** — 동시성 처리 (UniqueConstraint + DataIntegrityViolationException 예외 처리)
- **실시간 알림** — SSE 기반 댓글/좋아요 알림
- **검색** — Full-Text Search (ngram, 한글 지원) + LIKE 폴백
- **RBAC 권한 관리** — Role/Permission 기반 접근 제어
- **마이페이지** — 내 게시글, 댓글, 좋아요 목록 + 통계 캐싱

---

## 성능 최적화

### 목록 페이지 쿼리 개선
엔티티 전체 조회 시 불필요한 컬럼까지 SELECT되고 N+1 발생. QueryDSL Projection으로 필요한 컬럼만 SELECT하도록 개선

- Fetch Join으로 N:1 관계 한 번에 조회
- @BatchSize(100)으로 1:N 컬렉션 IN 쿼리로 묶음 처리
- Projection DTO로 필요한 컬럼만 SELECT

---

### 조회수 처리 방식 비교
매 조회마다 DB UPDATE 시 트래픽 몰릴 때 병목 발생. 4가지 방식을 구현하고 k6로 성능 비교

- v1: 더티체킹 — JPA 자동 UPDATE, 동시 요청 시 Lost Update 발생
- v2: 원자적 UPDATE — 쿼리 한 번으로 처리, 동시성 문제 없음
- v3: Redis INCR + 배치 동기화 — Redis에 증가분을 모아뒀다가 30초마다 DB 반영
- v4: 비관적 락 — DB 락으로 정확한 동시성 보장, 처리량 낮음

v3 채택. DB 부하를 Redis로 분산하고, 어뷰징 방지 락으로 중복 조회 차단. 배치 동기화로 DB Write 횟수 감소

---

### 인기글 캐시 스탬피드 방지
캐시 만료 순간 수백 개 요청이 동시에 DB 조회하는 문제를 3가지 방식으로 비교

- v1: 단순 캐시 — 만료 시 DB 부하 집중
- v2: Redis 분산 락 — 첫 요청만 DB 조회, 나머지는 대기
- v3: Cache Warming — 25초마다 선제 갱신해서 만료 자체를 제거

v3 채택. 인기글은 자주 바뀌지 않아 선제 갱신 비용이 낮고, 락 대기 없이 빠른 응답 달성

---

### JWT 권한 조회 방식 비교
매 요청마다 DB에서 권한 조회 시 API 응답시간 증가. 3가지 방식 구현 후 성능 비교

- v1: DB 조회 — 항상 최신, 매 요청마다 DB 부하
- v2: JWT claims 직접 추출 — DB 조회 없음, 권한 변경이 토큰 만료 전까지 미반영
- v3: Redis 캐시 — 미스 시 DB Fallback, 로그인 시 캐싱

v3 채택. 권한 변경 시 캐시 즉시 무효화 가능해 보안과 성능을 동시에 달성

---

### SSE 실시간 알림 안정화
단방향 텍스트 이벤트 스트리밍이므로 WebSocket 대비 구현 복잡도가 낮은 SSE 채택. 운영 중 세 가지 문제 확인 후 개선

**메모리 누수 문제**
비정상 종료된 연결의 Emitter가 해제되지 않아 힙 메모리 사용량이 계속 증가

- 주기적 Heartbeat 전송, 실패 시 Emitter 즉시 제거

**다중 인스턴스 알림 유실 문제**
알림을 생성한 인스턴스와 SSE 연결 중인 인스턴스가 다르면 알림이 전달되지 않음

- Redis Pub/Sub으로 알림 이벤트를 전체 인스턴스에 공유
- 각 인스턴스가 자신에게 연결된 클라이언트에게만 전달
- 다중 인스턴스 환경 구축 후 인스턴스 간 알림 전달 테스트에서 메시지 유실 없이 실시간 전송 확인

**네트워크 단절 시 알림 유실 문제**
SSE 연결이 끊긴 동안 발생한 알림은 클라이언트에 전달되지 않음. SSE 스펙의 Last-Event-ID만으로는 서버가 자동으로 복구해주지 않아 별도 복구 로직 설계 필요

- SSE 이벤트 전송 시 알림 ID를 이벤트 ID로 부여
- 재연결 시 Last-Event-ID를 기준으로 DB에서 미수신 알림 조회 후 재전송

---

### 마이페이지 통계 캐싱
내 게시글 수, 댓글 수 등 매번 COUNT 쿼리로 집계 시 비용 큼

- user:stats TTL 10분으로 캐싱
- 게시글/댓글 작성, 삭제 시 @CacheEvict로 즉시 무효화

---


## 실행 방법

### 사전 요구사항
- Docker & Docker Compose
- Google OAuth2 클라이언트 ID/Secret
- AWS S3 버킷 및 액세스 키

### 1. 레포지토리 클론
```bash
git clone https://github.com/min318777/meow.git
cd meow
```

### 2. 환경변수 파일 생성
프로젝트 루트에 `.env.local` 파일 생성 후 아래 키에 맞는 값 입력:

```
JWT_SECRET=
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
AWS_S3_BUCKET=
AWS_S3_BASE_URL=
AWS_CLOUDFRONT_DOMAIN=
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
GF_ADMIN_USER=
GF_ADMIN_PASSWORD=
```

### 3. 실행
```bash
docker-compose -f docker-compose.local.yml up -d
```

### 4. 접속
| 서비스 | URL |
|--------|-----|
| API Swagger | http://localhost:8080/swagger-ui.html |
| Grafana | http://localhost:3001 |
| Prometheus | http://localhost:9090 |

---

## Docker 서비스 구성

| 서비스 | 포트 | 용도 |
|--------|------|------|
| api | 8080 | Spring Boot API |
| mysql | 3307 | 데이터베이스 |
| redis | 6379 | 캐시 / 조회수 |
| prometheus | 9090 | 메트릭 수집 |
| grafana | 3001 | 메트릭 시각화 |

---

## API 문서

서버 실행 후 접속: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## RBAC 권한 구조

### 권한 목록 (10개)

| 권한 코드 | 설명 |
|-----------|------|
| `post:read` | 게시글 조회 |
| `post:create` | 게시글 작성 |
| `post:update` | 게시글 수정 |
| `post:delete` | 게시글 삭제 (타인 포함) |
| `comment:create` | 댓글 작성 |
| `comment:update` | 댓글 수정 |
| `comment:delete` | 댓글 삭제 (타인 포함) |
| `user:read` | 유저 목록/통계 조회 |
| `user:restrict` | 유저 계정 제재/복원 |
| `user:delete` | 유저 강제 탈퇴 |

### 역할별 권한 매핑 (4개 역할)

| 권한 | ROLE_USER | ROLE_VIEWER | ROLE_ADMIN | ROLE_RESTRICTED |
|------|:---------:|:-----------:|:----------:|:---------------:|
| post:read | ✓ | ✓ | ✓ | ✓ |
| post:create | ✓ | ✓ | ✓ | |
| post:update | ✓ | ✓ | ✓ | |
| post:delete | | ✓ | ✓ | |
| comment:create | ✓ | ✓ | ✓ | |
| comment:update | ✓ | ✓ | ✓ | |
| comment:delete | | ✓ | ✓ | |
| user:read | | ✓ | ✓ | |
| user:restrict | | | ✓ | |
| user:delete | | | ✓ | |
