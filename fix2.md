# Meow 아키텍처·성능·동시성 검토 체크리스트

> fix.md의 보안/버그 항목과 별도로, 아키텍처적 정합성·동시성·성능·오버엔지니어링 관점에서 검토한 결과입니다.
> 각 항목에는 **왜 이 방향이 더 나은지** 트레이드오프 분석을 포함합니다.

---

## 🔴 동시성 & 정합성 (즉시 검토)

- [ ] **CC-1. PostLikeService 좋아요 Race Condition → likeCount 오염 가능성**
  - 파일: `api/src/main/java/com/min/meow/postlike/service/PostLikeService.java`
  - 문제: 현재 `addLike()` 흐름:
    1. `existsByBoastCatPostIdAndUserId()` → false 확인
    2. `postLikeRepository.save(like)` → JPA 1차 캐시에 등록 (아직 flush 전)
    3. `boastCatPostRepository.incrementLikeCountByDelta(postId, 1)` → **@Modifying이므로 즉시 UPDATE SQL 실행**
    4. 트랜잭션 커밋 시점에 save flush → UniqueConstraint 위반 시 `DataIntegrityViolationException`

    두 스레드가 동시에 1번을 통과하면 3번이 두 번 실행된 후 한 트랜잭션이 롤백됨.
    롤백은 save의 INSERT만 취소하고, 이미 커밋된 UPDATE는 취소 불가
    → **likeCount +2이지만 PostLike 레코드는 1개 (count 오염)**

    `cancelLike()`에서도 동일 패턴: `exists()` 후 `deleteByXxx()` 사이에 다른 스레드 삭제 → likeCount -2

  - 트레이드오프:
    | 방법 | 장점 | 단점 |
    |------|------|------|
    | 비관적 락 (`@Lock(PESSIMISTIC_WRITE)`) | 완벽한 정합성 | 처리량 감소, 데드락 위험 |
    | 낙관적 락 (`@Version`) | 처리량 유지 | OptimisticLockException 재시도 로직 필요 |
    | exists() 제거 + DataIntegrityViolation만 처리 | 가장 단순, DB Unique에 위임 | incrementLikeCount를 exception 핸들러 이후로 이동해야 함 |
    | **권장: save() → saveAndFlush()로 변경** | 단순하고 기존 구조 유지 | flush 강제 필요 |
  - 해결:
    ```java
    postLikeRepository.saveAndFlush(like); // flush 먼저 → constraint 위반 선행 감지
    boastCatPostRepository.incrementLikeCountByDelta(postId, 1); // constraint 통과 후 실행
    ```
    또는 `cancelLike()`에서 `deleteByXxx()` 반환값(int)이 0이면 likeCount decrement 건너뜀

- [ ] **CC-2. ViewCountService GETDEL → DB 실패 시 조회수 영구 손실**
  - 파일: `api/src/main/java/com/min/meow/post/service/ViewCountService.java`
  - 문제: `collectFromRedis()`에서 `GETDEL` 명령(원자적 읽기+삭제) 사용.
    Redis에서 값을 가져오는 순간 삭제됨 → DB 반영 실패 시 해당 조회수 영구 손실.
    루프 내 try/catch가 개별 키 실패를 무시하므로 스케줄러 자체는 계속 동작하지만 데이터는 사라짐.
  - 트레이드오프:
    | 방법 | 장점 | 단점 |
    |------|------|------|
    | **GET → DB 성공 → DEL** | 손실 없음, 구현 단순 | DB-DEL 사이 서버 재시작 시 중복 반영 가능(+α) |
    | GETDEL + 실패 시 Redis 재저장 | 원자성 유지 | 복구 로직 복잡, 재저장 실패 시 또 손실 |
    | Lua Script (GET + conditional DEL) | 진정한 원자성 | Redis Lua 스크립트 관리 부담 |

    조회수는 절대적 정확도보다 **최종 일관성(eventual consistency)** 이 더 중요.
    중복 반영(+α) 가능성이 있는 GET→DEL 방식이 손실(zero)보다 낫다.
  - 해결: `GETDEL` → `GET`으로 변경, DB 반영 성공한 키만 `DEL` 실행

