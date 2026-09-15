package com.ubidict.backend.member.presentation.dto;

import com.ubidict.backend.member.domain.MemberRole;
import com.ubidict.backend.member.service.model.TokenPairResult;

/**
 * {@code exchange} 엔드포인트 전용 응답({@code docs/API.md} "콜백 및 토큰 교환"). 이미 가입된
 * 회원이면 {@code accessToken}/{@code role}이, 최초 로그인(신규 식별자)이면
 * {@code needsNickname}/{@code registrationToken}이 채워진다 — 서로 배타적이다.
 */
public record OAuthExchangeResponse(
        String accessToken, MemberRole role, Boolean needsNickname, String registrationToken) {

    public static OAuthExchangeResponse loggedIn(TokenPairResult tokenPair) {
        return new OAuthExchangeResponse(tokenPair.accessToken(), tokenPair.role(), null, null);
    }

    public static OAuthExchangeResponse registrationRequired(String registrationToken) {
        return new OAuthExchangeResponse(null, null, true, registrationToken);
    }
}
