package com.ubidict.backend.member.infra.security;

import com.ubidict.backend.member.domain.OAuthProvider;

/**
 * Google 로그인 성공 시 발급하는 1회용 교환 코드에 매달아 두는 사용자 정보.
 * {@code docs/API.md}의 "콜백 및 토큰 교환" 절 참고.
 */
public record OAuthExchangeEntry(String email, String displayName, OAuthProvider provider, String providerId) {}
