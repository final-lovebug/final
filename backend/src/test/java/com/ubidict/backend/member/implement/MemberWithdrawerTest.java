package com.ubidict.backend.member.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.MemberStatus;
import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.infra.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberWithdrawerTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberReader memberReader = new MemberReader(memberRepository);
    private final MemberWithdrawer memberWithdrawer = new MemberWithdrawer(memberReader, memberRepository);

    @DisplayName("존재하는 회원을 탈퇴시키면 상태가 WITHDRAWN으로 바뀐다.")
    @Test
    void withdraw() {
        // given
        Member member = Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        memberWithdrawer.withdraw(1L);

        // then
        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.isDeleted()).isTrue();
    }

    @DisplayName("존재하지 않는 회원을 탈퇴시키려 하면 예외가 발생한다.")
    @Test
    void withdraw_notFound() {
        // given
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberWithdrawer.withdraw(1L)).isInstanceOf(BusinessException.class);
    }
}
