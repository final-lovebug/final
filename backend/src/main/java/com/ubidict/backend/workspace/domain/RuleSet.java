package com.ubidict.backend.workspace.domain;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 워크스페이스의 리뷰 규칙. 식별자도 생명주기도 따로 두지 않는 값 객체이며 workspace 테이블의 컬럼 2개로 저장된다.
 *
 * <p>상한(참여자 수 이하)은 참여자 수를 알아야 하므로 여기서 검증하지 않는다. 룰셋 수정 유스케이스가 생길 때 implement 레이어에서 검증한다.
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

    public boolean exceedsParticipantCount(long participantCount) {
        return requiredDocumentReviewerCount > participantCount || requiredDictionaryReviewerCount > participantCount;
    }
}
