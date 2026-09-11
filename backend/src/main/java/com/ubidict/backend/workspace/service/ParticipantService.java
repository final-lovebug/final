package com.ubidict.backend.workspace.service;

import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.implement.ParticipantPolicyValidator;
import com.ubidict.backend.workspace.implement.ParticipantReader;
import com.ubidict.backend.workspace.implement.ParticipantRemover;
import com.ubidict.backend.workspace.implement.ParticipantUpdater;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParticipantService {
    private final WorkspaceAccessValidator accessValidator;
    private final ParticipantReader participantReader;
    private final ParticipantUpdater participantUpdater;
    private final ParticipantPolicyValidator policyValidator;
    private final ParticipantRemover participantRemover;

    @Transactional(readOnly = true)
    public List<ParticipantResult> readAll(Long workspaceId, Long memberId) {
        accessValidator.validateParticipant(workspaceId, memberId);
        return participantReader.readAllByWorkspace(workspaceId).stream()
                .map(ParticipantResult::from).toList();
    }

    @Transactional
    public ParticipantResult changePermission(ChangePermissionCommand command) {
        accessValidator.validateAtLeast(command.workspaceId(), command.actorId(), Permission.OWNER);
        Participant actor = participantReader.readOptional(command.workspaceId(), command.actorId()).orElseThrow();
        Participant target = participantReader.readOptional(command.workspaceId(), command.memberId())
                .orElseThrow(() -> new com.ubidict.backend.common.exception.BusinessException(
                        com.ubidict.backend.workspace.exception.WorkspaceErrorCode.WORKSPACE_PARTICIPANT_NOT_FOUND));
        policyValidator.validateManageable(target, actor.getPermission());
        return ParticipantResult.from(participantUpdater.changePermission(target, command.permission()));
    }

    @Transactional
    public void transferOwnership(TransferOwnershipCommand command) {
        accessValidator.validateAtLeast(command.workspaceId(), command.actorId(), Permission.OWNER);
        Participant owner = participantReader.readOptional(command.workspaceId(), command.actorId()).orElseThrow();
        Participant target = participantReader.readOptional(command.workspaceId(), command.targetMemberId())
                .orElseThrow(() -> new com.ubidict.backend.common.exception.BusinessException(
                        com.ubidict.backend.workspace.exception.WorkspaceErrorCode.WORKSPACE_PARTICIPANT_NOT_FOUND));
        participantUpdater.transferOwnership(owner, target);
    }

    @Transactional
    public void remove(RemoveParticipantCommand command) {
        accessValidator.validateAtLeast(command.workspaceId(), command.actorId(), Permission.ADMIN);
        Participant actor = participantReader.readOptional(command.workspaceId(), command.actorId()).orElseThrow();
        Participant target = participantReader.readOptional(command.workspaceId(), command.memberId())
                .orElseThrow(() -> new com.ubidict.backend.common.exception.BusinessException(
                        com.ubidict.backend.workspace.exception.WorkspaceErrorCode.WORKSPACE_PARTICIPANT_NOT_FOUND));
        policyValidator.validateRemovable(target, actor.getPermission());
        participantRemover.remove(target);
    }
}
