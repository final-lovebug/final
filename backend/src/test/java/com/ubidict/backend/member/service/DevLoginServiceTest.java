package com.ubidict.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.MemberErrorCode;
import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.implement.MemberReader;
import com.ubidict.backend.member.implement.MemberStatusValidator;
import com.ubidict.backend.member.implement.TokenIssuer;
import com.ubidict.backend.member.implement.TokenPair;
import com.ubidict.backend.member.service.model.TokenPairResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DevLoginServiceTest {

    private final MemberReader memberReader = mock(MemberReader.class);
    private final MemberStatusValidator memberStatusValidator = mock(MemberStatusValidator.class);
    private final TokenIssuer tokenIssuer = mock(TokenIssuer.class);
    private final DevLoginService devLoginService =
            new DevLoginService(memberReader, memberStatusValidator, tokenIssuer);

    @DisplayName("존재하고 로그인 가능한 회원이면 토큰 쌍을 반환한다.")
    @Test
    void loginAs() {
        // given
        Member member = Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        given(memberReader.read(1L)).willReturn(member);
        given(tokenIssuer.issue(member.getId(), member.getRole()))
                .willReturn(new TokenPair("access-token", "refresh-token", member.getRole()));

        // when
        TokenPairResult result = devLoginService.loginAs(1L);

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @DisplayName("존재하지 않는 회원이면 실패한다.")
    @Test
    void loginAs_notFound() {
        // given
        given(memberReader.read(999L)).willThrow(new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> devLoginService.loginAs(999L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                        .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        verify(tokenIssuer, never()).issue(any(), any());
    }

    @DisplayName("로그인할 수 없는 상태의 회원이면 토큰을 발급하지 않는다.")
    @Test
    void loginAs_notLoginable() {
        // given
        Member member = Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        given(memberReader.read(1L)).willReturn(member);
        doThrow(new BusinessException(MemberErrorCode.MEMBER_LOGIN_NOT_ALLOWED))
                .when(memberStatusValidator)
                .validateLoginable(member);

        // when & then
        assertThatThrownBy(() -> devLoginService.loginAs(1L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                        .isEqualTo(MemberErrorCode.MEMBER_LOGIN_NOT_ALLOWED));
        verify(tokenIssuer, never()).issue(any(), any());
    }
}
