package com.ubidict.backend.member.presentation;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * refresh token을 {@code Set-Cookie}(HttpOnly, Secure, SameSite)로 내려주기 위한 쿠키를
 * 만든다({@code docs/API.md}). {@code secure}는 로컬(http)에서는 꺼야 브라우저가 쿠키를
 * 돌려보내므로 프로필별로 다르다 — {@code application-local.properties} 참고.
 */
@Component
public class RefreshTokenCookieProvider {

    public static final String COOKIE_NAME = "refreshToken";

    private final Duration maxAge;
    private final boolean secure;

    public RefreshTokenCookieProvider(
            @Value("${app.jwt.refresh-token-validity}") Duration maxAge,
            @Value("${app.auth.cookie.secure:true}") boolean secure) {
        this.maxAge = maxAge;
        this.secure = secure;
    }

    public ResponseCookie issue(String refreshToken) {
        return build(refreshToken, maxAge);
    }

    public ResponseCookie expire() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }
}
