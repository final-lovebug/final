package com.ubidict.backend.member.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.exception.MemberErrorCode;
import com.ubidict.backend.member.infra.MemberRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * OAuth 로그인 시 회원을 찾거나(find) 등록한다(register). 이미 다른 소셜 제공자로 가입된
 * 이메일이면 등록을 거부한다 — 한 회원은 하나의 소셜 계정으로만 가입한다({@code docs/DOMAIN.md}
 * 정책).
 *
 * <p>find/register를 분리해둔 이유(REQ-USR-001 9/11 번복): 신규 식별자는 닉네임을 받기 전까지
 * {@code Member} row 자체를 만들지 않는다 — {@code MemberOAuthLoginService}가 먼저 {@link #find}로
 * 기존 회원 여부를 확인하고, 없으면 등록 토큰을 발급했다가 닉네임 제출 시점에야 {@link #register}를
 * 호출한다.
 */
@RequiredArgsConstructor
@Component
public class MemberRegistrar {

    private final MemberRepository memberRepository;

    public Optional<Member> find(OAuthProvider provider, String providerId) {
        return memberRepository.findByProviderAndProviderId(provider, providerId);
    }

    public Member register(String email, String displayName, OAuthProvider provider, String providerId) {
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(MemberErrorCode.MEMBER_DUPLICATE_SOCIAL_ACCOUNT);
        }
        return memberRepository.save(Member.create(email, displayName, provider, providerId));
    }
}
