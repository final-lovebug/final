package com.ubidict.backend.member.infra.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 서명·수명 설정. {@code secret}은 반드시 환경변수(`${JWT_SECRET}`)로 주입하며
 * 저장소에 실제 값을 커밋하지 않는다.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration accessTokenValidity, Duration refreshTokenValidity) {}
