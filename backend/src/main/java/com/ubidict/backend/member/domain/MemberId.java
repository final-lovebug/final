package com.ubidict.backend.member.domain;

/**
 * 회원 식별자.
 *
 * <p>자동증가 PK({@code Long})를 감싸 다른 도메인의 식별자와 타입 레벨에서
 * 섞이지 않게 한다.
 */
public record MemberId(Long value) {

    public MemberId {
        if (value == null) {
            throw new IllegalArgumentException("MemberId는 null일 수 없습니다.");
        }
    }
}
