package com.ubidict.backend.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * 로그인한 회원 식별자를 SecurityContext 의 principal 에 넣는다.
 *
 * <p>컨트롤러 테스트는 {@code @AutoConfigureMockMvc(addFilters = false)}로 필터 체인을 우회하므로
 * {@link org.springframework.security.core.annotation.AuthenticationPrincipal}이 꺼낼 principal 을 테스트가 직접 채워야 한다.
 * {@code JwtAuthenticationFilter}가 넣는 것과 같은 형태({@link Long} memberId)로 맞춘다 — 타입이 어긋나면
 * {@code @AuthenticationPrincipal}은 예외 대신 조용히 {@code null}을 넣는다.
 *
 * <p>{@code @WithMockUser}를 쓰지 않는 이유는 그쪽 principal 이 {@code UserDetails}라서 이 프로젝트의 계약과 다르기 때문이다.
 * 인증·인가 흐름 자체는 {@code SecurityConfigTest}가 실제 필터 체인으로 검증한다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithLoginMemberSecurityContextFactory.class)
public @interface WithLoginMember {

    /** 로그인한 회원 식별자. */
    long value() default 1L;

    /** 권한. {@code JwtAuthenticationFilter}가 붙이는 {@code ROLE_} 접두사까지 그대로 맞춘다. */
    String role() default "REGULAR";
}
