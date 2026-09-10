package com.ubidict.backend.member.implement;

import com.ubidict.backend.member.domain.MemberRole;
import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.member.infra.security.RefreshTokenHasher;
import com.ubidict.backend.member.infra.security.RefreshTokenRedisRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 로그인 성공 시 access/refresh 토큰 쌍을 새로 발급한다. 발급할 때마다 해당
 * 회원의 기존 리프레시 토큰(current/grace)을 전부 지우고 새로 저장해 "계정당
 * 활성 세션은 하나"라는 정책을 지킨다 — 다른 기기에서 로그인해 있던 세션은
 * 다음 재발급 시점에 실패하게 된다.
 */
@RequiredArgsConstructor
@Component
public class TokenIssuer {

    private final JwtProvider jwtProvider;
    private final RefreshTokenHasher refreshTokenHasher;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    public TokenPair issue(Long memberId, MemberRole role) {
        String accessToken = jwtProvider.issueAccessToken(memberId, role);
        String refreshToken =
                jwtProvider.issueRefreshToken(memberId, UUID.randomUUID().toString());

        refreshTokenRedisRepository.deleteAll(memberId);
        refreshTokenRedisRepository.saveCurrent(memberId, refreshTokenHasher.hash(refreshToken));

        return new TokenPair(accessToken, refreshToken, role);
    }
}
