package com.ubidict.backend.member.implement;

import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.infra.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 회원 탈퇴를 처리한다. 로컬 상태 변경(soft delete)만 하고, Google API를 호출해 토큰을
 * revoke하지는 않는다 — Google 토큰을 저장하지 않는 이 프로젝트의 stateless 인증 구조와
 * 맞지 않아 의도적으로 범위에서 뺐다({@code docs/DOMAIN.md} 인증·회원가입 정책, 9/10 확정).
 * 필요하면 회원이 Google 계정에서 직접 연동을 해제해야 한다.
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
