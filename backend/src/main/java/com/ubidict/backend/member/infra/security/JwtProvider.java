package com.ubidict.backend.member.infra.security;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.AuthErrorCode;
import com.ubidict.backend.member.domain.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * JWT access/refresh 토큰을 발급·검증한다. 서명 키는 {@link JwtProperties#secret()}에서
 * 얻으며, 이 값은 항상 환경변수로 주입된다.
 */
@Component
public class JwtProvider {

    private final SecretKey key;
    private final JwtProperties jwtProperties;

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(Long memberId, MemberRole role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.accessTokenValidity())))
                .signWith(key)
                .compact();
    }

    public String issueRefreshToken(Long memberId, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.refreshTokenValidity())))
                .signWith(key)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        return parse(token);
    }

    public Claims parseRefreshToken(String token) {
        return parse(token);
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(AuthErrorCode.AUTH_TOKEN_EXPIRED, e.getMessage(), e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.AUTH_TOKEN_INVALID, e.getMessage(), e);
        }
    }
}
