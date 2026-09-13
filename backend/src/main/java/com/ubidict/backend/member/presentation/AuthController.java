package com.ubidict.backend.member.presentation;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.exception.AuthErrorCode;
import com.ubidict.backend.member.presentation.dto.CompleteRegistrationRequest;
import com.ubidict.backend.member.presentation.dto.OAuthExchangeRequest;
import com.ubidict.backend.member.presentation.dto.OAuthExchangeResponse;
import com.ubidict.backend.member.presentation.dto.TokenResponse;
import com.ubidict.backend.member.service.LogoutService;
import com.ubidict.backend.member.service.MemberOAuthLoginService;
import com.ubidict.backend.member.service.TokenReissueService;
import com.ubidict.backend.member.service.model.LoginSucceeded;
import com.ubidict.backend.member.service.model.OAuthExchangeOutcome;
import com.ubidict.backend.member.service.model.RegistrationRequired;
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

    /**
     * 이미 가입된 회원이면 로그인시키고, 최초 로그인(신규 식별자)이면 {@code Member} row를
     * 만들지 않고 등록 토큰만 응답한다 — 프론트가 이어서
     * {@code POST /api/auth/oauth/google/complete-registration}을 호출해야 한다
     * ({@code docs/API.md} "콜백 및 토큰 교환").
     */
    @PostMapping("/oauth/google/exchange")
    public ResponseEntity<OAuthExchangeResponse> exchange(@Valid @RequestBody OAuthExchangeRequest request) {
        OAuthExchangeOutcome outcome = memberOAuthLoginService.loginByExchangeCode(request.code());

        return switch (outcome) {
            case LoginSucceeded loginSucceeded -> {
                TokenPairResult tokenPair = loginSucceeded.tokenPair();
                ResponseCookie cookie = refreshTokenCookieProvider.issue(tokenPair.refreshToken());
                yield ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, cookie.toString())
                        .body(OAuthExchangeResponse.loggedIn(tokenPair));
            }
            case RegistrationRequired registrationRequired ->
                ResponseEntity.ok(OAuthExchangeResponse.registrationRequired(registrationRequired.registrationToken()));
        };
    }

    /**
     * 닉네임 온보딩을 마무리하고 로그인을 완료한다({@code docs/API.md} "닉네임 등록 완료").
     */
    @PostMapping("/oauth/google/complete-registration")
    public ResponseEntity<TokenResponse> completeRegistration(@Valid @RequestBody CompleteRegistrationRequest request) {
        TokenPairResult result =
                memberOAuthLoginService.completeRegistration(request.registrationToken(), request.displayName());
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
