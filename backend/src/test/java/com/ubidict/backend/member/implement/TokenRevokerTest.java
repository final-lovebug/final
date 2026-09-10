package com.ubidict.backend.member.implement;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.member.infra.security.RefreshTokenRedisRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenRevokerTest {

    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final RefreshTokenRedisRepository refreshTokenRedisRepository = mock(RefreshTokenRedisRepository.class);
    private final TokenRevoker tokenRevoker = new TokenRevoker(jwtProvider, refreshTokenRedisRepository);

    @DisplayName("로그아웃하면 해당 회원의 저장된 리프레시 토큰을 전부 지운다.")
    @Test
    void revoke() {
        // given
        Claims claims = mock(Claims.class);
        given(jwtProvider.parseRefreshToken("refresh-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("1");

        // when
        tokenRevoker.revoke("refresh-token");

        // then
        verify(refreshTokenRedisRepository).deleteAll(1L);
    }
}
