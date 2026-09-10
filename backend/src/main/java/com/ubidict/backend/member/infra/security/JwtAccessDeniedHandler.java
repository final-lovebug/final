package com.ubidict.backend.member.infra.security;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 인증은 됐지만 권한이 부족한 요청(예: REGULAR가 {@code /api/admin/**} 호출)을 처리한다.
 * {@link JwtAuthenticationEntryPoint}와 마찬가지로 {@link HandlerExceptionResolver}로 넘겨
 * BusinessException 처리 경로를 재사용한다.
 *
 * <p>{@link JwtAuthenticationEntryPoint}와 같은 이유로 서블릿 웹 애플리케이션일 때만 등록한다.
 */
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final HandlerExceptionResolver handlerExceptionResolver;

    public JwtAccessDeniedHandler(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) {
        handlerExceptionResolver.resolveException(
                request, response, null, new BusinessException(AuthErrorCode.AUTH_FORBIDDEN));
    }
}
