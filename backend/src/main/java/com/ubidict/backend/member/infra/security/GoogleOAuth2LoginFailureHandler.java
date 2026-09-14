package com.ubidict.backend.member.infra.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Google OAuth2 로그인이 실패하면(동의 거부, provider 오류 등) 호출된다. API로 노출된
 * 엔드포인트가 아니라 브라우저가 직접 오가는 리다이렉트 흐름이라, 에러 JSON 대신 프론트엔드로
 * {@code error} 쿼리 파라미터를 붙여 되돌려보낸다.
 */
@Component
public class GoogleOAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final String frontendRedirectUri;

    public GoogleOAuth2LoginFailureHandler(@Value("${app.oauth.frontend-redirect-uri}") String frontendRedirectUri) {
        this.frontendRedirectUri = frontendRedirectUri;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("error", "oauth_failed")
                .build()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }
}
