package com.ubidict.backend.workspace.domain;

/**
 * 참여자 식별자.
 *
 * <p>자동증가 PK({@code Long})를 감싸 다른 도메인의 식별자와 타입 레벨에서 섞이지 않게 한다.
 */
public record ParticipantId(Long value) {

    public ParticipantId {
        if (value == null) {
            throw new IllegalArgumentException("ParticipantId는 null일 수 없습니다.");
        }
    }
}
