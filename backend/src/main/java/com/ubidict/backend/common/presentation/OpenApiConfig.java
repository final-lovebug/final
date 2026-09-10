package com.ubidict.backend.common.presentation;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI에 access token을 붙여넣을 수 있는 "Authorize" 버튼을 추가한다. 이게 없으면 인증이
 * 필요한 API를 Swagger UI의 "Try it out"으로 테스트할 때마다 매번 다른 방법으로 헤더를 실어야
 * 한다. 전역 {@link SecurityRequirement}로 등록해 모든 엔드포인트에 적용되게 한다({@code /api/auth/**}
 * 처럼 실제로 인증이 필요 없는 엔드포인트도 버튼은 뜨지만, 토큰 없이 호출해도 서버가 permitAll로
 * 통과시키므로 문제없다).
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_AUTH,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
