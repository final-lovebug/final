package com.ubidict.backend.member.domain;

/**
 * 소셜 로그인 제공자.
 *
 * <p>MVP1은 구글만 지원한다. 카카오·네이버는 MVP2 확장 대상이다
 * ({@code docs/DOMAIN.md} 참고).
 */
public enum OAuthProvider {
    GOOGLE
}
