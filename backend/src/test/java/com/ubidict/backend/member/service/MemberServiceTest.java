package com.ubidict.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.MemberRole;
import com.ubidict.backend.member.domain.MemberStatus;
import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.implement.MemberCreator;
import com.ubidict.backend.member.implement.MemberReader;
import com.ubidict.backend.member.implement.MemberUpdater;
import com.ubidict.backend.member.implement.MemberWithdrawer;
import com.ubidict.backend.member.service.model.CreateMemberCommand;
import com.ubidict.backend.member.service.model.MemberResult;
import com.ubidict.backend.member.service.model.UpdateMemberCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * MemberService는 implement 계층을 조합해 유스케이스를 표현하는 흐름만 검증한다.
 * 실제 저장·검증 로직은 각 implement 클래스의 단위 테스트에서 이미 검증했다.
 */
class MemberServiceTest {

    private final MemberCreator memberCreator = mock(MemberCreator.class);
    private final MemberReader memberReader = mock(MemberReader.class);
    private final MemberUpdater memberUpdater = mock(MemberUpdater.class);
    private final MemberWithdrawer memberWithdrawer = mock(MemberWithdrawer.class);
    private final MemberService memberService =
            new MemberService(memberCreator, memberReader, memberUpdater, memberWithdrawer);

    @DisplayName("회원을 생성하면 생성된 회원 정보를 반환한다.")
    @Test
    void create() {
        // given
        Member member = Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        given(memberCreator.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1"))
                .willReturn(member);

        // when
        MemberResult result = memberService.create(
                new CreateMemberCommand("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1"));

        // then
        assertThat(result.email()).isEqualTo("member@example.com");
        assertThat(result.displayName()).isEqualTo("member1");
        assertThat(result.status()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(result.role()).isEqualTo(MemberRole.REGULAR);
    }

    @DisplayName("id로 회원을 조회하면 회원 정보를 반환한다.")
    @Test
    void getById() {
        // given
        Member member = Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        given(memberReader.read(1L)).willReturn(member);

        // when
        MemberResult result = memberService.getById(1L);

        // then
        assertThat(result.email()).isEqualTo("member@example.com");
    }

    @DisplayName("회원을 수정하면 수정된 회원 정보를 반환한다.")
    @Test
    void update() {
        // given
        Member member = Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        member.changeDisplayName("새이름");
        given(memberUpdater.update(1L, "새이름")).willReturn(member);

        // when
        MemberResult result = memberService.update(new UpdateMemberCommand(1L, "새이름"));

        // then
        assertThat(result.displayName()).isEqualTo("새이름");
    }

    @DisplayName("회원을 탈퇴시키면 MemberWithdrawer를 호출한다.")
    @Test
    void withdraw() {
        // when
        memberService.withdraw(1L);

        // then
        verify(memberWithdrawer).withdraw(1L);
    }
}