- [ ] **CC-3. @CacheEvict와 @Transactional 커밋 순서 불일치 (stale cache window)**
  - 파일: `api/src/main/java/com/min/meow/comment/service/CommentService.java`,
    `api/src/main/java/com/min/meow/post/service/BoastCatPostService.java`
  - 문제: Spring AOP 프록시 체인 실행 순서:
    ```
    @Transactional 프록시 진입 → 트랜잭션 시작
      @CacheEvict 프록시 진입
        메서드 실행 (DB 변경)
      @CacheEvict 프록시 종료 → 캐시 삭제 ← ⚠️ 여기서 삭제됨
    @Transactional 프록시 종료 → 트랜잭션 커밋
    ```
    캐시가 삭제된 직후 ~ 트랜잭션 커밋 전 사이에 다른 스레드가 캐시를 채우면
    **커밋 전 old 데이터**가 캐시에 적재됨 → TTL 동안 stale 데이터 제공.
  - 트레이드오프:
    | 방법 | 장점 | 단점 |
    |------|------|------|
    | 현재 유지 | 변경 없음 | TTL(10분) 동안 stale 가능성 (낮은 빈도) |
    | `beforeInvocation = true` | window 없음 | 트랜잭션 롤백 시에도 캐시 삭제됨 |
    | `@TransactionalEventListener(AFTER_COMMIT)` 으로 evict | 완벽한 정합성 | 이벤트 클래스 추가, 코드 복잡도 증가 |

    TTL이 10분이고 불일치 window가 수 ms 수준이므로 실용적으로 허용 가능.
    `user:stats`는 통계성 데이터이므로 현 구조 유지 가능. 단, 주석으로 trade-off를 명시할 것.

- [ ] **CC-4. CommentService 대댓글 삭제 Race Condition → 고아 댓글**
  - 파일: `api/src/main/java/com/min/meow/comment/service/CommentService.java` `deleteRootComment()`
  - 문제:
    ```java
    long activeReplies = commentRepository.countActiveRepliesByParentId(comment.getId());
    if (activeReplies == 0) {
        commentRepository.delete(comment); // ← 물리 삭제
    }
    ```
    Thread A가 `countActiveReplies == 0` 확인 직후, Thread B가 대댓글을 추가하면
    Thread A는 원댓글을 물리 삭제 → Thread B의 대댓글은 parent가 없는 고아 상태.
    소프트 삭제이므로 데이터 손실은 없지만 렌더링 시 null parent 처리 필요.
  - 트레이드오프:
    | 방법 | 장점 | 단점 |
    |------|------|------|
    | 비관적 락으로 원댓글 lock | 완벽 | 과도한 설계, 처리량 감소 |
    | 물리 삭제 제거 → 항상 softDelete | 단순, 고아 문제 없음 | DB 용량 증가 |
    | GC 배치로 실제 삭제 처리 | 안전한 삭제 시점 | 배치 추가 필요 |
  - 해결: 원댓글도 항상 `softDelete()` 적용 (물리 삭제 제거).
    불필요한 행은 별도 배치(30일 후 물리 삭제)로 처리.
    렌더링 레이어에서 `isDeleted == true && replies.isEmpty()` 인 경우 숨김 처리.

---

## 🟡 아키텍처 (1~2주 내)

