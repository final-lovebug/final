package com.ubidict.backend.member.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.MemberErrorCode;
import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.infra.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberReaderTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberReader memberReader = new MemberReader(memberRepository);

    @DisplayName("존재하는 회원 id로 조회하면 회원을 반환한다.")
    @Test
    void read() {
        // given
        Member member = Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        // when
        Member found = memberReader.read(1L);

        // then
        assertThat(found).isEqualTo(member);
    }

    @DisplayName("존재하지 않는 회원 id로 조회하면 예외가 발생한다.")
    @Test
    void read_notFound() {
        // given
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberReader.read(1L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                        .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
