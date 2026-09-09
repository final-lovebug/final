package com.ubidict.backend.member.implement;

/**
 * 새로 발급되거나 재발급된 access/refresh 토큰 쌍.
 */
public record TokenPair(String accessToken, String refreshToken) {}
