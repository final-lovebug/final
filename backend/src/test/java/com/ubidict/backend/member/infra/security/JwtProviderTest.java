package com.ubidict.backend.member.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.AuthErrorCode;
import com.ubidict.backend.member.domain.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final String SECRET = "unit-test-jwt-secret-key-please-do-not-use-in-production-0123456789";

    private final JwtProperties jwtProperties = new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(7));
    private final JwtProvider jwtProvider = new JwtProvider(jwtProperties);

    @DisplayName("액세스 토큰을 발급하고 파싱하면 회원 id와 역할을 얻을 수 있다.")
    @Test
    void issueAndParseAccessToken() {
        // when
        String accessToken = jwtProvider.issueAccessToken(1L, MemberRole.REGULAR);
        Claims claims = jwtProvider.parseAccessToken(accessToken);

        // then
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("role", String.class)).isEqualTo("REGULAR");
    }

    @DisplayName("리프레시 토큰을 발급하고 파싱하면 회원 id와 jti를 얻을 수 있다.")
    @Test
    void issueAndParseRefreshToken() {
        // when
        String refreshToken = jwtProvider.issueRefreshToken(1L, "jti-1");
        Claims claims = jwtProvider.parseRefreshToken(refreshToken);

        // then
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.getId()).isEqualTo("jti-1");
    }

    @DisplayName("만료된 토큰을 파싱하면 예외가 발생한다.")
    @Test
    void parse_expired() {
        // given
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Instant expiredAt = Instant.now().minus(Duration.ofMinutes(1));
        String expiredToken = Jwts.builder()
                .subject("1")
                .issuedAt(Date.from(expiredAt.minus(Duration.ofMinutes(30))))
                .expiration(Date.from(expiredAt))
                .signWith(key)
                .compact();

        // when & then
        assertThatThrownBy(() -> jwtProvider.parseAccessToken(expiredToken))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                        .isEqualTo(AuthErrorCode.AUTH_TOKEN_EXPIRED));
    }

    @DisplayName("서명이 다른 토큰을 파싱하면 예외가 발생한다.")
    @Test
    void parse_invalidSignature() {
        // given
        SecretKey otherKey = Keys.hmacShaKeyFor("other-unit-test-secret-key-0123456789-abcdefghijklmnop".getBytes());
        String tokenSignedWithOtherKey = Jwts.builder()
                .subject("1")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(30))))
                .signWith(otherKey)
                .compact();

        // when & then
        assertThatThrownBy(() -> jwtProvider.parseAccessToken(tokenSignedWithOtherKey))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                        .isEqualTo(AuthErrorCode.AUTH_TOKEN_INVALID));
    }
}
