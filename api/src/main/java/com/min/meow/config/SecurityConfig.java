package com.min.meow.config;


import com.min.meow.user.jwt.CustomLogoutFilter;
import com.min.meow.user.jwt.JwtFilter;
import com.min.meow.user.jwt.JwtUtil;
import com.min.meow.user.jwt.CustomLoginFilter;
import com.min.meow.user.oauth2.CustomSuccessHandler;
import com.min.meow.user.repository.RefreshTokenRepository;
import com.min.meow.user.service.CustomOauth2UserService;
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
    private final RefreshTokenRepository refreshTokenRepository;

    private final CustomOauth2UserService customOauth2UserService;
    private final CustomSuccessHandler customSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{

        CustomLoginFilter customLoginFilter = new CustomLoginFilter(authenticationManager(authenticationConfiguration), jwtUtil, refreshTokenRepository);
        customLoginFilter.setFilterProcessesUrl("/api/users/login");
        http
                .cors((cors) -> cors.configurationSource(new CorsConfigurationSource() {
                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                        CorsConfiguration corsConfiguration = new CorsConfiguration();

                        corsConfiguration.setAllowedOrigins(Collections.singletonList("http://localhost:3000"));
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

        // oauth2
        http
                .oauth2Login((oauth2) -> oauth2
                        .userInfoEndpoint((userInfoEndpointConfig) -> userInfoEndpointConfig
                                .userService(customOauth2UserService))
                        .successHandler(customSuccessHandler)
                );

        http.
                authorizeHttpRequests((auth) -> auth
                        .requestMatchers( "api/users/**",
                                "api/meow/lost-cat/**",
                                "api/meow/boast-cat/**",
                                "api/user/reissue",
                                "api/notice",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**").permitAll()
                        .requestMatchers("/admin").hasRole("ADMIN")
                        .anyRequest().authenticated());
        /*
        http.
                addFilterAfter(new JwtFilter(jwtUtil), OAuth2LoginAuthenticationFilter.class);
        http.
                addFilterBefore(new JwtFilter(jwtUtil), LoginFilter.class);
        */

        // JwtFilter는 LoginFilter 뒤에 등록하여, 로그인 성공 후 JWT 인증 처리
        http.
                addFilterAfter(new JwtFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);
        http.
                addFilterAt(customLoginFilter, UsernamePasswordAuthenticationFilter.class);
        http.
                addFilterBefore(new CustomLogoutFilter(refreshTokenRepository, jwtUtil), LogoutFilter.class);
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
