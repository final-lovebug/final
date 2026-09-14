package com.ubidict.backend.member.infra.security;

import com.ubidict.backend.member.domain.OAuthProvider;

/**
 * Google 로그인 성공 시 발급하는 1회용 교환 코드에 매달아 두는 신원 정보.
 * {@code docs/API.md}의 "콜백 및 토큰 교환" 절 참고.
 *
 * <p>실명(Google profile)은 담지 않는다(REQ-USR-001 9/11 번복) — 최초 로그인 시 사용자가
 * 직접 입력한 닉네임을 쓴다. 이 레코드는 신규 식별자의 등록 토큰({@link RegistrationTokenRedisRepository})
 * 페이로드로도 그대로 재사용한다 — 필드 구성이 정확히 같다.
 */
public record OAuthExchangeEntry(String email, OAuthProvider provider, String providerId) {}
