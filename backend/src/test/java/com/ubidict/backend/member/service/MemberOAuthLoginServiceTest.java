package com.ubidict.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.exception.AuthErrorCode;
import com.ubidict.backend.member.exception.MemberErrorCode;
import com.ubidict.backend.member.implement.MemberRegistrar;
import com.ubidict.backend.member.implement.MemberStatusValidator;
import com.ubidict.backend.member.implement.OAuthExchangeCodeResolver;
import com.ubidict.backend.member.implement.RegistrationTokenIssuer;
import com.ubidict.backend.member.implement.RegistrationTokenResolver;
import com.ubidict.backend.member.implement.TokenIssuer;
import com.ubidict.backend.member.implement.TokenPair;
import com.ubidict.backend.member.infra.security.OAuthExchangeEntry;
import com.ubidict.backend.member.service.model.LoginSucceeded;
import com.ubidict.backend.member.service.model.OAuthExchangeOutcome;
import com.ubidict.backend.member.service.model.RegistrationRequired;
import com.ubidict.backend.member.service.model.TokenPairResult;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberOAuthLoginServiceTest {

    private final MemberRegistrar memberRegistrar = mock(MemberRegistrar.class);
    private final MemberStatusValidator memberStatusValidator = mock(MemberStatusValidator.class);
    private final OAuthExchangeCodeResolver oAuthExchangeCodeResolver = mock(OAuthExchangeCodeResolver.class);
    private final RegistrationTokenIssuer registrationTokenIssuer = mock(RegistrationTokenIssuer.class);
    private final RegistrationTokenResolver registrationTokenResolver = mock(RegistrationTokenResolver.class);
    private final TokenIssuer tokenIssuer = mock(TokenIssuer.class);
    private final MemberOAuthLoginService memberOAuthLoginService = new MemberOAuthLoginService(
            memberRegistrar,
            memberStatusValidator,
            oAuthExchangeCodeResolver,
            registrationTokenIssuer,
            registrationTokenResolver,
            tokenIssuer);

    @DisplayName("이미 가입된 회원이면 교환 코드로 바로 로그인해 토큰 쌍을 반환한다.")
    @Test
    void loginByExchangeCode_existingMember() {
        // given
        given(oAuthExchangeCodeResolver.resolve("exchange-code"))
                .willReturn(new OAuthExchangeEntry("member@example.com", OAuthProvider.GOOGLE, "google-1"));
        Member member = Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        given(memberRegistrar.find(OAuthProvider.GOOGLE, "google-1")).willReturn(Optional.of(member));
        given(tokenIssuer.issue(member.getId(), member.getRole()))
                .willReturn(new TokenPair("access-token", "refresh-token", member.getRole()));

        // when
        OAuthExchangeOutcome outcome = memberOAuthLoginService.loginByExchangeCode("exchange-code");

        // then
        assertThat(outcome).isInstanceOfSatisfying(LoginSucceeded.class, loginSucceeded -> {
            assertThat(loginSucceeded.tokenPair().accessToken()).isEqualTo("access-token");
            assertThat(loginSucceeded.tokenPair().refreshToken()).isEqualTo("refresh-token");
        });
        verify(registrationTokenIssuer, never()).issue(any(), any(), any());
    }

    @DisplayName("처음 보는 식별자면 회원을 만들지 않고 등록 토큰만 발급한다.")
    @Test
    void loginByExchangeCode_newIdentity() {
        // given
        given(oAuthExchangeCodeResolver.resolve("exchange-code"))
                .willReturn(new OAuthExchangeEntry("member@example.com", OAuthProvider.GOOGLE, "google-1"));
        given(memberRegistrar.find(OAuthProvider.GOOGLE, "google-1")).willReturn(Optional.empty());
        given(registrationTokenIssuer.issue("member@example.com", OAuthProvider.GOOGLE, "google-1"))
                .willReturn("registration-token");

        // when
        OAuthExchangeOutcome outcome = memberOAuthLoginService.loginByExchangeCode("exchange-code");

        // then
        assertThat(outcome).isInstanceOfSatisfying(RegistrationRequired.class, registrationRequired -> assertThat(
                        registrationRequired.registrationToken())
                .isEqualTo("registration-token"));
        verify(memberRegistrar, never()).register(any(), any(), any(), any());
        verify(tokenIssuer, never()).issue(any(), any());
    }

    @DisplayName("교환 코드가 만료됐거나 이미 쓰였으면 로그인에 실패한다.")
    @Test
    void loginByExchangeCode_invalidCode() {
        // given
        given(oAuthExchangeCodeResolver.resolve("invalid-code"))
                .willThrow(new BusinessException(AuthErrorCode.AUTH_TOKEN_INVALID));

        // when & then
        assertThatThrownBy(() -> memberOAuthLoginService.loginByExchangeCode("invalid-code"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                        .isEqualTo(AuthErrorCode.AUTH_TOKEN_INVALID));
        verify(tokenIssuer, never()).issue(any(), any());
    }

    @DisplayName("닉네임 온보딩을 마무리하면 회원을 등록하고 토큰 쌍을 반환한다.")
    @Test
    void completeRegistration_success() {
        // given
        given(registrationTokenResolver.resolve("registration-token"))
                .willReturn(new OAuthExchangeEntry("member@example.com", OAuthProvider.GOOGLE, "google-1"));
        Member member = Member.create("member@example.com", "닉네임", OAuthProvider.GOOGLE, "google-1");
        given(memberRegistrar.register("member@example.com", "닉네임", OAuthProvider.GOOGLE, "google-1"))
                .willReturn(member);
        given(tokenIssuer.issue(member.getId(), member.getRole()))
                .willReturn(new TokenPair("access-token", "refresh-token", member.getRole()));

        // when
        TokenPairResult result = memberOAuthLoginService.completeRegistration("registration-token", "닉네임");

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @DisplayName("등록 토큰이 만료됐거나 이미 쓰였으면 등록을 완료할 수 없다.")
    @Test
    void completeRegistration_invalidToken() {
        // given
        given(registrationTokenResolver.resolve("invalid-token"))
                .willThrow(new BusinessException(AuthErrorCode.AUTH_TOKEN_INVALID));

        // when & then
        assertThatThrownBy(() -> memberOAuthLoginService.completeRegistration("invalid-token", "닉네임"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                        .isEqualTo(AuthErrorCode.AUTH_TOKEN_INVALID));
        verify(tokenIssuer, never()).issue(any(), any());
    }

    @DisplayName("다른 소셜 계정으로 이미 가입된 이메일이면 등록을 완료할 수 없다.")
    @Test
    void completeRegistration_duplicateSocialAccount() {
        // given
        given(registrationTokenResolver.resolve("registration-token"))
                .willReturn(new OAuthExchangeEntry("member@example.com", OAuthProvider.GOOGLE, "google-1"));
        given(memberRegistrar.register("member@example.com", "닉네임", OAuthProvider.GOOGLE, "google-1"))
                .willThrow(new BusinessException(MemberErrorCode.MEMBER_DUPLICATE_SOCIAL_ACCOUNT));

        // when & then
        assertThatThrownBy(() -> memberOAuthLoginService.completeRegistration("registration-token", "닉네임"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                        .isEqualTo(MemberErrorCode.MEMBER_DUPLICATE_SOCIAL_ACCOUNT));
        verify(tokenIssuer, never()).issue(any(), any());
    }
}
