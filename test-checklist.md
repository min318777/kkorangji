# 테스트 코드 체크리스트 (프론트에서 실제 쓰는 API만)

`meow-front2`의 `src/lib/api/*.ts`, `src/hooks/useNotificationSSE.ts` 전체를 훑어서, 프론트가 실제로 호출하는 백엔드 엔드포인트만 추렸음.
백엔드에 있지만 프론트가 안 쓰는 API(조회수 v1/v2/v4 성능 비교용, 카카오 unlink 웹훅 등)는 이번 범위에서 제외 — 맨 아래 "제외 목록" 참고.

## 이미 테스트된 것 (건너뛰기)

- [x] `POST /api/users/login` — `UserServiceTest`
- [x] `POST /api/users/join` — `UserServiceTest`
- [x] `DELETE /api/users/me` (회원탈퇴) — `UserServiceTest`
- [x] `GET /api/users/me` (마이페이지 요약) — `MyPageServiceTest`
- [x] `PATCH /api/users/me` (닉네임 수정) — `MyPageServiceTest`
- [x] `GET /api/users/me/posts`, `/me/comments` — `MyPageServiceTest`
- [x] 로그인 통합 흐름 — `UserControllerTest`, `MyPageControllerTest`

---

## 우선순위 1 — 게시글 CRUD (프론트 사용 빈도 가장 높음, 권한 검증 필수)

- [x] `POST /api/meow/boast-cat` (글쓰기) — `BoastCatPostServiceTest`
- [x] `PUT /api/meow/boast-cat/{id}` (글수정) — `BoastCatPostServiceTest` (본인 성공 / 타인 시 관리자 권한이어도 403)
- [x] `DELETE /api/meow/boast-cat/{id}` — `BoastCatPostServiceTest` (본인 / `post:delete` 권한 / 권한 없는 타인 차단)
- [x] `POST /api/meow/lost-cat` (실종글 작성) — `LostCatPostServiceTest` (lat/lng 있음/없음 둘 다)
- [x] `PUT /api/meow/lost-cat/{id}` — `LostCatPostServiceTest`
- [x] `DELETE /api/meow/lost-cat/{id}` — `LostCatPostServiceTest`
- [x] `PATCH /api/meow/lost-cat/{id}/status` (찾는중↔완료) — `LostCatPostServiceTest` (관리자 권한으로도 불가한 비대칭성 검증 완료)

## 우선순위 2 — 좋아요 (동시성 처리 검증)

- [x] `POST /api/meow/boast-cat/{id}/like` — `PostLikeServiceTest`
- [x] `DELETE /api/meow/boast-cat/{id}/like` — `PostLikeServiceTest`
- [ ] `GET /api/meow/boast-cat/{id}/like/status` — 단순 위임(`existsByBoastCatPostIdAndUserId` 그대로 반환)이라 프로젝트 규칙상 생략 대상

## 우선순위 3 — 댓글

- [x] `POST /api/meow/{postType}/{postId}/comments` — `CommentServiceTest` (원댓글/대댓글, 2뎁스 제한, 알림 생략 조건, LOST는 인기점수 이벤트 미발행)
- [x] `DELETE /api/meow/comments/{commentId}` — `CommentServiceTest` (즉시삭제/소프트삭제/대댓글 연쇄삭제/권한)
- [ ] `GET /api/meow/{postType}/{postId}/comments` — 단순 조회 조합 로직, 프로젝트 규칙상 우선순위 낮음 (skip 후보)

## 우선순위 4 — 조회 API (프론트가 실제 쓰는 버전만: v3)

- [ ] `GET /api/meow/boast-cat`, `GET /api/meow/boast-cat/{id}` — 단순 조회, 우선순위 낮음
- [x] `GET /api/meow/boast-cat/view/v3/{id}`, `POST /api/meow/lost-cat/v3/{id}/view` — `ViewCountServiceTest`
  - `incrementViewCount()`: 정상 증가, 어뷰징 락 차단, **존재하지 않는 postId는 카운트 증가 없이 무시(회귀 방지 핵심 검증 완료)**, Redis 장애 시 DB fallback
- [x] `GET /api/meow/boast-cat/popular/v5` (인기글 TOP24) — `PopularRankingServiceTest`
  - `removeFromRanking()`(삭제 시 랭킹 제거), `updateViewScores()`(boast만 반영, lost 무시), `getTop24PostIds()`
