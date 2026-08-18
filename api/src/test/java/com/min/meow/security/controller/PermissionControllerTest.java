package com.min.meow.security.controller;

import com.min.meow.support.IntegrationTestBase;
import com.min.meow.user.entity.*;
import com.min.meow.user.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RBAC 권한 체크 통합 테스트")
class PermissionControllerTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    /**
     * 권한이 있는 사용자를 위한 JWT 헤더를 생성한다.
     *
     * FastAPI의 admin_client fixture와 동일한 역할.
     * JWT Claim에 permissions 목록을 포함시켜
     * @PreAuthorize("hasAuthority('comment:create')") 통과를 보장한다.
     *
     * @param userId    사용자 PK
     * @param role      역할 (예: "ROLE_USER", "ROLE_ADMIN")
     * @param perms     권한 코드 목록 (예: ["comment:create", "post:update"])
     * @return Authorization: Bearer {token} + X-Auth-Version: v2 헤더
     */
    private HttpHeaders createAuthHeaderWithPermissions(Long userId, String role, List<String> perms) {
        // JWT Access Token에 permissions 클레임을 포함하여 생성
        // JwtAuthenticationFilter → CustomUserDetails → getAuthorities()에서 권한으로 등록됨
        String token = jwtProvider.createAccessToken(userId, role, perms);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        // v2: DB 조회 없이 JWT Claims만으로 인증 (성능 + 테스트 독립성)
        headers.add("X-Auth-Version", "v2");
        return headers;
    }

    /**
     * 지정한 권한을 가진 테스트용 사용자를 DB에 생성한다.
     *
     * FastAPI의 admin_user fixture 매핑:
     *   1) Permission 저장 (link 테이블에 직접 삽입 — 비동기 관계 추가 이슈 방지)
     *   2) Role 저장
     *   3) RolePermission(링크) 저장
     *   4) User 저장
     *   5) UserRole(링크) 저장
     *
     * @param loginId     로그인 ID (유니크해야 함)
     * @param email       이메일 (유니크해야 함)
     * @param roleName    역할 이름 (예: "ROLE_USER", "ROLE_ADMIN")
     * @param permCodes   부여할 권한 코드 목록
     * @return 저장된 User 엔티티
     */
    private User createUserWithPermissions(String loginId, String email, String roleName, List<String> permCodes) {
        // 1. Permission 저장 — code가 같으면 기존 것을 재사용 (중복 저장 방지)
        List<Permission> permissions = permCodes.stream()
                .map(code -> permissionRepository.findByCode(code)
                        .orElseGet(() -> permissionRepository.save(new Permission(code, code + " 권한"))))
                .toList();

        // 2. Role 저장 — name이 같으면 기존 것을 재사용
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName, roleName + " 역할")));

        // 3. RolePermission(링크 테이블) 직접 저장
        //    FastAPI의 test_session.add(RolePermissionLink(role_id=..., permission_id=...))
        //    findAll() 대신 (role_id, permission_id) 조합으로 DB에서 직접 존재 확인
        //    → 전체 테이블 로드 없이 단건 쿼리로 중복 방지
        for (Permission perm : permissions) {
            rolePermissionRepository.findByRoleIdAndPermissionId(role.getId(), perm.getId())
                    .orElseGet(() -> rolePermissionRepository.save(new RolePermission(role, perm)));
        }

        // 4. User 저장 — nickname은 VARCHAR(10) 제한이므로 짧게 고정
        User user = User.builder()
                .loginId(loginId)
                .email(email)
                .nickname("냥이")   // VARCHAR(10) 제한 — loginId가 길면 초과하므로 고정값 사용
                .password("encoded_password")
                .userRoles(new java.util.ArrayList<>())
                .build();
        user = userRepository.save(user);

        // 5. UserRole(링크 테이블) 직접 저장
        //    FastAPI의 test_session.add(UserRoleLink(user_id=..., role_id=...))
        userRoleRepository.save(new UserRole(user, role));

        return user;
    }

    @Nested
    @DisplayName("POST /api/meow/boast-cat/{postId}/comments — comment:create 권한 체크")
    class BoastCatCommentPermission {

        private User savedUser;

        @AfterEach
        void tearDown() {
            // 생성한 User 삭제 — 연결된 UserRole도 cascade 또는 직접 삭제
            if (savedUser != null) {
                // deleteByUserId: DB 레벨에서 해당 User의 UserRole만 삭제 (findAll() 불필요)
                userRoleRepository.deleteByUserId(savedUser.getId());
                userRepository.deleteById(savedUser.getId());
            }
        }

        @Test
        @DisplayName("성공: comment:create 권한이 있으면 댓글 작성 요청이 처리된다 (201 또는 4xx — 게시글 없음)")
        void test_성공_자랑글_댓글_작성_권한_보유() {

            // given — comment:create 권한을 가진 사용자 생성
            savedUser = createUserWithPermissions(
                    "permuser01", "permuser01@test.com",
                    "ROLE_USER", List.of("comment:create", "post:read")
            );

            // given — JWT에 comment:create 권한 포함
            // 핵심: @PreAuthorize("hasAuthority('comment:create')") 통과를 위해
            //       JWT claims의 permissions 필드에 "comment:create"를 반드시 포함해야 함
            HttpHeaders headers = createAuthHeaderWithPermissions(
                    savedUser.getId(), "ROLE_USER",
                    List.of("comment:create", "post:read")
            );
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = Map.of("content", "테스트 댓글");

            // when — 존재하지 않는 boastCatPostId=999999로 요청
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/meow/boast-cat/999999/comments",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — 권한 체크 통과 (403이 아니어야 함)
            //        게시글이 존재하지 않으므로 404 또는 500이 예상되지만, 403은 아님
            //        @PreAuthorize 통과 여부만 검증
            assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);

            // 중요: 인증은 됐으나 게시글 없음이므로 2xx가 아닌 4xx/5xx
            assertThat(response.getStatusCode().is2xxSuccessful()).isFalse();
        }

        @Test
        @DisplayName("실패: comment:create 권한이 없으면 403 Forbidden을 반환한다")
        void test_실패_자랑글_댓글_작성_권한_없음() {

            // given — comment:create 권한 없이 post:read만 가진 사용자
            savedUser = createUserWithPermissions(
                    "nopermuser01", "nopermuser01@test.com",
                    "ROLE_USER", List.of("post:read")
            );

            // given — JWT에 comment:create 미포함
            // 핵심: permissions에 "comment:create"가 없으면
            //       CustomUserDetails.getAuthorities()에 해당 권한이 없고
            //       @PreAuthorize("hasAuthority('comment:create')")에서 거부됨
            HttpHeaders headers = createAuthHeaderWithPermissions(
                    savedUser.getId(), "ROLE_USER",
                    List.of("post:read")   // comment:create 없음
            );
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = Map.of("content", "권한 없는 댓글");

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/meow/boast-cat/999999/comments",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — 403 Forbidden (@PreAuthorize 거부)
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("실패: 인증 없이 댓글 작성하면 4xx를 반환한다 (401 또는 403)")
        void test_실패_자랑글_댓글_작성_인증_없음() {
            // given — 헤더 없음 (비인증 요청)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> requestBody = Map.of("content", "비인증 댓글");

            // when — 비인증 403은 Spring Security 기본 처리로 본문이 비어있을 수 있어 String으로 수신
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/meow/boast-cat/999999/comments",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            // then — 4xx (인증 없는 접근 → Spring Security 기본 거부)
            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        }
    }

    @Nested
    @DisplayName("POST /api/meow/lost-cat/{postId}/comments — comment:create 권한 체크")
    class LostCatCommentPermission {

        private User savedUser;

        @AfterEach
        void tearDown() {
            if (savedUser != null) {
                // deleteByUserId: DB 레벨에서 해당 User의 UserRole만 삭제 (findAll() 불필요)
                userRoleRepository.deleteByUserId(savedUser.getId());
                userRepository.deleteById(savedUser.getId());
            }
        }

        @Test
        @DisplayName("성공: comment:create 권한이 있으면 403이 반환되지 않는다")
        void test_성공_실종글_댓글_작성_권한_보유() {
            // given
            savedUser = createUserWithPermissions(
                    "lostperm01", "lostperm01@test.com",
                    "ROLE_USER", List.of("comment:create")
            );

            HttpHeaders headers = createAuthHeaderWithPermissions(
                    savedUser.getId(), "ROLE_USER",
                    List.of("comment:create")
            );
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> requestBody = Map.of("content", "실종글 댓글");

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/meow/lost-cat/999999/comments",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — @PreAuthorize 통과 (403 아님)
            assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("실패: comment:create 권한이 없으면 403 Forbidden을 반환한다")
        void test_실패_실종글_댓글_작성_권한_없음() {
            // given
            savedUser = createUserWithPermissions(
                    "lostnoperm01", "lostnoperm01@test.com",
                    "ROLE_USER", List.of("post:read")
            );

            HttpHeaders headers = createAuthHeaderWithPermissions(
                    savedUser.getId(), "ROLE_USER",
                    List.of("post:read")   // comment:create 없음
            );
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> requestBody = Map.of("content", "권한 없는 댓글");

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/meow/lost-cat/999999/comments",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — 403 Forbidden
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("PUT /api/meow/comments/{id} — comment:update 권한 체크 (댓글 수정)")
    class UpdateCommentPermission {

        private User savedUser;

        @AfterEach
        void tearDown() {
            if (savedUser != null) {
                // deleteByUserId: DB 레벨에서 해당 User의 UserRole만 삭제 (findAll() 불필요)
                userRoleRepository.deleteByUserId(savedUser.getId());
                userRepository.deleteById(savedUser.getId());
            }
        }

        @Test
        @DisplayName("성공: comment:update 권한이 있으면 댓글 수정 요청이 403 없이 처리된다")
        void test_성공_댓글_수정_권한_보유() {
            // given
            savedUser = createUserWithPermissions(
                    "updateperm01", "updateperm01@test.com",
                    "ROLE_USER", List.of("comment:update")
            );

            HttpHeaders headers = createAuthHeaderWithPermissions(
                    savedUser.getId(), "ROLE_USER",
                    List.of("comment:update")
            );
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> requestBody = Map.of("content", "수정된 댓글");

            // when — 존재하지 않는 commentId=999999
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/meow/comments/999999",
                    HttpMethod.PUT,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — 권한 체크 통과 (403이 아님)
            assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("실패: comment:update 권한이 없으면 댓글 수정 시 403 Forbidden을 반환한다")
        void test_실패_댓글_수정_권한_없음() {
            // given
            savedUser = createUserWithPermissions(
                    "updatenoperm01", "updatenoperm01@test.com",
                    "ROLE_USER", List.of("post:read")
            );

            HttpHeaders headers = createAuthHeaderWithPermissions(
                    savedUser.getId(), "ROLE_USER",
                    List.of("post:read")   // comment:update 없음
            );
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> requestBody = Map.of("content", "수정 시도");

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/meow/comments/999999",
                    HttpMethod.PUT,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — 403 Forbidden
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("ROLE_ADMIN vs ROLE_USER — 역할별 권한 차이")
    class AdminVsUserPermission {

        private User adminUser;
        private User normalUser;

        @AfterEach
        void tearDown() {
            // deleteByUserId: DB 레벨에서 해당 User의 UserRole만 삭제 (findAll() 불필요)
            if (adminUser != null) {
                userRoleRepository.deleteByUserId(adminUser.getId());
                userRepository.deleteById(adminUser.getId());
            }
            if (normalUser != null) {
                userRoleRepository.deleteByUserId(normalUser.getId());
                userRepository.deleteById(normalUser.getId());
            }
        }

        @Test
        @DisplayName("관리자: comment:create + post:update 권한 모두 보유 → 각 엔드포인트에서 403 반환 안 됨")
        void test_성공_관리자_댓글_작성_전체_권한_보유() {
            // given — ROLE_ADMIN: 모든 권한 보유
            // security.md에 명시된 실제 권한 코드 구성
            List<String> adminPerms = List.of(
                    "post:read", "post:create", "post:update", "post:delete",
                    "comment:create", "comment:update", "comment:delete", "user:delete"
            );
            adminUser = createUserWithPermissions(
                    "adminuser01", "adminuser01@test.com",
                    "ROLE_ADMIN", adminPerms
            );

            HttpHeaders headers = createAuthHeaderWithPermissions(
                    adminUser.getId(), "ROLE_ADMIN", adminPerms
            );
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> requestBody = Map.of("content", "관리자 댓글");

            // when — comment:create 필요 엔드포인트
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/meow/boast-cat/999999/comments",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — @PreAuthorize("hasAuthority('comment:create')") 통과 (403 아님)
            assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("일반 사용자: comment:create 없이 요청 → 403 Forbidden")
        void test_실패_일반_사용자_댓글_작성_권한_없음() {
            // given — ROLE_USER: post:read만 보유 (DataInitializer 기본값이 아닌 최소 권한 테스트)
            normalUser = createUserWithPermissions(
                    "normaluser01", "normaluser01@test.com",
                    "ROLE_USER_MIN", List.of("post:read")   // comment:create 없음
            );

            HttpHeaders headers = createAuthHeaderWithPermissions(
                    normalUser.getId(), "ROLE_USER_MIN",
                    List.of("post:read")
            );
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> requestBody = Map.of("content", "권한 없는 댓글");

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/meow/boast-cat/999999/comments",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — 403 Forbidden
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
