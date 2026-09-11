package com.ubidict.backend.workspace.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import org.springframework.stereotype.Component;

@Component
public class ParticipantPolicyValidator {
    public void validateManageable(Participant target, Permission actor) {
        if (!target.canBeManagedBy(actor)) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_PARTICIPANT_NOT_MANAGEABLE);
        }
    }

    public void validateRemovable(Participant target, Permission actor) {
        if (target.isOwner()) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_OWNER_NOT_REMOVABLE);
        }
        validateManageable(target, actor);
    }
}
