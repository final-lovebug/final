package com.ubidict.backend.workspace.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.workspace.domain.Invitation;
import com.ubidict.backend.workspace.domain.InvitationStatus;
import com.ubidict.backend.workspace.exception.InvitationErrorCode;
import com.ubidict.backend.workspace.infra.InvitationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvitationReader {

    private final InvitationRepository invitationRepository;

    public Invitation read(Long invitationId, Long workspaceId) {
        return invitationRepository
                .findByIdAndWorkspaceId(invitationId, workspaceId)
                .orElseThrow(() -> new BusinessException(InvitationErrorCode.INVITATION_NOT_FOUND));
    }

    public Invitation readByToken(String token) {
        return invitationRepository
                .findByToken(token)
                .orElseThrow(() -> new BusinessException(InvitationErrorCode.INVITATION_NOT_FOUND));
    }

    public List<Invitation> readAll(Long workspaceId) {
        return invitationRepository.findAllByWorkspaceId(workspaceId);
    }

    public boolean hasPending(Long workspaceId, String inviteeEmail) {
        return inviteeEmail != null
                && invitationRepository.existsByWorkspaceIdAndInviteeEmailAndStatus(
                        workspaceId, inviteeEmail, InvitationStatus.PENDING);
    }
}
