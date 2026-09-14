package com.ubidict.backend.member.implement;

import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.member.infra.security.RefreshTokenRedisRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 로그아웃 시 회원의 리프레시 토큰(current/grace)을 서버에서 폐기한다.
 */
@RequiredArgsConstructor
@Component
public class TokenRevoker {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    public void revoke(String refreshToken) {
        Claims claims = jwtProvider.parseRefreshToken(refreshToken);
        Long memberId = Long.valueOf(claims.getSubject());
        refreshTokenRedisRepository.deleteAll(memberId);
    }
}
