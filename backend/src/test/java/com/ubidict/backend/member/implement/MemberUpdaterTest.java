package com.ubidict.backend.member.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.infra.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberUpdaterTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberReader memberReader = new MemberReader(memberRepository);
    private final MemberUpdater memberUpdater = new MemberUpdater(memberReader, memberRepository);

    @DisplayName("존재하는 회원의 표시 이름을 수정한다.")
    @Test
    void update() {
        // given
        Member member = Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Member updated = memberUpdater.update(1L, "새이름");

        // then
        assertThat(updated.getDisplayName()).isEqualTo("새이름");
    }

    @DisplayName("존재하지 않는 회원을 수정하려 하면 예외가 발생한다.")
    @Test
    void update_notFound() {
        // given
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberUpdater.update(1L, "새이름")).isInstanceOf(BusinessException.class);
    }
}
