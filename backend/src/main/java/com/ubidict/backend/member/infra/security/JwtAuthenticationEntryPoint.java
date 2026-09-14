package com.ubidict.backend.member.infra.security;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 인증되지 않은 요청이 보호된 API에 닿으면 Spring Security가 호출한다.
 *
 * <p>{@link JwtAuthenticationFilter}가 남겨둔 구체적인 실패 원인(서명 불일치·만료 등)이 있으면
 * 그대로, 없으면(애초에 토큰이 없었던 경우) {@link AuthErrorCode#AUTH_TOKEN_MISSING}으로
 * 처리한다. Security 필터 체인은 {@code DispatcherServlet} 앞단이라 {@code GlobalExceptionHandler}가
 * 직접 못 받으므로, {@link HandlerExceptionResolver}로 넘겨 BusinessException 처리 경로를
 * 그대로 재사용한다({@code docs/EXCEPTION.md} 에러 응답 형식 통일).
 *
 * <p>{@code HandlerExceptionResolver}는 서블릿 웹 MVC가 있어야만 존재하는 빈이라,
 * {@code webEnvironment = WebEnvironment.NONE}으로 띄우는 서비스 통합 테스트
 * (예: {@code IntegrationTestSupport})에서는 이 빈 자체가 없다. 그런 컨텍스트까지 억지로
 * 만들려다 실패하지 않도록 서블릿 웹 애플리케이션일 때만 등록한다.
 */
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver handlerExceptionResolver;

    public JwtAuthenticationEntryPoint(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
        Object attribute = request.getAttribute(JwtAuthenticationFilter.AUTH_EXCEPTION_ATTRIBUTE);
        BusinessException exception = attribute instanceof BusinessException businessException
                ? businessException
                : new BusinessException(AuthErrorCode.AUTH_TOKEN_MISSING);

        handlerExceptionResolver.resolveException(request, response, null, exception);
    }
}
