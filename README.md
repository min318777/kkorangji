# Meow — 고양이 테마 소셜 플랫폼

고양이를 사랑하는 사람들을 위한 커뮤니티 플랫폼입니다.  
자랑 게시글, 실종 고양이 신고, 댓글, 좋아요, 실시간 알림 기능을 제공합니다.

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
| 모니터링 | Prometheus + Grafana |
| API 문서 | Springdoc OpenAPI (Swagger) |
| 컨테이너 | Docker + docker-compose |
| 성능 테스트 | k6 |

---

## 주요 기능

- **자랑 게시글** — 고양이 사진과 함께 게시글 작성, 좋아요, 댓글
- **실종 고양이 신고** — 실종 위치(위도/경도), 고양이 특징 등록
- **댓글 시스템** — 원댓글 + 대댓글 (2depth), 소프트 삭제
- **좋아요** — 동시성 처리 (UniqueConstraint + DataIntegrityViolationException 핸들링)
- **실시간 알림** — SSE 기반 댓글/좋아요 알림
- **검색** — Full-Text Search (ngram, 한글 지원) + LIKE 폴백
- **RBAC 권한 관리** — Role/Permission 기반 접근 제어
- **마이페이지** — 내 게시글, 댓글, 좋아요 목록 + 통계 캐싱

---

## 성능 최적화

### 조회수 — Redis INCR + 배치 동기화
- 동일 사용자 10분 내 재조회 어뷰징 방지 (Redis SET NX)
- Redis INCR으로 원자적 증가, 30초마다 DB 배치 동기화
- Redis 장애 시 DB 직접 업데이트 Fallback

### 인기글 캐시 스탬피드 방지 (3가지 방식 비교)
- v1: 단순 캐시
- v2: Redis 분산 락 (SET NX)
- v3: Cache Warming (25초마다 선제 갱신)

### JWT 권한 조회 방식 (3가지 비교)
- v1: 매 요청마다 DB 조회
- v2: JWT claims에서 직접 추출
- v3: Redis 캐시 조회 (캐시 미스 시 DB Fallback)

### N+1 문제 해결
- Fetch Join (N:1 관계)
- @BatchSize(100) (1:N 컬렉션)
- QueryDSL Projection (필요한 컬럼만 SELECT)

### 마이페이지 통계 캐싱
- `user:stats` 캐시 (TTL 10분)
- 게시글/댓글 작성·삭제 시 @CacheEvict 무효화

---

## 아키텍처

```
단일 모듈 Spring Boot 애플리케이션 (포트 8080)

Client
  └─ Spring Security (JWT Filter + OAuth2)
       └─ Controller
            └─ Service
                 ├─ JPA Repository (MySQL)
                 ├─ Redis (캐시 / 조회수 / 세션)
                 └─ Spring Event (비동기 알림 → SSE)
```

---

## 실행 방법

### Docker로 실행 (권장)
```bash
# 환경변수 파일 준비
cp .env.example .env.local  # 값 채워넣기

# 전체 서비스 실행
docker-compose -f docker-compose.local.yml up -d

# 로그 확인
docker-compose -f docker-compose.local.yml logs -f api
```

### 로컬 직접 실행
```bash
./gradlew :api:bootRun
```

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