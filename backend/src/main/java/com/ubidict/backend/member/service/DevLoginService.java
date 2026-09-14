package com.ubidict.backend.member.service;

import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.implement.MemberReader;
import com.ubidict.backend.member.implement.MemberStatusValidator;
import com.ubidict.backend.member.implement.TokenIssuer;
import com.ubidict.backend.member.implement.TokenPair;
import com.ubidict.backend.member.service.model.TokenPairResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로컬 개발 전용 — Google 로그인을 거치지 않고, 이미 존재하는 회원으로 바로 토큰을 발급한다.
 * 다른 도메인(Workspace 등)을 개발하는 팀원이 매번 Google OAuth 왕복 없이 자기 API를 테스트할 수
 * 있게 하기 위함이다({@code POST /api/members}로 만든 테스트 회원과 함께 씀).
 *
 * <p>{@code local} 프로필에서만 빈이 등록되므로 배포 환경엔 아예 존재하지 않는다. 권한 승격 API는
 * 제공하지 않는다는 기존 정책({@code docs/DOMAIN.md})을 그대로 따라, 회원의 role은 DB에 저장된
 * 값을 그대로 쓴다(이 서비스가 role을 바꿔주지 않음).
 */
@Profile("local")
@RequiredArgsConstructor
@Service
public class DevLoginService {

    private final MemberReader memberReader;
    private final MemberStatusValidator memberStatusValidator;
    private final TokenIssuer tokenIssuer;

    @Transactional
    public TokenPairResult loginAs(Long memberId) {
        Member member = memberReader.read(memberId);
        memberStatusValidator.validateLoginable(member);

        TokenPair tokenPair = tokenIssuer.issue(member.getId(), member.getRole());
        return new TokenPairResult(tokenPair.accessToken(), tokenPair.refreshToken(), tokenPair.role());
    }
}
