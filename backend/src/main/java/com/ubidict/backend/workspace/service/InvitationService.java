package com.ubidict.backend.workspace.service;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.workspace.domain.Invitation;
import com.ubidict.backend.workspace.domain.InvitationStatus;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.exception.InvitationErrorCode;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.implement.InvitationAppender;
import com.ubidict.backend.workspace.implement.InvitationReader;
import com.ubidict.backend.workspace.implement.InvitationTargetReader;
import com.ubidict.backend.workspace.implement.InvitationUpdater;
import com.ubidict.backend.workspace.implement.ParticipantAppender;
import com.ubidict.backend.workspace.implement.ParticipantReader;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import com.ubidict.backend.workspace.implement.WorkspaceReader;
import com.ubidict.backend.workspace.service.model.InvitationResult;
import com.ubidict.backend.workspace.service.model.IssueInvitationCommand;
import com.ubidict.backend.workspace.service.model.WorkspaceResult;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final WorkspaceAccessValidator workspaceAccessValidator;
    private final WorkspaceReader workspaceReader;
    private final ParticipantReader participantReader;
    private final InvitationReader invitationReader;
    private final InvitationAppender invitationAppender;
    private final InvitationUpdater invitationUpdater;
    private final ParticipantAppender participantAppender;
    private final InvitationTargetReader invitationTargetReader;

    @Transactional
    public InvitationResult issue(IssueInvitationCommand command) {
        workspaceAccessValidator.validateAtLeast(command.workspaceId(), command.memberId(), Permission.ADMIN);
        if (invitationTargetReader.isParticipant(command.workspaceId(), command.inviteeEmail())) {
            throw new BusinessException(InvitationErrorCode.INVITATION_ALREADY_PARTICIPANT);
        }
        if (invitationReader.hasPending(command.workspaceId(), command.inviteeEmail())) {
            throw new BusinessException(InvitationErrorCode.INVITATION_DUPLICATE_PENDING);
        }

        Invitation invitation = invitationAppender.append(
                command.workspaceId(), command.inviteeEmail(), command.permission(), command.memberId());
        return InvitationResult.from(invitation);
    }

    @Transactional
    public List<InvitationResult> readAll(Long workspaceId, Long memberId, InvitationStatus status) {
        workspaceAccessValidator.validateAtLeast(workspaceId, memberId, Permission.ADMIN);
        List<Invitation> invitations = invitationReader.readAll(workspaceId);
        invitationUpdater.refreshExpiration(invitations, OffsetDateTime.now());

        return invitations.stream()
                .filter(invitation -> status == null || invitation.getStatus() == status)
                .map(InvitationResult::from)
                .toList();
    }

    @Transactional
    public void cancel(Long workspaceId, Long invitationId, Long memberId) {
        workspaceAccessValidator.validateParticipant(workspaceId, memberId);
        Invitation invitation = invitationReader.read(invitationId, workspaceId);
        if (!invitation.getCreatedBy().equals(memberId)) {
            workspaceAccessValidator.validateAtLeast(workspaceId, memberId, Permission.ADMIN);
        }
        invitationUpdater.cancel(invitation);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public WorkspaceResult accept(String token, Long memberId) {
        Invitation invitation = invitationReader.readByToken(token);
        OffsetDateTime acceptedAt = OffsetDateTime.now();
        if (!invitation.isAcceptable(acceptedAt)) {
            throw new BusinessException(InvitationErrorCode.INVITATION_NOT_ACCEPTABLE);
        }
        if (participantReader
                .readOptional(invitation.getWorkspaceId(), memberId)
                .isPresent()) {
            throw new BusinessException(InvitationErrorCode.INVITATION_ALREADY_PARTICIPANT);
        }
        if (participantReader.countByWorkspace(invitation.getWorkspaceId()) >= Participant.MAX_PARTICIPANTS) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_PARTICIPANT_LIMIT_EXCEEDED);
        }

        Participant participant = participantAppender.appendMember(
                invitation.getWorkspaceId(), memberId, invitation.getPermission(), invitation.getCreatedBy());
        invitationUpdater.accept(invitation, participant.getId(), acceptedAt);
        Workspace workspace = workspaceReader.read(invitation.getWorkspaceId());
        return WorkspaceResult.of(workspace, participant.getPermission());
    }
}
