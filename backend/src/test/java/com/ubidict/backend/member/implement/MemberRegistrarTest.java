package com.ubidict.backend.member.implement;

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
import com.ubidict.backend.member.exception.MemberErrorCode;
import com.ubidict.backend.member.infra.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberRegistrarTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberRegistrar memberRegistrar = new MemberRegistrar(memberRepository);

    @DisplayName("이미 같은 provider·providerId로 가입된 회원이면 그대로 반환한다.")
    @Test
    void findOrRegister_existingMember() {
        // given
        Member member = Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        given(memberRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-1"))
                .willReturn(Optional.of(member));

        // when
        Member found =
                memberRegistrar.findOrRegister("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");

        // then
        assertThat(found).isEqualTo(member);
        verify(memberRepository, never()).save(any());
    }

    @DisplayName("처음 로그인하는 provider·providerId면 새 회원을 등록한다.")
    @Test
    void findOrRegister_newMember() {
        // given
        given(memberRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-1"))
                .willReturn(Optional.empty());
        given(memberRepository.existsByEmail("member@example.com")).willReturn(false);
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Member registered =
                memberRegistrar.findOrRegister("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");

        // then
        assertThat(registered.getEmail()).isEqualTo("member@example.com");
        assertThat(registered.getDisplayName()).isEqualTo("member1");
    }

    @DisplayName("이미 다른 소셜 계정으로 가입된 이메일이면 등록에 실패한다.")
    @Test
    void findOrRegister_duplicateSocialAccount() {
        // given
        given(memberRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-1"))
                .willReturn(Optional.empty());
        given(memberRepository.existsByEmail("member@example.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> memberRegistrar.findOrRegister(
                        "member@example.com", "member1", OAuthProvider.GOOGLE, "google-1"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                        .isEqualTo(MemberErrorCode.MEMBER_DUPLICATE_SOCIAL_ACCOUNT));
    }
}
