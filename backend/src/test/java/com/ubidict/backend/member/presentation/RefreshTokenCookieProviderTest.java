package com.ubidict.backend.member.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshTokenCookieProviderTest {

    @DisplayName("발급하면 HttpOnly·Secure·SameSite가 설정된 쿠키를 만든다.")
    @Test
    void issue() {
        // given
        RefreshTokenCookieProvider cookieProvider = new RefreshTokenCookieProvider(Duration.ofDays(7), true);

        // when
        ResponseCookie cookie = cookieProvider.issue("refresh-token");

        // then
        assertThat(cookie.getName()).isEqualTo(RefreshTokenCookieProvider.COOKIE_NAME);
        assertThat(cookie.getValue()).isEqualTo("refresh-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(7));
    }

    @DisplayName("secure 설정을 false로 주면 Secure가 꺼진 쿠키를 만든다(로컬 프로필).")
    @Test
    void issue_secureDisabled() {
        // given
        RefreshTokenCookieProvider cookieProvider = new RefreshTokenCookieProvider(Duration.ofDays(7), false);

        // when
        ResponseCookie cookie = cookieProvider.issue("refresh-token");

        // then
        assertThat(cookie.isSecure()).isFalse();
    }

    @DisplayName("만료시키면 값이 비어 있고 즉시 만료되는 쿠키를 만든다.")
    @Test
    void expire() {
        // given
        RefreshTokenCookieProvider cookieProvider = new RefreshTokenCookieProvider(Duration.ofDays(7), true);

        // when
        ResponseCookie cookie = cookieProvider.expire();

        // then
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
    }
}
