package com.min.meow.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.meow.notification.sse.SseEmitterManager;
import com.min.meow.security.filter.CustomLogoutFilter;
import com.min.meow.security.jwt.JwtAuthenticationFilter;
import com.min.meow.security.jwt.JwtProvider;
import com.min.meow.security.filter.CustomLoginFilter;
import com.min.meow.security.oauth2.CustomSuccessHandler;
import com.min.meow.security.service.PermissionCacheService;
import com.min.meow.user.repository.UserRepository;
import com.min.meow.security.oauth2.CustomOauth2UserService;
import com.min.meow.security.service.RefreshTokenService;
import com.min.meow.user.service.DauService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // @PreAuthorize 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final JwtProvider jwtProvider;

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final PermissionCacheService permissionCacheService;

    private final CustomOauth2UserService customOauth2UserService;
    private final CustomSuccessHandler customSuccessHandler;
    private final ObjectMapper objectMapper;
    private final DauService dauService;
    private final SseEmitterManager sseEmitterManager;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{

        CustomLoginFilter customLoginFilter = new CustomLoginFilter(authenticationManager(authenticationConfiguration), jwtProvider, refreshTokenService, permissionCacheService, dauService, objectMapper);
        customLoginFilter.setFilterProcessesUrl("/api/users/login");
        http
                .cors((cors) -> cors.configurationSource(new CorsConfigurationSource() {
                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                        CorsConfiguration corsConfiguration = new CorsConfiguration();
                        corsConfiguration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "https://meow-front.vercel.app"));
                        corsConfiguration.setAllowedMethods(Collections.singletonList("*"));
                        corsConfiguration.setAllowCredentials(true);
                        corsConfiguration.setAllowedHeaders(Collections.singletonList("*"));
                        corsConfiguration.setMaxAge(3600L);
                        corsConfiguration.setExposedHeaders(Arrays.asList("Set-Cookie", "Authorization"));

                        return corsConfiguration;
                    }
                }));
        http
                .csrf((auth) -> auth.disable());
        http
                .formLogin((auth) -> auth.disable());
        http
                .httpBasic((auth) -> auth.disable());

        http.
                authorizeHttpRequests((auth) -> auth
                        // 인증 없이 접근 가능한 엔드포인트
                        .requestMatchers(
                                "/api/users/login",
                                "/api/users/join",
                                "/api/users/check-id",
                                "/api/users/check-nickname",
                                "/api/auth/token/refresh",
                                "/api/logout",
                                "/error",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                // 모니터링 엔드포인트 (Prometheus, Actuator)
                                "/actuator/**").permitAll()
                        // 게시글 및 댓글 조회는 인증 없이 가능 (GET 요청만)
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/meow/boast-cat",
                                "/api/meow/boast-cat/**",
                                "/api/meow/lost-cat",
                                "/api/meow/lost-cat/**").permitAll()
                        // 조회수 증가 API는 인증 없이 가능 (POST 요청)
                        // v2: /api/meow/boast-cat/{id}/view (원자적 업데이트)
                        // v1: /api/meow/boast-cat/v1/{id}/view (더티체킹 - 동시성 테스트용)
                        // v3: /api/meow/boast-cat/v3/{id}/view (Redis INCR 방식)
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/meow/boast-cat/*/view",
                                "/api/meow/boast-cat/v1/*/view",
                                "/api/meow/boast-cat/v3/*/view",
                                "/api/meow/lost-cat/*/view",
                                "/api/meow/lost-cat/v1/*/view",
                                "/api/meow/lost-cat/v3/*/view",
                                "/api/meow/boast-cat/v4/*/view") // 추가
                        .permitAll()
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/notifications/status").permitAll()
                        // 관리자 권한 필요
                        .requestMatchers("/admin").hasRole("ADMIN")
                        // 나머지 모든 요청은 인증 필요 (알림 구독, 읽음 처리 등)
                        .anyRequest().authenticated());
        http.
                addFilterBefore(new JwtAuthenticationFilter(jwtProvider, userRepository, objectMapper, permissionCacheService), UsernamePasswordAuthenticationFilter.class);
        http.
                addFilterAt(customLoginFilter, UsernamePasswordAuthenticationFilter.class);
        http.
                addFilterBefore(new CustomLogoutFilter(refreshTokenService, jwtProvider, sseEmitterManager), LogoutFilter.class);
        http.
                sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception{
        return authenticationConfiguration.getAuthenticationManager();
    }
}
