package com.ubidict.backend.member.implement;

import com.ubidict.backend.member.domain.MemberRole;

/**
 * 새로 발급되거나 재발급된 access/refresh 토큰 쌍. {@code role}은 응답 조립 시
 * 토큰을 다시 파싱하지 않아도 되도록 함께 담는다.
 */
public record TokenPair(String accessToken, String refreshToken, MemberRole role) {}
