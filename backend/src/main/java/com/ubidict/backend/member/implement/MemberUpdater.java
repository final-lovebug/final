package com.ubidict.backend.member.implement;

import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.infra.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class MemberUpdater {

    private final MemberReader memberReader;
    private final MemberRepository memberRepository;

    public Member update(Long memberId, String displayName) {
        Member member = memberReader.read(memberId);
        member.changeDisplayName(displayName);
        return memberRepository.save(member);
    }
}
