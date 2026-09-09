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
import com.ubidict.backend.member.implement.MemberRegistrar;
import com.ubidict.backend.member.implement.MemberStatusValidator;
import com.ubidict.backend.member.implement.TokenIssuer;
import com.ubidict.backend.member.implement.TokenPair;
import com.ubidict.backend.member.service.model.OAuthLoginCommand;
import com.ubidict.backend.member.service.model.TokenPairResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberOAuthLoginServiceTest {

    private final MemberRegistrar memberRegistrar = mock(MemberRegistrar.class);
    private final MemberStatusValidator memberStatusValidator = mock(MemberStatusValidator.class);
    private final TokenIssuer tokenIssuer = mock(TokenIssuer.class);
    private final MemberOAuthLoginService memberOAuthLoginService =
            new MemberOAuthLoginService(memberRegistrar, memberStatusValidator, tokenIssuer);

    @DisplayName("로그인에 성공하면 토큰 쌍을 반환한다.")
    @Test
    void login() {
        // given
        Member member = Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        given(memberRegistrar.findOrRegister("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1"))
                .willReturn(member);
        given(tokenIssuer.issue(member.getId(), member.getRole()))
                .willReturn(new TokenPair("access-token", "refresh-token", member.getRole()));

        // when
        TokenPairResult result = memberOAuthLoginService.login(
                new OAuthLoginCommand("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1"));

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @DisplayName("로그인할 수 없는 상태의 회원이면 토큰을 발급하지 않는다.")
    @Test
    void login_notLoginable() {
        // given
        Member member = Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        given(memberRegistrar.findOrRegister("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1"))
                .willReturn(member);
        doThrow(new BusinessException(MemberErrorCode.MEMBER_LOGIN_NOT_ALLOWED))
                .when(memberStatusValidator)
                .validateLoginable(member);

        // when & then
        assertThatThrownBy(() -> memberOAuthLoginService.login(
                        new OAuthLoginCommand("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1")))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                        .isEqualTo(MemberErrorCode.MEMBER_LOGIN_NOT_ALLOWED));
        verify(tokenIssuer, never()).issue(any(), any());
    }
}
