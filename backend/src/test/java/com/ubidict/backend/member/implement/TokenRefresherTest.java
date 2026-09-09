package com.ubidict.backend.member.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.AuthErrorCode;
import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.member.infra.security.RefreshTokenHasher;
import com.ubidict.backend.member.infra.security.RefreshTokenRedisRepository;
import io.jsonwebtoken.Claims;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenRefresherTest {

    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final MemberReader memberReader = mock(MemberReader.class);
    private final RefreshTokenHasher refreshTokenHasher = mock(RefreshTokenHasher.class);
    private final RefreshTokenRedisRepository refreshTokenRedisRepository = mock(RefreshTokenRedisRepository.class);
    private final TokenRefresher tokenRefresher =
            new TokenRefresher(jwtProvider, memberReader, refreshTokenHasher, refreshTokenRedisRepository);

    private final Claims claims = mock(Claims.class);

    @DisplayName("제시한 토큰이 current와 일치하면 재발급에 성공한다.")
    @Test
    void refresh_matchesCurrent() {
        // given
        given(jwtProvider.parseRefreshToken("presented-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("1");
        given(refreshTokenHasher.hash("presented-token")).willReturn("presented-hash");
        given(refreshTokenRedisRepository.findCurrent(1L)).willReturn(Optional.of("presented-hash"));
        given(refreshTokenRedisRepository.findGrace(1L)).willReturn(Optional.empty());
        given(memberReader.read(1L))
                .willReturn(Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1"));
        given(jwtProvider.issueAccessToken(eq(1L), any())).willReturn("new-access-token");
        given(jwtProvider.issueRefreshToken(eq(1L), anyString())).willReturn("new-refresh-token");
        given(refreshTokenHasher.hash("new-refresh-token")).willReturn("new-refresh-hash");

        // when
        TokenPair tokenPair = tokenRefresher.refresh("presented-token");

        // then
        assertThat(tokenPair.accessToken()).isEqualTo("new-access-token");
        assertThat(tokenPair.refreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenRedisRepository).saveGrace(1L, "presented-hash");
        verify(refreshTokenRedisRepository).saveCurrent(1L, "new-refresh-hash");
    }

    @DisplayName("제시한 토큰이 grace와 일치해도 재발급에 성공한다(동시 탭 재발급 허용).")
    @Test
    void refresh_matchesGrace() {
        // given
        given(jwtProvider.parseRefreshToken("presented-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("1");
        given(refreshTokenHasher.hash("presented-token")).willReturn("presented-hash");
        given(refreshTokenRedisRepository.findCurrent(1L)).willReturn(Optional.of("other-hash"));
        given(refreshTokenRedisRepository.findGrace(1L)).willReturn(Optional.of("presented-hash"));
        given(memberReader.read(1L))
                .willReturn(Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1"));
        given(jwtProvider.issueAccessToken(eq(1L), any())).willReturn("new-access-token");
        given(jwtProvider.issueRefreshToken(eq(1L), anyString())).willReturn("new-refresh-token");
        given(refreshTokenHasher.hash("new-refresh-token")).willReturn("new-refresh-hash");

        // when
        TokenPair tokenPair = tokenRefresher.refresh("presented-token");

        // then
        assertThat(tokenPair.accessToken()).isEqualTo("new-access-token");
    }

    @DisplayName("제시한 토큰이 current, grace 어느 쪽과도 일치하지 않으면 예외가 발생한다.")
    @Test
    void refresh_noMatch() {
        // given
        given(jwtProvider.parseRefreshToken("presented-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("1");
        given(refreshTokenHasher.hash("presented-token")).willReturn("presented-hash");
        given(refreshTokenRedisRepository.findCurrent(1L)).willReturn(Optional.of("other-hash"));
        given(refreshTokenRedisRepository.findGrace(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tokenRefresher.refresh("presented-token"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                        .isEqualTo(AuthErrorCode.AUTH_TOKEN_INVALID));
    }
}