- [ ] **AR-1. EAGER 로딩 4단계 체인: User → UserRole → Role → RolePermission**
  - 파일:
    - `api/src/main/java/com/min/meow/user/entity/User.java` (userRoles: `FetchType.EAGER`)
    - `api/src/main/java/com/min/meow/user/entity/UserRole.java` (role: `FetchType.EAGER`)
    - `api/src/main/java/com/min/meow/user/entity/Role.java` (rolePermissions: `FetchType.EAGER`)
  - 문제: User 1건 조회 시 UserRole → Role → RolePermission까지 연쇄 로딩.
    JWT v1 인증 방식 사용 시 **매 HTTP 요청마다** 이 체인이 실행됨.
    `@BatchSize(size = 10)`은 LAZY 타입에서만 IN절 최적화 효과가 있으며 EAGER에는 무의미.
    User 1명의 Role이 2개, 각 Role의 Permission이 3개면 → **총 10회 쿼리**.
  - 트레이드오프:
    | 방법 | 장점 | 단점 |
    |------|------|------|
    | LAZY 전환 + 필요한 곳에 JOIN FETCH | 쿼리 제어 가능 | 각 호출 위치 확인 필요 |
    | v3 인증(Redis 캐시) 사용 | DB 조회 자체를 없앰 | Redis 장애 시 DB fallback으로 여전히 문제 |
  - 해결: 모두 `FetchType.LAZY`로 변경 + `SecurityConfig`에서 User 로드 시 JOIN FETCH 1쿼리로 처리.
    ※ fix.md H-3과 동일 항목 — 4단계 체인 구조가 핵심임을 강조

- [ ] **AR-2. SSE ConcurrentHashMap → 멀티 인스턴스 배포 시 즉시 장애**
  - 파일: `api/src/main/java/com/min/meow/notification/sse/SseEmitterManager.java`
  - 문제: `private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>()`
    JVM 로컬 Map이므로 서버 2대 이상 배포 시, 서버 A에 연결된 사용자에게
    서버 B에서 알림 전송 시 emitter를 찾지 못해 **SSE 전송 실패**.
    현재 단일 인스턴스이면 무관하지만, k8s 수평 확장 시 즉시 장애.
  - 트레이드오프:
    | 방법 | 장점 | 단점 |
    |------|------|------|
    | Sticky Session (LB IP Hash) | 변경 없이 단기 해결 | 로드 불균형 |
    | Redis Pub/Sub 기반 분산 SSE | 진정한 수평 확장 | Redis 장애 시 SSE 전체 중단, 구현 복잡 |
    | WebSocket + STOMP | 업계 표준, 재연결 쉬움 | 큰 변경, 인프라 추가 |
  - 해결(단기): 로드밸런서에서 Sticky Session 설정.
    해결(중기): Redis Pub/Sub으로 알림 라우팅 — 채널 키 `sse:notify:{userId}`, 로컬 Map은 해당 서버 emitter만 보관.

- [ ] **AR-3. user:stats 캐시 무효화 로직이 3개 서비스에 분산 → 누락 위험**
  - 파일: `BoastCatPostService.java`, `LostCatPostService.java`, `CommentService.java`
  - 문제: `user:stats` 캐시를 게시글 작성/삭제, 댓글 작성/삭제 4개 이벤트에서 각각 `@CacheEvict`.
    새로운 통계 변경 이벤트 추가 시 누락 위험. LostCatPostService 삭제 시 evict 여부 수동 확인 필요.
  - 트레이드오프:
    | 방법 | 장점 | 단점 |
    |------|------|------|
    | 현재 유지 (분산 @CacheEvict) | 간단 | 누락 위험, 리뷰 필요 |
    | `UserStatsChangedEvent` 도메인 이벤트로 통합 | 중앙화, 확장 용이 | 간접성 증가 |
    | TTL 1~2분으로 단축 | 구현 없이 stale 최소화 | 캐시 효율 감소 |
  - 해결(단기): 현재 구조 유지 + 통합 테스트로 evict 커버리지 확보.
    해결(장기): `UserStatsChangedEvent` 도메인 이벤트로 통합.

