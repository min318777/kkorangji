package com.min.meow.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


// 리액트와 연동시 cors에러 방지 클래스
@Configuration
public class CorsMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**")
                .exposedHeaders("Set-Cookie")
                .allowedOrigins("http://localhost:3000");
    }
}
