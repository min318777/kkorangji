package com.min.meow.support;

import com.min.meow.security.jwt.JwtProvider;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    /** 로컬 Redis 없이도 통합 테스트가 자체 완결되도록 Docker로 임시 Redis 컨테이너를 띄운다 */
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.0-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    /** 실제 HTTP 요청을 보내는 클라이언트 (포트는 랜덤으로 할당됨) */
    @Autowired
    protected TestRestTemplate restTemplate;

    /** JWT 토큰 생성을 위한 유틸리티 (application-test.yml 설정으로 동작) */
    @Autowired
    protected JwtProvider jwtProvider;

    /**
     * JWT Access Token을 Bearer 헤더로 포함한 HttpHeaders 생성.
     *
     * FastAPI의 override_get_current_user 역할.
     *
     * X-Auth-Version: v2를 포함하여 JwtAuthenticationFilter가 DB 조회 없이 JWT Claims로만 인증하도록 한다.
     * 이로써 테스트 트랜잭션 격리 문제를 피하고, MyPageController의
     * @AuthenticationPrincipal PrincipalUser에서 loginId/userId를 올바르게 추출한다.
     *
     * @param userId  사용자 PK (JWT subject)
     * @param role    역할 (예: "ROLE_USER", "ROLE_ADMIN")
     * @return Authorization: Bearer {token} + X-Auth-Version: v2 헤더
     */
    protected HttpHeaders createAuthHeader(Long userId, String role) {
        // 실제 JwtProvider로 서명된 토큰 생성 — 운영과 동일한 로직
        String token = jwtProvider.createAccessToken(userId, role, List.of());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        // v2: DB 조회 없이 JWT Claims만으로 인증 (성능 + 테스트 독립성)
        headers.add("X-Auth-Version", "v2");
        return headers;
    }

    /**
     * 테스트용 User 엔티티를 생성하여 DB에 저장한다.
     *
     * FastAPI의 test_session.add(user) 역할.
     * 저장 후 실제로 커밋되어 서버 HTTP 요청에서도 조회 가능하다.
     * 각 테스트에서 @AfterEach로 삭제해야 데이터가 누적되지 않는다.
     *
     * @param repo      UserRepository 빈
     * @param loginId   로그인 ID (유니크)
     * @param email     이메일 (유니크)
     * @param nickname  닉네임
     * @return 저장된 User 엔티티 (id 포함)
     */
    protected User createAndSaveUser(UserRepository repo, String loginId, String email, String nickname) {
        User user = User.builder()
                .loginId(loginId)
                .email(email)
                .nickname(nickname)
                .password("encoded_password")  // 실제 암호화된 값이 아니어도 테스트에서는 무관
                .userRoles(new ArrayList<>())   // 역할 없이 생성 (테스트 목적)
                .build();
        return repo.save(user);
    }
}
