package com.ubidict.backend.member.infra.security;

import com.ubidict.backend.member.domain.OAuthProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Google OAuth2 로그인이 끝나면 호출된다. 회원 정보를 직접 응답하지 않고 1회용 교환 코드를
 * 발급해 프론트엔드로 리다이렉트한다({@code docs/API.md} "콜백 및 토큰 교환") — 프론트가 그
 * 코드로 {@code POST /api/auth/oauth/google/exchange}를 호출해 실제 토큰을 받는다.
 */
@Component
public class GoogleOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthExchangeCodeRedisRepository oAuthExchangeCodeRedisRepository;
    private final String frontendRedirectUri;

    public GoogleOAuth2LoginSuccessHandler(
            OAuthExchangeCodeRedisRepository oAuthExchangeCodeRedisRepository,
            @Value("${app.oauth.frontend-redirect-uri}") String frontendRedirectUri) {
        this.oAuthExchangeCodeRedisRepository = oAuthExchangeCodeRedisRepository;
        this.frontendRedirectUri = frontendRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String code = UUID.randomUUID().toString();

        oAuthExchangeCodeRedisRepository.save(
                code,
                new OAuthExchangeEntry(
                        oAuth2User.getAttribute("email"),
                        oAuth2User.getAttribute("name"),
                        OAuthProvider.GOOGLE,
                        oAuth2User.getName()));

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("code", code)
                .build()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }
}
