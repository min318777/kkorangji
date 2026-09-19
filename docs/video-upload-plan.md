# 이미지/동영상 업로드 크기·개수 제한 (PUT + 서버 사이드 검증) — 구현 완료

## 최종 방식 (계획 대비 변경됨)

당초 이미지/동영상 업로드를 Presigned **POST**로 통일해 S3 레벨에서 크기를 강제하려 했으나,
**AWS SDK for Java v2는 어떤 버전(2.20.45, 최신 2.29.52 모두 확인)에도 `presignPostObject`를
제공하지 않는다는 사실을 구현 착수 전 `javap`으로 직접 확인**하고 계획을 변경했다.

최종적으로 **기존 PUT presigned URL 방식을 유지**하고, 클라이언트가 신고한 파일 크기(`fileSize`)를
서버가 검증하는 방식으로 구현했다.

**한계**: 이 방식은 S3가 실제로 업로드 크기를 강제하는 게 아니라, 클라이언트가 신고한 값을
서버가 신뢰하고 비교하는 것이다. 악의적인 클라이언트가 fileSize를 속이고 실제로 더 큰 파일을
PUT으로 올리는 것을 S3 레벨에서는 막지 못한다. 포폴/면접에서는 "S3가 강제한다"가 아니라
"서버가 신고된 크기를 검증한다"로 정확히 설명해야 한다.

---

## 구현 내용

### 백엔드

1. **`PresignedFileRequest`** (신규) — `{contentType, fileSize}` 항목 DTO
2. **`PresignedUrlRequest`** — 기존 `contentTypes: List<String>`을 `files: List<PresignedFileRequest>`로 교체
3. **`ImageController`**
   - `validateFiles()` — 이미지 최대 10장, 동영상 최대 1개 (타입별 개수 검증)
   - `validateFile()` — 이미지 10MB, 동영상 200MB 초과 시 `IMAGE_SIZE_EXCEEDED`
   - 허용 타입에 `video/mp4` 추가
4. **`ErrorCode`** — `IMAGE_COUNT_EXCEEDED` 추가, `INVALID_IMAGE_FORMAT`/`IMAGE_SIZE_EXCEEDED` 메시지에 동영상 포함
5. **`S3Service`** — `getExtensionFromContentType()`에 `video/mp4 → .mp4` 추가 (발급 로직 자체는 PUT 그대로 유지)
6. **`BoastCatPost` 엔티티** — `videoUrl` 컬럼 추가, `updatePost()` 시그니처에 `newVideoUrl` 파라미터 추가
7. **Flyway `V22__add_video_url_to_boast_cat_post.sql`** — `boast_cat_post.video_url VARCHAR(500) NULL`
8. **`CreateBoastCatPostRequest`** — `videoKey: String` 추가
9. **`UpdateBoastCatPostRequest`** — `video: ImageItemRequest` 추가 (미포함 시 기존 유지)
10. **`ImageItemRequest`** — `ImageType`에 `REMOVE` 추가 (동영상 삭제 표현용), `value`의 `@NotBlank` 제거(REMOVE는 값 없음)
11. **`BoastCatPostService`**
    - `createBoastCatPost` — videoKey → CloudFront URL 변환 후 엔티티 저장
    - `updateVideo()` (신규) — EXISTING/NEW/REMOVE 처리, 교체·삭제 시 기존 S3 파일을 비동기 삭제 이벤트로 정리
    - `deleteBoastCatPost` — 게시글 삭제 시 videoUrl도 S3에서 같이 삭제
12. **Response DTO** (`GetBoastCatPostResponse`, `CreateBoastCatPostResponse`, `UpdateBoastCatPostResponse`) — `videoUrl` 필드 추가.
    `BoastCatPostListResponse`(목록 Projection)에는 추가하지 않음 — 목록에서는 썸네일만 필요, 불필요한 컬럼 조회 방지
13. **`LegacyUploadTestController`** (신규, `@Profile("local")`) — Before/After 비교 측정 전용,
    `POST /api/test/boast-posts/legacy-upload` — `S3Uploader`(기존 `@Deprecated`) 재사용, 실서비스 미반영
14. **`application-local.yml`** — `spring.servlet.multipart.max-file-size/max-request-size`를 250MB로 상향
    (legacy 테스트 엔드포인트 대응, local 프로필 전용 — 실서비스는 Presigned 방식이라 이 설정의 영향을 받지 않음)

### 프론트엔드 (`meow-front2`)

1. **`lib/api/posts.ts`**
   - `getPresignedUrls(contentTypes: string[])` → `getPresignedUrls(files: PresignedFileRequest[])`로 시그니처 변경
   - `createBoastPost`에 `videoKey?: string` 추가
   - `BoastPostDetail`에 `videoUrl: string | null` 추가
   - `updateBoastPost`에 `video?: VideoItemRequest` 추가, `VideoItemRequest` 타입(EXISTING/NEW/REMOVE) 신규
