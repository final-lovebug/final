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

    public void validate(Workspace workspace, RuleSet ruleSet) {
        long participantCount = participantRepository.countByWorkspaceIdAndDeletedAtIsNull(workspace.getId());
        if (ruleSet.exceedsParticipantCount(participantCount)) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_REVIEWER_COUNT_EXCEEDS_PARTICIPANTS);
        }
    }
}
