package com.ubidict.backend.member.service;

import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.implement.MemberRegistrar;
import com.ubidict.backend.member.implement.MemberStatusValidator;
import com.ubidict.backend.member.implement.OAuthExchangeCodeResolver;
import com.ubidict.backend.member.implement.TokenIssuer;
import com.ubidict.backend.member.implement.TokenPair;
import com.ubidict.backend.member.infra.security.OAuthExchangeEntry;
import com.ubidict.backend.member.service.model.OAuthLoginCommand;
import com.ubidict.backend.member.service.model.TokenPairResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Google OAuth2 로그인을 처리한다. 별도의 회원가입 API는 없고, 최초 로그인
 * 시 회원이 자동 등록된다({@code docs/DOMAIN.md} 정책).
 */
@RequiredArgsConstructor
@Service
public class MemberOAuthLoginService {

    private final MemberRegistrar memberRegistrar;
    private final MemberStatusValidator memberStatusValidator;
    private final OAuthExchangeCodeResolver oAuthExchangeCodeResolver;
    private final TokenIssuer tokenIssuer;

    @Transactional
    public TokenPairResult login(OAuthLoginCommand command) {
        return login(command.email(), command.displayName(), command.provider(), command.providerId());
    }

    /**
     * Google 콜백이 발급한 1회용 교환 코드로 로그인한다({@code docs/API.md} "콜백 및 토큰 교환").
     */
    @Transactional
    public TokenPairResult loginByExchangeCode(String code) {
        OAuthExchangeEntry entry = oAuthExchangeCodeResolver.resolve(code);
        return login(entry.email(), entry.displayName(), entry.provider(), entry.providerId());
    }

    private TokenPairResult login(String email, String displayName, OAuthProvider provider, String providerId) {
        Member member = memberRegistrar.findOrRegister(email, displayName, provider, providerId);
        memberStatusValidator.validateLoginable(member);

        TokenPair tokenPair = tokenIssuer.issue(member.getId(), member.getRole());
        return new TokenPairResult(tokenPair.accessToken(), tokenPair.refreshToken(), tokenPair.role());
    }
}
