package com.min.meow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI(Swagger) 전역 설정
 * - API 제목, 설명, 버전 정의
 * - JWT Bearer 인증 스키마 전역 등록 (Swagger UI "Authorize" 버튼)
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("Meow API")
                .description("고양이 테마 소셜 플랫폼 REST API. "
                        + "자랑글·실종글 CRUD, 댓글, 좋아요, 알림(SSE), 이미지 업로드(S3 Presigned URL)를 제공합니다.")
                .version("v1.0.0");
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT 액세스 토큰을 입력하세요. (Bearer 접두사 불필요)");
    }
}
