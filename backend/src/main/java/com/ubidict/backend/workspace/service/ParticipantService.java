package com.ubidict.backend.workspace.service;

import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.implement.ParticipantPolicyValidator;
import com.ubidict.backend.workspace.implement.ParticipantReader;
import com.ubidict.backend.workspace.implement.ParticipantRemover;
import com.ubidict.backend.workspace.implement.ParticipantUpdater;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import com.ubidict.backend.workspace.service.model.ChangePermissionCommand;
import com.ubidict.backend.workspace.service.model.ParticipantResult;
import com.ubidict.backend.workspace.service.model.RemoveParticipantCommand;
import com.ubidict.backend.workspace.service.model.TransferOwnershipCommand;
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
                .map(ParticipantResult::from)
                .toList();
    }

    @Transactional
    public void changePermission(ChangePermissionCommand command) {
        accessValidator.validateAtLeast(command.workspaceId(), command.actorId(), Permission.OWNER);
        Participant actor = participantReader
                .readOptional(command.workspaceId(), command.actorId())
                .orElseThrow();
        Participant target = participantReader.readById(command.participantId(), command.workspaceId());
        policyValidator.validateManageable(target, actor.getPermission());
        participantUpdater.changePermission(target, command.permission());
    }

    @Transactional
    public void transferOwnership(TransferOwnershipCommand command) {
        accessValidator.validateAtLeast(command.workspaceId(), command.actorId(), Permission.OWNER);
        Participant owner = participantReader
                .readOptional(command.workspaceId(), command.actorId())
                .orElseThrow();
        Participant target = participantReader.readById(command.participantId(), command.workspaceId());
        participantUpdater.transferOwnership(owner, target);
    }

    @Transactional
    public void remove(RemoveParticipantCommand command) {
        accessValidator.validateAtLeast(command.workspaceId(), command.actorId(), Permission.ADMIN);
        Participant actor = participantReader
                .readOptional(command.workspaceId(), command.actorId())
                .orElseThrow();
        Participant target = participantReader.readById(command.participantId(), command.workspaceId());
        policyValidator.validateRemovable(target, actor.getPermission());
        participantRemover.remove(target);
    }
}
