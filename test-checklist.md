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

- [ ] `GET /api/meow/boast-cat`, `GET /api/meow/boast-cat/{id}`
- [ ] `GET /api/meow/boast-cat/view/v3/{id}` (상세+조회수 통합)
  - **존재하지 않는 postId 요청 시 조회수 증가 없이 404만 나는지 — 최근 실제 장애(인기글 Sorted Set에 좀비 데이터 93만 건) 재발 방지 지점, 반드시 검증**
- [ ] `GET /api/meow/boast-cat/popular/v5` (인기글 TOP24)
- [ ] `GET /api/meow/lost-cat`, `GET /api/meow/lost-cat/{id}`
- [ ] `POST /api/meow/lost-cat/{id}/view`
- [ ] `POST /api/meow/lost-cat/v3/{id}/view` — 마찬가지로 존재하지 않는 postId 케이스
- [ ] `GET /api/meow/lost-cat/nearby`, `/nearby/st` — 반경 밖 게시글 필터링 확인

## 우선순위 5 — 이미지 업로드

- [ ] `POST /api/images/presigned-urls` — 요청 개수만큼 URL 발급되는지

## 우선순위 6 — 알림 (SSE 포함)

- [ ] `GET /api/notifications`
- [ ] `PATCH /api/notifications/{id}/read`, `PATCH /api/notifications/read-all`
- [ ] `GET /api/notifications/stream` (SSE) — MockMvc로 검증 까다로움. 우선순위 낮게, 대신 `NotificationEventListener` 단위 테스트로 "저장 시 publish 호출되는지"만 확인

## 우선순위 7 — 인증 부가 기능 / 관리자

- [ ] `POST /api/auth/token/refresh` — 재발급 성공/만료·위조 토큰 실패
- [ ] `GET /api/users/check-id`, `check-nickname` — 중복 있음/없음
- [ ] `GET /api/admin/users`, `PATCH .../status`, `DELETE /api/admin/users/{id}`
  - 제재/복원/강제탈퇴 시 `PermissionCacheService` 캐시 무효화가 같이 불리는지
- [ ] `GET /api/admin/stats/dau`

---

## 제외 목록 (프론트가 안 쓰므로 이번 범위 밖)

| 엔드포인트 | 이유 |
|---|---|
| `GET /api/meow/boast-cat/view/v1,v2,v4/{id}` | 동시성 처리방식 성능 비교용 실험 코드, 프론트는 v3만 호출 |
| `POST /api/meow/lost-cat/v1/{id}/view` | 위와 동일 |
| `POST /api/auth/kakao/webhook/unlink` | 카카오 서버가 직접 호출하는 서버-서버 웹훅, 프론트 무관 |
| `GET /oauth2/authorization/kakao` | Spring Security가 처리, 컨트롤러 코드 자체가 없음 |

---

## 시작 순서 제안

1. `PostLikeService` — 동시성 로직이 명확하고 이미 있는 `UserServiceTest` 패턴을 그대로 따라 짤 수 있음
2. 자랑글/실종글 CRUD 권한 검증 (`isAuthor` 체크가 여러 메서드에 반복되므로 하나 짜면 나머지도 패턴 재사용)
3. 조회수 v3의 "존재하지 않는 postId" 케이스 — 실제 장애를 재현하는 회귀 테스트라 가치가 큼
4. 나머지는 여유 될 때 순서 무관하게 진행
