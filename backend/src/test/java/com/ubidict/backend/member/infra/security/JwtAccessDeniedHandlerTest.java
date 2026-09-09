package com.ubidict.backend.member.infra.security;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.AuthErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.HandlerExceptionResolver;

class JwtAccessDeniedHandlerTest {

    private final HandlerExceptionResolver handlerExceptionResolver = mock(HandlerExceptionResolver.class);
    private final JwtAccessDeniedHandler accessDeniedHandler = new JwtAccessDeniedHandler(handlerExceptionResolver);

    @DisplayName("권한이 부족하면 AUTH_FORBIDDEN으로 처리한다.")
    @Test
    void handle() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        accessDeniedHandler.handle(request, response, new AccessDeniedException("no authority"));

        // then
        verify(handlerExceptionResolver)
                .resolveException(
                        eq(request),
                        eq(response),
                        isNull(),
                        argThat(exception -> exception instanceof BusinessException be
                                && be.errorCode() == AuthErrorCode.AUTH_FORBIDDEN));
    }
}
