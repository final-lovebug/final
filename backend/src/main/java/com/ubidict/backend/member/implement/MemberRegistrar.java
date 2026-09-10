package com.ubidict.backend.member.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.MemberErrorCode;
import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.infra.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * OAuth 로그인 시 회원을 찾거나(find), 없으면 새로 등록한다(register). 이미
 * 다른 소셜 제공자로 가입된 이메일이면 거부한다 — 한 회원은 하나의 소셜
 * 계정으로만 가입한다({@code docs/DOMAIN.md} 정책).
 */
@RequiredArgsConstructor
@Component
public class MemberRegistrar {

    private final MemberRepository memberRepository;

    public Member findOrRegister(String email, String displayName, OAuthProvider provider, String providerId) {
        return memberRepository
                .findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> register(email, displayName, provider, providerId));
    }

    private Member register(String email, String displayName, OAuthProvider provider, String providerId) {
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(MemberErrorCode.MEMBER_DUPLICATE_SOCIAL_ACCOUNT);
        }
        return memberRepository.save(Member.create(email, displayName, provider, providerId));
    }
}
