# 꼬랑지 - 반려동물 테마 소셜 플랫폼

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
| 인증 | OAuth2 (Kakao) + JWT |
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

- **반려동물 자랑 게시글** - 반려동물 사진과 함께 게시글 작성, 좋아요, 댓글, 인기글
- **반려동물 실종 신고** - 실종 위치(위도/경도), 반려동물 특징 등록, 내 위치기반 게시글 조회
- **댓글 시스템** - 댓글 + 대댓글
- **좋아요** - 동시성 처리 (UniqueConstraint + DataIntegrityViolationException 예외 처리)
- **실시간 알림** - SSE 기반 댓글/좋아요 알림
- **검색** - Full-Text Search (ngram, 한글 지원) + LIKE 폴백
- **RBAC 권한 관리** - Role/Permission 기반 접근 제어
- **마이페이지** - 내 게시글, 댓글, 좋아요 목록 + 통계 캐싱

---

## 문제해결

- **게시글 목록 조회 성능 개선**
    - offset 기반 페이징에서 Full Table Scan과 뒷 페이지로 갈수록 느려지는 문제를 created_at 단일 인덱스와 커버링 인덱스 기반 서브쿼리 JOIN으로 개선하여 평균 응답 속도를 1,100ms에서 20ms로, offset 10만 기준 1,700ms에서 40ms로 개선

- **인기글 목록 조회 점수 집계 방식 개선**
    - 댓글 수, 좋아요 수, 조회수를 합산해 정렬하는 인기글 조회의 집계, 정렬 비용 문제를 Redis Sorted Set으로 점수를 실시간 반영하고 DB IN 조회로 대체하여 평균 응답 속도를 1,200ms에서 100ms로 개선

- **조회수 동시성으로 인한 Lost Update 문제 해결**
    - 동시 요청 시 조회수 80%가 유실되는 Lost Update 문제를 JPA 더티 체킹 방식에서 DB 원자적 UPDATE 방식으로 전환하여 해결

- **SSE 기반 실시간 알림 기능의 지속적인 재연결 시도 문제 해결**
    - Nginx read timeout이 SSE 연결 유지 시간보다 짧아 발생하던 반복 재연결 문제를 proxy_read_timeout 조정과 heartbeat 도입으로 해결


## 실행 방법

### 사전 요구사항
- Docker & Docker Compose
- Kakao OAuth2 클라이언트 ID/Secret
- AWS S3 버킷 및 액세스 키

### 1. 레포지토리 클론
```bash
git clone https://github.com/min318777/kkorangji.git
cd kkorangji
```

### 2. 환경변수 파일 생성
프로젝트 루트에 `.env.local` 파일 생성 후 아래 키에 맞는 값 입력:

```
JWT_SECRET=
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
KAKAO_ADMIN_KEY=
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
| API (Nginx 경유) | http://localhost/api/... |
| Grafana | http://localhost:3001 |
| Prometheus | http://localhost:9090 |

---

## Docker 서비스 구성

| 서비스 | 포트 | 용도 |
|--------|------|------|
| api | 8080 | Spring Boot API |
| nginx | 80 | 리버스 프록시 |
| mysql | 3307 | 데이터베이스 |
| redis | 6379 | 캐시 / 조회수 |
| prometheus | 9090 | 메트릭 수집 |
| grafana | 3001 | 메트릭 시각화 |
| node-exporter | 9100 | 호스트 시스템 메트릭 수집 |

---

## API 문서

서버 실행 후 접속: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## RBAC 권한 구조

### 권한 목록 (12개)

| 권한 코드 | 설명 |
|-----------|------|
| `post:read` | 게시글 조회 |
| `post:create` | 게시글 작성 |
| `post:update` | 게시글 수정 (본인은 소유권 체크로 별도 허용, 이 권한은 타인 게시글 수정용) |
| `post:delete` | 게시글 삭제 (본인만) |
| `post:delete:any` | 게시글 삭제 (타인 포함, 관리자용) |
| `comment:create` | 댓글 작성 |
| `comment:update` | 댓글 수정 (본인은 소유권 체크로 별도 허용, 이 권한은 타인 댓글 수정용) |
| `comment:delete` | 댓글 삭제 (본인만) |
| `comment:delete:any` | 댓글 삭제 (타인 포함, 관리자용) |
| `user:read` | 유저 목록/통계 조회 |
| `user:restrict` | 유저 계정 제재/복원 |
| `user:delete` | 유저 강제 탈퇴 |

### 역할별 권한 매핑 (4개 역할)

| 권한 | ROLE_USER | ROLE_VIEWER | ROLE_ADMIN | ROLE_RESTRICTED |
|------|:---------:|:-----------:|:----------:|:---------------:|
| post:read | ✓ | ✓ | ✓ | ✓ |
| post:create | ✓ | ✓ | ✓ | |
| post:update | | | ✓ | |
| post:delete | ✓ | ✓ | ✓ | |
| post:delete:any | | ✓ | ✓ | |
| comment:create | ✓ | ✓ | ✓ | |
| comment:update | | | ✓ | |
| comment:delete | ✓ | ✓ | ✓ | |
| comment:delete:any | | ✓ | ✓ | |
| user:read | | ✓ | ✓ | |
| user:restrict | | | ✓ | |
| user:delete | | | ✓ | |
