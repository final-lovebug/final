package com.ubidict.backend.member.domain;

/**
 * 회원 상태.
 *
 * <p>{@code PENDING}은 소셜 로그인만으로는 도달하지 않는다(별도 가입 절차가
 * 없어 최초 로그인 시 바로 {@code ACTIVE}로 생성된다). 향후 가입 후 확인
 * 단계가 추가될 경우를 대비해 남겨둔다.
 */
public enum MemberStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    WITHDRAWN;

    /**
     * 로그인 가능 여부를 판단하는 정책이다. {@code ACTIVE}가 아니면 로그인을
     * 막는다.
     */
    public boolean canLogin() {
        return this == ACTIVE;
    }
}
