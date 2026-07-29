# Meow — 반려동물 테마 소셜 플랫폼

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

### 조회수 Redis INCR + 배치 동기화
- 동일 사용자 10분 내 재조회 어뷰징 방지 (Redis SET NX)
- Redis INCR으로 원자적 증가, 30초마다 DB 배치 동기화
- Redis 장애 시 DB 직접 업데이트 Fallback

### 인기글 캐시 스탬피드 방지 (3가지 방식 측정)
- v1: 단순 캐시
- v2: Redis 분산 락 (SET NX)
- v3: Cache Warming (30초마다 선제 갱신)

### JWT 권한 조회 방식 (3가지 방식 측정)
- v1: 매 요청마다 DB 조회
- v2: JWT claims에서 직접 추출
- v3: Redis 캐시 조회 (캐시 미스 시 DB Fallback)

### N+1 문제 해결
- Fetch Join (N:1 관계)
- @BatchSize(100) (1:N 컬렉션)
- QueryDSL Projection (필요한 컬럼만 SELECT)

### 마이페이지 통계 캐싱
- user:stats 캐시 (TTL 10분)
- 게시글/댓글 작성, 삭제 시 @CacheEvict 무효화

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

| 역할 | 권한 |
|------|------|
| ROLE_USER | post:read, post:write, comment:write |
| ROLE_ADMIN | 모든 권한 (post:delete, comment:delete, user:manage 포함) |
| ROLE_RESTRICTED | post:read만 (신고된 사용자) |