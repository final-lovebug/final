package com.ubidict.backend.member.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.AuthErrorCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtProvider);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @DisplayName("Authorization 헤더가 없으면 인증 정보를 채우지 않고 체인을 통과시킨다.")
    @Test
    void doFilterInternal_withoutHeader() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.AUTH_EXCEPTION_ATTRIBUTE))
                .isNull();
        verify(filterChain).doFilter(request, response);
    }

    @DisplayName("유효한 access token이면 SecurityContext에 회원 id와 권한을 채운다.")
    @Test
    void doFilterInternal_withValidToken() throws Exception {
        // given
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn("1");
        given(claims.get("role", String.class)).willReturn("REGULAR");
        given(jwtProvider.parseAccessToken("valid-token")).willReturn(claims);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getPrincipal()).isEqualTo(1L);
        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_REGULAR");
        verify(filterChain).doFilter(request, response);
    }

    @DisplayName("Bearer 형식이 아니면 AUTH_TOKEN_INVALID를 요청 attribute에 남긴다.")
    @Test
    void doFilterInternal_notBearerScheme() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        assertAuthExceptionAttribute(request, AuthErrorCode.AUTH_TOKEN_INVALID);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @DisplayName("access token에 role 클레임이 없으면 AUTH_TOKEN_INVALID를 요청 attribute에 남긴다.")
    @Test
    void doFilterInternal_withoutRoleClaim() throws Exception {
        // given
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn("1");
        given(claims.get("role", String.class)).willReturn(null);
        given(jwtProvider.parseAccessToken("legacy-token")).willReturn(claims);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer legacy-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        assertAuthExceptionAttribute(request, AuthErrorCode.AUTH_TOKEN_INVALID);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @DisplayName("JwtProvider가 예외를 던지면 그 예외를 그대로 요청 attribute에 남긴다.")
    @Test
    void doFilterInternal_expiredToken() throws Exception {
        // given
        given(jwtProvider.parseAccessToken("expired-token"))
                .willThrow(new BusinessException(AuthErrorCode.AUTH_TOKEN_EXPIRED));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        assertAuthExceptionAttribute(request, AuthErrorCode.AUTH_TOKEN_EXPIRED);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private void assertAuthExceptionAttribute(MockHttpServletRequest request, AuthErrorCode expected) {
        Object attribute = request.getAttribute(JwtAuthenticationFilter.AUTH_EXCEPTION_ATTRIBUTE);
        assertThat(attribute).isInstanceOf(BusinessException.class);
        assertThat(((BusinessException) attribute).errorCode()).isEqualTo(expected);
    }
}
