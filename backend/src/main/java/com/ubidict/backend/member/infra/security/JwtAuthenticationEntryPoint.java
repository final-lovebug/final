package com.ubidict.backend.member.infra.security;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
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
 */
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
