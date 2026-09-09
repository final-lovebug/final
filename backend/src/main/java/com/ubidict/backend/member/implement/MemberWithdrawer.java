package com.ubidict.backend.member.implement;

import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.infra.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 회원 탈퇴를 처리한다. 로컬 상태 변경(soft delete)만 하고, 실제 Google 소셜
 * 연동 해제(Unlink) 호출은 로그인 도메인(별도 티켓)에서 처리한다.
 */
@RequiredArgsConstructor
@Component
public class MemberWithdrawer {

    private final MemberReader memberReader;
    private final MemberRepository memberRepository;

    public void withdraw(Long memberId) {
        Member member = memberReader.read(memberId);
        member.withdraw();
        memberRepository.save(member);
    }
}
