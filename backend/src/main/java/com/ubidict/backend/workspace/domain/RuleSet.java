package com.ubidict.backend.workspace.domain;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 워크스페이스의 리뷰 규칙. 식별자도 생명주기도 따로 두지 않는 값 객체이며 workspace 테이블의 컬럼 2개로 저장된다.
 *
 * <p>상한은 참여자 수를 알아야 하므로 여기서 검증하지 않는다. 룰셋 수정 유스케이스가 생길 때 implement 레이어에서 검증한다.
 */
@Embeddable
public record RuleSet(
        @Column(name = "required_document_reviewer_count", nullable = false)
        int requiredDocumentReviewerCount,

        @Column(name = "required_dictionary_reviewer_count", nullable = false)
        int requiredDictionaryReviewerCount) {

    public RuleSet {
        if (requiredDocumentReviewerCount < 0 || requiredDictionaryReviewerCount < 0) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_INVALID_REVIEWER_COUNT);
        }
    }

    public static RuleSet initial() {
        return new RuleSet(0, 0);
    }

    /**
     * 정족수를 채울 수 있는 최대 인원을 넘는지 본다.
     *
     * <p>요청자 본인은 자기 요청을 리뷰할 수 없으므로(`D-95`) 승인을 달 수 있는 사람은 늘 <b>참여자 수 - 1</b>명이다. 참여자 수와 같은 정족수를 허용하면
     * 영원히 채울 수 없는 값이 저장된다.
     */
    public boolean exceedsReviewerCapacity(long participantCount) {
        long capacity = Math.max(0, participantCount - 1);

        return requiredDocumentReviewerCount > capacity || requiredDictionaryReviewerCount > capacity;
    }
}