- [ ] **AR-4. Soft Delete에 @Where 미적용 → 탈퇴 유저 어드민 목록 노출 가능**
  - 파일: `api/src/main/java/com/min/meow/user/entity/User.java`,
    `api/src/main/java/com/min/meow/user/repository/UserRepository.java`
  - 문제: `User.isDelete` 필드가 있으나 `@Where` 어노테이션 미사용.
    `UserRepository.findAllByOptionalRole()`에 `isDelete = false` 조건 없음 → 어드민 유저 목록에 탈퇴자 노출.
    쿼리마다 개발자가 수동으로 필터를 추가해야 하므로 누락 가능성 높음.
  - 트레이드오프:
    | 방법 | 장점 | 단점 |
    |------|------|------|
    | `@Where(clause = "is_delete = false")` | 자동 필터, 누락 불가 | 탈퇴자 직접 조회 불가 → AdminUserService 문제 |
    | `@FilterDef + @Filter` | 세션별 동적 활성화 가능 | Session-level 관리 복잡 |
    | 쿼리마다 수동 추가 (현재) | 유연 | 누락 위험 |
  - 해결: `@Where(clause = "is_delete = false")` 적용.
    AdminUserService처럼 탈퇴자를 직접 조회해야 하는 경우 Native Query 또는 `EntityManager.find()`로 별도 처리.

- [ ] **AR-5. JwtAuthenticationFilter가 v1/v2/v3 세 전략을 한 클래스에서 처리 → 보안 위험**
  - 파일: `api/src/main/java/com/min/meow/security/jwt/JwtAuthenticationFilter.java` (L85-144)
  - 문제: `X-Auth-Version` 요청 헤더로 인증 전략을 **클라이언트가 선택** 가능.
    운영 환경에서 `v1`(매번 DB 조회)을 선택해 DB 부하 유발 가능.
    `v2`(JWT claims만 사용)로 권한 변경 우회 가능.
    단일 책임 원칙 위반, 120줄 이상의 분기 로직.
  - 트레이드오프:
    | 방법 | 장점 | 단점 |
    |------|------|------|
    | 현재 유지 (k6 비교 목적) | 포트폴리오 데모 가능 | 운영 보안 위험, 코드 복잡 |
    | 전략 패턴 분리 (AuthStrategy 인터페이스) | 올바른 설계, 테스트 용이 | 대규모 리팩토링 |
    | `@Profile("local")`로 헤더 분기 비활성화 | 운영 보안 확보, 데모도 유지 | 프로파일 전환 필요 |
  - 해결: `application-prod.yml`에서 헤더 분기 비활성화 + 운영 전략 v3(Redis 캐시) 고정.
    비교 테스트는 `local` 프로파일에서만 헤더로 전환 가능하도록 제한.

- [ ] **AR-6. 비교용 엔드포인트(v1~v4)가 운영 API에 혼재 → API 표면 증가**
  - 파일: `api/src/main/java/com/min/meow/post/controller/BoastCatPostController.java`
    (인기글 v1/v2/v3 엔드포인트 3개 + 조회수 v1/v2/v3/v4 엔드포인트 4개)
  - 문제: 포트폴리오 비교 목적으로 구현된 엔드포인트들이 운영 코드와 혼재.
    Swagger에 전부 노출 → 문서화 부담, 보안 감사 범위 확대.
    `SecurityConfig.java`에서 모든 변형을 명시적으로 허용/차단해야 함.
  - 트레이드오프:
    | 방법 | 장점 | 단점 |
    |------|------|------|
    | 전부 유지 | 포트폴리오 데모 편의 | API 복잡성, 보안 표면 증가 |
    | `@Profile("local")` 또는 Feature Flag | 운영과 개발 분리 | 로컬에서 테스트 필요 |
    | 운영 엔드포인트만 남기고 나머지 삭제 | 깔끔한 API | 데모 불편 |
  - 해결: 운영 확정 전략 엔드포인트만 기본 노출.
    비교용 v1~v4는 `@ConditionalOnProperty(name = "feature.comparison-endpoints", havingValue = "true")`
    또는 `@Profile("local")`로 조건부 활성화.

---

## 🟢 성능 개선 (2주 내)