- [ ] `GET /api/meow/lost-cat`, `GET /api/meow/lost-cat/{id}` — 단순 조회, 우선순위 낮음
- [ ] `POST /api/meow/lost-cat/{id}/view` — 단순 위임 (postExists 체크 없는 구버전, 프론트는 이것보다 v3 위주로 사용)
- [ ] `GET /api/meow/lost-cat/nearby`, `/nearby/st` — QueryDSL 지리 쿼리라 유닛보다 `@DataJpaTest` 통합테스트가 적합, 남겨둠

## 우선순위 5 — 이미지 업로드

- [ ] `POST /api/images/presigned-urls` — Content-Type 검증은 Controller에 있고, 실제 URL 생성은 AWS S3Presigner 호출이라 순수 유닛보다 `@SpringBootTest` 통합테스트(또는 LocalStack)가 적합. 남겨둠

## 우선순위 6 — 알림 (SSE 포함)

- [ ] `GET /api/notifications` — 단순 조회, 우선순위 낮음
- [x] `PATCH /api/notifications/{id}/read`, `PATCH /api/notifications/read-all` — `NotificationQueryServiceTest`
  - 타인 알림 접근 차단(`FORBIDDEN_NOTIFICATION_ACCESS`), 이미 읽은 알림 재요청 시 예외 없이 통과, 다건/전체 읽음 처리 시 "실제로 새로 읽은 개수"만 정확히 집계되는지
- [ ] `GET /api/notifications/stream` (SSE) — MockMvc로 검증 까다로움. 남겨둠

## 우선순위 7 — 인증 부가 기능 / 관리자

- [ ] `POST /api/auth/token/refresh` — `ReissueService`, 로컬 Redis 필요해 통합테스트가 적합. 남겨둠
- [ ] `GET /api/users/check-id`, `check-nickname` — 단순 위임(`!existsByX`)이라 프로젝트 규칙상 생략 대상
- [x] `GET /api/admin/users`, `PATCH .../status`, `DELETE /api/admin/users/{id}` — `AdminUserServiceTest`
  - 본인 관리 차단, 관리자 계정 면역, 상태 전이 규칙(이미 제한/제한 아님), 탈퇴 유저 차단, 캐시 무효화 항상 호출 확인
- [ ] `GET /api/admin/stats/dau` — 단순 조회, 우선순위 낮음

---

## 제외 목록 (프론트가 안 쓰므로 이번 범위 밖)

| 엔드포인트 | 이유 |
|---|---|
| `GET /api/meow/boast-cat/view/v1,v2,v4/{id}` | 동시성 처리방식 성능 비교용 실험 코드, 프론트는 v3만 호출 |
| `POST /api/meow/lost-cat/v1/{id}/view` | 위와 동일 |
| `POST /api/auth/kakao/webhook/unlink` | 카카오 서버가 직접 호출하는 서버-서버 웹훅, 프론트 무관 |
| `GET /oauth2/authorization/kakao` | Spring Security가 처리, 컨트롤러 코드 자체가 없음 |

---

## 진행 현황

우선순위 1~7에서 유닛테스트로 다룰 가치가 있는 항목은 전부 완료 (8개 테스트 클래스, 총 60여 개 케이스):
`PostLikeServiceTest`, `BoastCatPostServiceTest`, `LostCatPostServiceTest`, `CommentServiceTest`,
`ViewCountServiceTest`, `PopularRankingServiceTest`, `AdminUserServiceTest`, `NotificationQueryServiceTest`.

남은 미체크 항목은 전부 두 부류 중 하나라 의도적으로 제외함:
1. 단순 위임/단순 조회라 프로젝트 규칙(`test.md`)상 생략 대상 (`check-id`, `like/status`, 목록 조회 등)
2. AWS S3 / Redis(Redisson) / QueryDSL 지리 쿼리처럼 실제 인프라가 필요해 `@SpringBootTest` 통합테스트가 더 적합한 것
   (`presigned-urls`, `token/refresh`, `nearby/st`, SSE `stream`)

부가로 발견한 것:
- `.gitignore`의 `api/src/test/` 규칙이 테스트 소스 전체를 무시하고 있던 버그 → 제거
- `MyPageServiceTest`에 있던 잘못된 에러코드 기대값(`UNREGISTERED_USER` → `NOT_FOUND_USER`) → 수정
- 기존 `@SpringBootTest` 통합테스트 3종(`UserControllerTest`, `MyPageControllerTest`, `PermissionControllerTest`)은
  로컬에 Redis(`localhost:6379`)가 떠있지 않아 실패 — 이번 작업과 무관한 기존 환경 문제, 로컬 Redis 기동 시 해결됨
