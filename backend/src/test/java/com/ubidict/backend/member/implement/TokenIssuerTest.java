package com.ubidict.backend.member.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.member.domain.MemberRole;
import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.member.infra.security.RefreshTokenHasher;
import com.ubidict.backend.member.infra.security.RefreshTokenRedisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenIssuerTest {

    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final RefreshTokenHasher refreshTokenHasher = mock(RefreshTokenHasher.class);
    private final RefreshTokenRedisRepository refreshTokenRedisRepository = mock(RefreshTokenRedisRepository.class);
    private final TokenIssuer tokenIssuer =
            new TokenIssuer(jwtProvider, refreshTokenHasher, refreshTokenRedisRepository);

    @DisplayName("토큰을 발급하면 기존 토큰을 지우고 새 토큰을 current로 저장한다.")
    @Test
    void issue() {
        // given
        given(jwtProvider.issueAccessToken(1L, MemberRole.REGULAR)).willReturn("access-token");
        given(jwtProvider.issueRefreshToken(eq(1L), anyString())).willReturn("refresh-token");
        given(refreshTokenHasher.hash("refresh-token")).willReturn("refresh-token-hash");

        // when
        TokenPair tokenPair = tokenIssuer.issue(1L, MemberRole.REGULAR);

        // then
        assertThat(tokenPair.accessToken()).isEqualTo("access-token");
        assertThat(tokenPair.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRedisRepository).deleteAll(1L);
        verify(refreshTokenRedisRepository).saveCurrent(1L, "refresh-token-hash");
    }
}