- [ ] **PF-1. Redis INCR 키에 TTL 미설정 → 삭제된 게시글 키 영구 잔류**
  - 파일: `api/src/main/java/com/min/meow/post/service/ViewCountService.java` (L63)
  - 문제: `redisTemplate.opsForValue().increment(countKey)` → TTL 없음.
    게시글이 삭제되어도 `view:count:BOAST:{id}` 키가 Redis에 영구 잔류.
    스케줄러 실패 시 또는 값이 0인 키는 정리 안 됨 → Redis 메모리 점진적 증가.
  - 해결:
    ```java
    Long currentValue = redisTemplate.opsForValue().increment(countKey);
    if (currentValue != null && currentValue == 1L) { // 최초 생성 시점
        redisTemplate.expire(countKey, Duration.ofHours(2)); // 2시간 TTL
    }
    ```
    또는 게시글 삭제 시 `redisTemplate.delete(countKey)` 명시적 호출.
    (왜 2시간? 스케줄러 30초 주기 기준 1시간이면 충분, 2시간은 재시작 여유분)

- [ ] **PF-2. 위치 검색(BETWEEN)에 공간 인덱스 없음 → Full Table Scan**
  - 파일: `api/src/main/java/com/min/meow/post/repository/LostCatRepositoryImpl.java` (L65-102)
  - 문제: `latitude BETWEEN ? AND ?` + `longitude BETWEEN ?` 조건에 위치 인덱스 없음.
    현재 구현은 직선거리가 아닌 **사각형 범위**(Bounding Box) 검색이라 정확도도 낮음.
  - 트레이드오프:
    | 방법 | 장점 | 단점 |
    |------|------|------|
    | `(latitude, longitude)` 복합 인덱스 | 단순, 즉시 적용 가능 | 범위 검색 시 두 번째 컬럼 효율 낮음 |
    | MySQL Spatial Index (`ST_Distance_Sphere`) | 정확한 원형 반경 검색 | POINT 타입 컬럼 변경 필요 (Flyway 마이그레이션) |
  - 해결(단기): `(latitude, longitude)` 복합 인덱스 추가 (Flyway V5 마이그레이션):
    ```sql
    CREATE INDEX idx_lost_post_location ON lost_cat_post (latitude, longitude);
    ```
  - 해결(중기): `POINT` 타입 컬럼 추가 후 `ST_Distance_Sphere`로 정확한 원형 반경 검색.

- [ ] **PF-3. 알림 스레드풀 큐 고갈 시 RejectedExecutionException 조용히 발생 → 알림 유실**
  - 파일: `api/src/main/java/com/min/meow/config/AsyncConfig.java`
  - 문제: `notificationExecutor`에 `RejectedExecutionHandler` 미설정 → 기본값 `AbortPolicy`.
    큐(100) 가득 차면 `RejectedExecutionException` 발생하지만 **로그 없이 알림 유실**.
    스레드풀 overflow 시 운영자가 문제를 인지 불가.
  - 트레이드오프:
    | RejectionPolicy | 알림 유실 | API 응답 지연 |
    |-----------------|---------|--------------|
    | AbortPolicy (현재) | 유실 + 감지 불가 | 없음 |
    | CallerRunsPolicy | 없음 | 있음 (HTTP 스레드 블로킹) |
    | **커스텀 핸들러 (로그+메트릭)** | 유실되지만 감지 가능 | 없음 |

    CallerRunsPolicy는 API 응답 지연 유발로 부적합. 커스텀 핸들러로 감지만 해도 충분.
  - 해결:
    ```java
    executor.setRejectedExecutionHandler((runnable, executor) -> {
        log.error("알림 스레드풀 큐 포화 - 알림 유실. 큐 크기: {}", executor.getQueue().size());
        // meterRegistry.counter("notification.rejected").increment();
    });
    ```
    큐 크기도 `queueCapacity = 500`으로 여유 확보 권장.