2. **`app/boast/write/page.tsx`** — 동영상 선택/미리보기/제거 UI, 이미지 10MB 크기 검증 추가,
   이미지+동영상 presigned 발급을 한 번에 묶어 요청
3. **`app/boast/[id]/edit/page.tsx`** — `VideoState`(existing/new/removed) 상태로 동영상 수정 흐름 처리,
   기존 동영상 유지/교체/삭제 각각 대응
4. **`app/lost/write/page.tsx`, `app/lost/[id]/edit/page.tsx`** — `getPresignedUrls` 시그니처 변경에 따른
   호출부 수정만 반영 (동영상 기능은 자랑글 전용, 실종글 범위 아님)

---

## 측정 방법 (k6 없이, 수동 호출) — 아직 미실행

### 준비물
- 테스트용 mp4 파일 1개, 50MB 내외
- Postman 또는 curl + `time` 명령으로 응답 시간 측정
- 로그인 후 발급받은 JWT accessToken 필요 (`Authorization: Bearer {token}`)

### 측정 케이스

**Before (서버 경유, local 프로필 실행 중에만 가능)**
```bash
time curl -X POST http://localhost:8080/api/test/boast-posts/legacy-upload \
  -H "Authorization: Bearer {accessToken}" \
  -F "video=@dummy_50mb.mp4" \
  -F "title=test" -F "content=test"
```
→ 전체 API 응답 시간 기록 (동일 파일로 3~5회 반복 후 평균)

**After (Presigned)**
1. `POST /api/images/presigned-urls` 호출 (`files: [{contentType: "video/mp4", fileSize: 52428800}]`) → 응답 시간 기록
2. 발급받은 presignedUrl로 S3에 직접 PUT 업로드 → 별도로 시간 기록 (참고용, 서버 무관)
3. 업로드 완료 후 key를 `videoKey`에 담아 `POST /api/meow/boast-cat-posts` 호출 → 응답 시간 기록

→ 1번과 3번 시간의 합이 "서버가 실제로 부담한 시간", 2번은 "클라이언트-S3 직접 통신 시간(서버 무관)"

### 기록 표 양식

| 구분 | 파일 크기 | 반복 횟수 | 평균 응답 시간 |
|---|---|---|---|
| Before - 자랑글 작성 API(동영상 포함) | 50MB | 5회 | ? ms |
| After - presigned URL 발급 API | - | 5회 | ? ms |
| After - S3 직접 업로드(참고, 서버 무관) | 50MB | 5회 | ? ms |
| After - 자랑글 작성 API(key만 전달) | - | 5회 | ? ms |

---

## 포폴 문서화 (초안)

```
문제 상황: 동영상 업로드 기능 추가 시, 서버가 파일을 직접 수신해 S3로 재전송하는
구조라면 파일 크기에 비례해 API 응답 시간과 서버 메모리 부담이 커지는 구조였습니다.

해결 과정: Presigned URL 방식을 도입해 클라이언트가 S3에 직접 업로드하도록 전환하고,
발급 요청 시 클라이언트가 신고한 파일 크기를 서버가 검증해 이미지는 10MB, 동영상은
200MB로 업로드 용량을 제한했습니다. (AWS Java SDK가 Presigned POST의 S3 레벨 크기
강제를 지원하지 않아, 서버 사이드에서 신고된 크기를 검증하는 방식을 택했습니다.)

성과: 50MB 동영상 업로드 기준, 자랑글 작성 API 응답 시간이 서버 경유 방식
[X ms] 대비 Presigned 방식(발급+등록 API 합산) [Y ms]로 감소했습니다.
```

- Before 수치는 파일 크기에 따라 응답 시간이 선형적으로 늘어나는 특성이 있으므로,
  "50MB 기준"처럼 조건을 명시할 것
- "S3가 강제한다"는 표현은 쓰지 말 것 — 실제로는 서버가 클라이언트 신고값을 검증하는 것

---

## 남은 작업

1. **실측 미실행** — 위 표의 `? ms`를 실제로 채워야 함. `LegacyUploadTestController`가 local 프로필에서
   정상 동작하는지, JWT 토큰 발급 후 실제로 curl 호출해서 확인 필요
2. **테스트용 mp4 파일 준비** — 50MB 더미 파일 생성 필요
3. **`LegacyUploadTestController`는 브랜치 정리 시 main에 머지하지 않거나, 측정 후 삭제 검토**
   (`@Profile("local")`로 안전장치는 되어 있으나 실서비스 코드베이스에 영구히 남길 이유는 없음)
4. **프론트 실제 동작 확인 미실시** — 타입 체크(`tsc --noEmit`)만 통과했고, 브라우저에서 실제
   업로드 플로우(이미지+동영상 동시 첨부, 수정 페이지에서 동영상 교체/삭제)는 아직 테스트 안 함
