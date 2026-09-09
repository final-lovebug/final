package com.ubidict.backend.member.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.AuthErrorCode;
import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.member.infra.security.RefreshTokenHasher;
import com.ubidict.backend.member.infra.security.RefreshTokenRedisRepository;
import io.jsonwebtoken.Claims;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 리프레시 토큰으로 access/refresh 토큰 쌍을 재발급한다. 제시된 토큰이 현재
 * 유효한 토큰(current)이거나 재발급 유예 시간(10초) 안의 직전 토큰(grace)과
 * 일치해야 한다 — 여러 탭이 같은 토큰으로 동시에 재발급을 요청해도 모두
 * 성공하게 하기 위함이다. 사용된 토큰은 다시 grace로 내려가 10초간만 더
 * 유효하고, 그 뒤엔 어느 쪽과도 일치하지 않아 거절된다.
 *
 * <p>권한(role)은 토큰이 아니라 저장된 회원 정보에서 다시 읽는다 — 권한
 * 변경이 재발급 시점에 곧바로 반영되게 하기 위함이다({@code docs/API.md} 참고).
 *
 * <p>탈퇴·정지 회원의 재발급 차단은 이번 단계 범위 밖이다 — {@code MemberStatusValidator}가
 * 추가되는 다음 단계(로그인 서비스)에서 통합한다.
 */
@RequiredArgsConstructor
@Component
public class TokenRefresher {

    private final JwtProvider jwtProvider;
    private final MemberReader memberReader;
    private final RefreshTokenHasher refreshTokenHasher;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    public TokenPair refresh(String refreshToken) {
        Claims claims = jwtProvider.parseRefreshToken(refreshToken);
        Long memberId = Long.valueOf(claims.getSubject());
        String presentedHash = refreshTokenHasher.hash(refreshToken);

        boolean matched = refreshTokenRedisRepository
                        .findCurrent(memberId)
                        .filter(presentedHash::equals)
                        .isPresent()
                || refreshTokenRedisRepository
                        .findGrace(memberId)
                        .filter(presentedHash::equals)
                        .isPresent();
        if (!matched) {
            throw new BusinessException(AuthErrorCode.AUTH_TOKEN_INVALID);
        }

        Member member = memberReader.read(memberId);
        String newAccessToken = jwtProvider.issueAccessToken(memberId, member.getRole());
        String newRefreshToken =
                jwtProvider.issueRefreshToken(memberId, UUID.randomUUID().toString());

        refreshTokenRedisRepository.saveGrace(memberId, presentedHash);
        refreshTokenRedisRepository.saveCurrent(memberId, refreshTokenHasher.hash(newRefreshToken));

        return new TokenPair(newAccessToken, newRefreshToken);
    }
}