- [x] **PF-4. MyPage 통계 캐시 키 통일 확인 → 이미 userId로 통일됨 (오탐)**
  - 파일: `api/src/main/java/com/min/meow/user/service/MyPageService.java`
  - 확인: `MyPageService.getMyPageStats()` → `@Cacheable(cacheNames = "user:stats", key = "#userId")` (L65)
    `BoastCatPostService`, `LostCatPostService`, `CommentService` 모두 `@CacheEvict(key = "#userId")` 동일.
    캐시 키가 이미 `userId`(Long)로 통일되어 있어 불일치 없음.

- [ ] **PF-5. ViewCountService syncAllToDatabase()의 @Transactional이 실질적으로 무의미**
  - 파일: `api/src/main/java/com/min/meow/post/service/ViewCountService.java` `syncAllToDatabase()`
  - 문제:
    ```java
    @Transactional // ← 이 어노테이션이 실제로 의미하는가?
    public void syncAllToDatabase() {
        for (...) {
            try {
                flushDeltaToDb(...);
            } catch (Exception e) {
                log.error(...); // 예외 삡아먹음 → 롤백 트리거 불가
            }
        }
    }
    ```
    루프 내 try/catch가 예외를 삡아먹으므로 `@Transactional`이 사실상 롤백 트리거 불가.
    100개 업데이트 중 50번째 실패해도 1~49번은 이미 커밋됨.
    코드 읽는 사람이 "하나 실패하면 전부 롤백된다"고 오해할 수 있음.
  - 해결: `@Transactional` 제거 + 각 키를 독립적으로 처리 (현재 실제 동작과 코드 의도 일치).
    또는 `flushDeltaToDb()`에 `@Transactional(propagation = REQUIRES_NEW)` 적용해 진짜 독립 트랜잭션 처리.

---

## 🔵 오버엔지니어링 & 단순화 검토

- [ ] **OE-1. RBAC 4엔티티 체계 — 현재 규모 대비 복잡도 검토**
  - 파일: `Role.java`, `Permission.java`, `RolePermission.java`, `UserRole.java` (4개 엔티티)
  - 현황: 역할 3개 고정(ROLE_USER, ROLE_ADMIN, ROLE_RESTRICTED), 권한 6개 고정.
    권한이 코드에 `"post:write"` 같은 문자열 리터럴로 하드코딩됨.
    `DataInitializer`에서 매 시작 시 DB에 upsert. User 1건 로드에 최대 4단계 조인.
  - 트레이드오프:
    | 방법 | 장점 | 단점 |
    |------|------|------|
    | 현재 유지 (DB RBAC) | 런타임 권한 변경 가능, 설계 역량 시연 | 4단계 조인, 초기화 복잡도 |
    | `enum Role` + 코드 레벨 권한 매핑 | 코드 절반 이상 감소, 조인 없음 | 권한 변경 시 재배포 필요 |
  - 현 구조 유지 권장 (포트폴리오/면접 목적). 단, README/주석에 "관리자 UI에서 동적 권한 변경을 고려해 DB 설계로 구현"이라는 설계 근거 명시 필요.

- [ ] **OE-2. v2 인기글 분산락에 Thread.sleep(100) 블로킹 폴링**
  - 파일: `api/src/main/java/com/min/meow/post/service/BoastCatPostService.java` `getPopularBoastCatPostsV2()`
  - 문제:
    ```java
    for (int i = 0; i < 30; i++) {
        Thread.sleep(100); // 스레드 점유 + 100ms 지연
        ...
    }
    return boastCatPostRepository.findTop24ByScore(); // 3초 후 결국 DB 조회 = v1과 동일
    ```
    Lettuce의 Non-blocking I/O 장점을 Thread.sleep으로 훼손.
    v3(Cache Warming)이 이미 TTL 만료 자체를 방지하므로 v2의 존재 이유 없음.
  - 해결: v3를 운영 전략으로 채택 후 v2 제거.
    k6 비교 테스트에서만 의미 있으므로 `@Profile("local")`로 격리하거나 삭제.

