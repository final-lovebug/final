package com.ubidict.backend.member.infra.security;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.exception.AuthErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.servlet.HandlerExceptionResolver;

class JwtAuthenticationEntryPointTest {

    private final HandlerExceptionResolver handlerExceptionResolver = mock(HandlerExceptionResolver.class);
    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(handlerExceptionResolver);

    @DisplayName("필터가 남겨둔 실패 원인이 있으면 그 예외 그대로 처리한다.")
    @Test
    void commence_withAttribute() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        BusinessException exception = new BusinessException(AuthErrorCode.AUTH_TOKEN_EXPIRED);
        request.setAttribute(JwtAuthenticationFilter.AUTH_EXCEPTION_ATTRIBUTE, exception);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        entryPoint.commence(request, response, new BadCredentialsException("no auth"));

        // then
        verify(handlerExceptionResolver).resolveException(eq(request), eq(response), isNull(), eq(exception));
    }

    @DisplayName("필터가 남겨둔 실패 원인이 없으면 AUTH_TOKEN_MISSING으로 처리한다.")
    @Test
    void commence_withoutAttribute() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        entryPoint.commence(request, response, new BadCredentialsException("no auth"));

        // then
        verify(handlerExceptionResolver)
                .resolveException(
                        eq(request),
                        eq(response),
                        isNull(),
                        argThat(exception -> exception instanceof BusinessException be
                                && be.errorCode() == AuthErrorCode.AUTH_TOKEN_MISSING));
    }
}
