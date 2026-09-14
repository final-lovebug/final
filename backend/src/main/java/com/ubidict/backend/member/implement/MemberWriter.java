package com.ubidict.backend.member.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.exception.MemberErrorCode;
import com.ubidict.backend.member.infra.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 회원 생성·수정을 처리한다. 생성은 테스트/관리자용이며, 실제 소셜 로그인을
 * 통한 가입은 로그인 도메인(별도 티켓)의 몫이다.
 */
@RequiredArgsConstructor
@Component
public class MemberWriter {

    private final MemberReader memberReader;
    private final MemberRepository memberRepository;

    public Member create(String email, String displayName, OAuthProvider provider, String providerId) {
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(MemberErrorCode.MEMBER_DUPLICATE_EMAIL);
        }
        return memberRepository.save(Member.create(email, displayName, provider, providerId));
    }

    public Member update(Long memberId, String displayName) {
        Member member = memberReader.read(memberId);
        member.changeDisplayName(displayName);
        return memberRepository.save(member);
    }
}
