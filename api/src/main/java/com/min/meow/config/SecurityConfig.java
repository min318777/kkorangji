package com.min.meow.config;


import com.min.meow.user.filter.CustomLogoutFilter;
import com.min.meow.user.jwt.JwtFilter;
import com.min.meow.user.jwt.JwtUtil;
import com.min.meow.user.filter.CustomLoginFilter;
import com.min.meow.user.oauth2.CustomSuccessHandler;
import com.min.meow.user.repository.UserRepository;
import com.min.meow.user.service.CustomOauth2UserService;
import com.min.meow.user.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final JwtUtil jwtUtil;

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    private final CustomOauth2UserService customOauth2UserService;
    private final CustomSuccessHandler customSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{

        CustomLoginFilter customLoginFilter = new CustomLoginFilter(authenticationManager(authenticationConfiguration), jwtUtil, refreshTokenService);
        customLoginFilter.setFilterProcessesUrl("/login");
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
                                "/login",
                                "/api/users/join",
                                "/api/reissue",
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
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/meow/boast-cat/*/view",
                                "/api/meow/lost-cat/*/view").permitAll()
                        // 알림 목록 조회만 인증 없이 가능 (GET 요청만)
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/notice",
                                "/api/notice/status").permitAll()
                        // 관리자 권한 필요
                        .requestMatchers("/admin").hasRole("ADMIN")
                        // 나머지 모든 요청은 인증 필요 (알림 구독, 읽음 처리 등)
                        .anyRequest().authenticated());
        // JwtFilter는 LoginFilter 뒤에 등록하여, 로그인 성공 후 JWT 인증 처리
        http.
                addFilterAfter(new JwtFilter(jwtUtil, userRepository), UsernamePasswordAuthenticationFilter.class);
        http.
                addFilterAt(customLoginFilter, UsernamePasswordAuthenticationFilter.class);
        http.
                addFilterBefore(new CustomLogoutFilter(refreshTokenService, jwtUtil), LogoutFilter.class);
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
