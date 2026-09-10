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
import com.ubidict.backend.member.domain.MemberErrorCode;
import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.infra.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberWriterTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberReader memberReader = new MemberReader(memberRepository);
    private final MemberWriter memberWriter = new MemberWriter(memberReader, memberRepository);

    @DisplayName("이메일이 중복되지 않으면 회원을 생성한다.")
    @Test
    void create() {
        // given
        given(memberRepository.existsByEmail("member@example.com")).willReturn(false);
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Member member = memberWriter.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");

        // then
        assertThat(member.getEmail()).isEqualTo("member@example.com");
        assertThat(member.getDisplayName()).isEqualTo("member1");
    }

    @DisplayName("이메일이 이미 등록되어 있으면 생성에 실패한다.")
    @Test
    void create_duplicateEmail() {
        // given
        given(memberRepository.existsByEmail("member@example.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> memberWriter.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                        .isEqualTo(MemberErrorCode.MEMBER_DUPLICATE_EMAIL));
        verify(memberRepository, never()).save(any());
    }

    @DisplayName("존재하는 회원의 표시 이름을 수정한다.")
    @Test
    void update() {
        // given
        Member member = Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Member updated = memberWriter.update(1L, "새이름");

        // then
        assertThat(updated.getDisplayName()).isEqualTo("새이름");
    }

    @DisplayName("존재하지 않는 회원을 수정하려 하면 예외가 발생한다.")
    @Test
    void update_notFound() {
        // given
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberWriter.update(1L, "새이름")).isInstanceOf(BusinessException.class);
    }
}
