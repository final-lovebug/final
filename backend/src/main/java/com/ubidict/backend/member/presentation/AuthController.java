package com.ubidict.backend.member.presentation;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.exception.AuthErrorCode;
import com.ubidict.backend.member.presentation.dto.OAuthExchangeRequest;
import com.ubidict.backend.member.presentation.dto.TokenResponse;
import com.ubidict.backend.member.service.LogoutService;
import com.ubidict.backend.member.service.MemberOAuthLoginService;
import com.ubidict.backend.member.service.TokenReissueService;
import com.ubidict.backend.member.service.model.TokenPairResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인과 토큰 수명 관리를 담당한다({@code docs/API.md} Auth API). 이 경로는
 * {@code SecurityConfig}에서 인증 없이 호출되도록 열려 있다 — 재발급·로그아웃은 access
 * token이 이미 만료된 상태에서 호출되기 때문이다.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final MemberOAuthLoginService memberOAuthLoginService;
    private final TokenReissueService tokenReissueService;
    private final LogoutService logoutService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    @PostMapping("/oauth/google/exchange")
    public ResponseEntity<TokenResponse> exchange(@Valid @RequestBody OAuthExchangeRequest request) {
        TokenPairResult result = memberOAuthLoginService.loginByExchangeCode(request.code());
        return withRefreshCookie(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = RefreshTokenCookieProvider.COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new BusinessException(AuthErrorCode.AUTH_TOKEN_MISSING);
        }

        TokenPairResult result = tokenReissueService.reissue(refreshToken);
        return withRefreshCookie(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookieProvider.COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken != null) {
            logoutService.logout(refreshToken);
        }

        ResponseCookie expiredCookie = refreshTokenCookieProvider.expire();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .build();
    }

    private ResponseEntity<TokenResponse> withRefreshCookie(TokenPairResult result) {
        ResponseCookie cookie = refreshTokenCookieProvider.issue(result.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(TokenResponse.from(result));
    }
}
