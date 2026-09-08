package com.ubidict.backend.member.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.MemberErrorCode;
import com.ubidict.backend.member.infra.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class MemberReader {

    private final MemberRepository memberRepository;

    public Member read(Long memberId) {
        return memberRepository
                .findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