- [ ] **OE-3. NotificationEventPublisher — 부가가치 없는 단순 래퍼**
  - 파일: `api/src/main/java/com/min/meow/notification/event/NotificationEventPublisher.java`
  - 문제: `eventPublisher.publishEvent(event)` 1:1 delegate, 추가 로직 없음.
    `ApplicationEventPublisher` 직접 주입 후 호출과 완전 동일.
  - 트레이드오프:
    | 방법 | 장점 | 단점 |
    |------|------|------|
    | 현재 유지 | 향후 발행 전 검증·로깅 추가 용이 | 불필요한 간접 계층 |
    | 제거 (직접 `ApplicationEventPublisher` 주입) | 코드 단순화 | 발행 위치에 직접 의존성 추가 |
  - 비용이 작으므로 현재 유지 권장. 향후 이벤트 발행 전 검증 로직(수신자 null 체크 등) 추가 시 가치 발생.

- [ ] **OE-4. ViewCountService Redis 장애 후 복구 시 자동 전환 없음**
  - 파일: `api/src/main/java/com/min/meow/post/service/ViewCountService.java`
  - 문제: Redis 장애 → `incrementDbDirectly()`로 전환.
    Redis 복구 후에도 전환 상태 유지 → DB fallback 계속됨 → 배치 최적화 혜택 없음.
  - 트레이드오프:
    | 방법 | 장점 | 단점 |
    |------|------|------|
    | 현재 유지 | 단순, 서버 재시작으로 복구 | 수동 재시작 필요 |
    | Resilience4j CircuitBreaker | 자동 반/개방, 표준 패턴 | 라이브러리 추가 |
    | Redis PING 헬스체크 후 전환 | 경량 | 주기적 PING 오버헤드 |
  - 해결(중기): Resilience4j `@CircuitBreaker` 적용. Actuator Redis health indicator로 복구 모니터링.

---

## 📊 우선순위 요약

| 항목 | 심각도 | 실제 영향 | 수정 난이도 |
|------|--------|---------|------------|
| ~~PF-4 캐시 키 불일치~~ | ~~🔴~~ | 오탐 — 이미 userId로 통일됨 | 완료 |
| CC-1 좋아요 Race Condition | 🔴 | likeCount 오염 | 낮음 (saveAndFlush 1줄) |
| CC-2 GETDEL 데이터 손실 | 🔴 | 조회수 영구 손실 | 낮음 (GET+DEL 분리) |
| CC-4 고아 댓글 | 🟡 | 렌더링 오류 | 낮음 (항상 softDelete) |
| AR-4 @Where 미적용 탈퇴자 노출 | 🟡 | 어드민 데이터 오염 | 중간 |
| AR-5 JWT 버전 분기 보안 | 🟡 | 클라이언트 전략 선택 가능 | 중간 |
| AR-2 SSE 멀티 인스턴스 불가 | 🟡 | 확장 시 즉시 장애 | 높음 |
| PF-1 Redis TTL 미설정 | 🟢 | 메모리 누수 | 낮음 |
| PF-2 위치 검색 인덱스 없음 | 🟢 | Full Table Scan | 낮음 (Flyway 추가) |
| PF-3 스레드풀 rejection 무음 | 🟢 | 알림 유실 감지 불가 | 낮음 |
| PF-5 @Transactional 오해 유발 | 🟢 | 코드 가독성 | 낮음 |
| AR-6 비교용 엔드포인트 혼재 | 🔵 | API 표면 증가 | 중간 |
| OE-2 v2 폴링 코드 | 🔵 | 기술 부채 | 낮음 (v3 채택 후 삭제) |
| AR-3 캐시 evict 분산 | 🔵 | 누락 위험 | 낮음 (테스트 추가) |
| CC-3 CacheEvict 순서 | 🔵 | ms 수준 stale window | 낮음 (주석 추가) |
