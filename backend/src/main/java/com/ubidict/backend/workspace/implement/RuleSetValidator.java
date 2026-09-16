package com.ubidict.backend.workspace.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.workspace.domain.RuleSet;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RuleSetValidator {
    private final ParticipantRepository participantRepository;

    /**
     * 요청자는 자기 요청의 리뷰어가 될 수 없으므로 정족수 상한은 "참여자 수 - 1"이다.
     */
    public void validate(Workspace workspace, RuleSet ruleSet) {
        long participantCount = participantRepository.countByWorkspaceIdAndDeletedAtIsNull(workspace.getId());
        if (ruleSet.exceedsReviewerCapacity(participantCount)) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_REVIEWER_COUNT_EXCEEDS_PARTICIPANTS);
        }
    }
}
