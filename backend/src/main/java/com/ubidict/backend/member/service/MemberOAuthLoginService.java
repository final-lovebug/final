package com.ubidict.backend.member.service;

import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.implement.MemberRegistrar;
import com.ubidict.backend.member.implement.MemberStatusValidator;
import com.ubidict.backend.member.implement.OAuthExchangeCodeResolver;
import com.ubidict.backend.member.implement.RegistrationTokenIssuer;
import com.ubidict.backend.member.implement.RegistrationTokenResolver;
import com.ubidict.backend.member.implement.TokenIssuer;
import com.ubidict.backend.member.implement.TokenPair;
import com.ubidict.backend.member.infra.security.OAuthExchangeEntry;
import com.ubidict.backend.member.service.model.LoginSucceeded;
import com.ubidict.backend.member.service.model.OAuthExchangeOutcome;
import com.ubidict.backend.member.service.model.RegistrationRequired;
import com.ubidict.backend.member.service.model.TokenPairResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Google OAuth2 로그인을 처리한다. 별도의 회원가입 API는 없고, 최초 로그인 시 사용자가
 * 입력한 닉네임으로 회원이 등록된다({@code docs/DOMAIN.md} 정책, REQ-USR-001 9/11 번복).
 */
@RequiredArgsConstructor
@Service
public class MemberOAuthLoginService {

    private final MemberRegistrar memberRegistrar;
    private final MemberStatusValidator memberStatusValidator;
    private final OAuthExchangeCodeResolver oAuthExchangeCodeResolver;
    private final RegistrationTokenIssuer registrationTokenIssuer;
    private final RegistrationTokenResolver registrationTokenResolver;
    private final TokenIssuer tokenIssuer;

    /**
     * Google 콜백이 발급한 1회용 교환 코드로 로그인한다({@code docs/API.md} "콜백 및 토큰 교환").
     * 이미 가입된 회원이면 바로 로그인시키고, 처음 보는 식별자면 {@code Member} row를 만들지
     * 않고 등록 토큰만 발급한다 — 닉네임은 {@link #completeRegistration}에서 받는다.
     */
    @Transactional
    public OAuthExchangeOutcome loginByExchangeCode(String code) {
        OAuthExchangeEntry entry = oAuthExchangeCodeResolver.resolve(code);

        return memberRegistrar
                .find(entry.provider(), entry.providerId())
                .<OAuthExchangeOutcome>map(member -> new LoginSucceeded(issueTokens(member)))
                .orElseGet(() -> new RegistrationRequired(
                        registrationTokenIssuer.issue(entry.email(), entry.provider(), entry.providerId())));
    }

    /**
     * 닉네임 온보딩을 마무리한다({@code docs/API.md} "닉네임 등록 완료"). 이 시점에 비로소
     * {@code Member} row가 만들어지고 바로 {@code ACTIVE} 상태로 로그인된다.
     */
    @Transactional
    public TokenPairResult completeRegistration(String registrationToken, String displayName) {
        OAuthExchangeEntry entry = registrationTokenResolver.resolve(registrationToken);
        Member member = memberRegistrar.register(entry.email(), displayName, entry.provider(), entry.providerId());

        return issueTokens(member);
    }

    private TokenPairResult issueTokens(Member member) {
        memberStatusValidator.validateLoginable(member);

        TokenPair tokenPair = tokenIssuer.issue(member.getId(), member.getRole());
        return new TokenPairResult(tokenPair.accessToken(), tokenPair.refreshToken(), tokenPair.role());
    }
}
