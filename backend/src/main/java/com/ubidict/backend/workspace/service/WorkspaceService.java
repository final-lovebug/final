package com.ubidict.backend.workspace.service;

import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.implement.ParticipantAppender;
import com.ubidict.backend.workspace.implement.ParticipantReader;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import com.ubidict.backend.workspace.implement.WorkspaceAppender;
import com.ubidict.backend.workspace.implement.WorkspaceReader;
import com.ubidict.backend.workspace.implement.WorkspaceRemover;
import com.ubidict.backend.workspace.implement.WorkspaceUpdater;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceReader workspaceReader;
    private final WorkspaceAppender workspaceAppender;
    private final WorkspaceUpdater workspaceUpdater;
    private final WorkspaceRemover workspaceRemover;
    private final ParticipantReader participantReader;
    private final ParticipantAppender participantAppender;
    private final WorkspaceAccessValidator workspaceAccessValidator;

    /**
     * 워크스페이스와 Owner 참여자를 한 트랜잭션에서 만든다. "Owner가 정확히 1명"을 이 경계가 보장한다.
     */
    @Transactional
    public WorkspaceResult create(CreateWorkspaceCommand command) {
        Workspace workspace = workspaceAppender.append(command.name(), command.memberId());
        participantAppender.appendOwner(workspace.getId(), command.memberId());

        return WorkspaceResult.of(workspace, Permission.OWNER);
    }

    /**
     * 참여한 워크스페이스만 조회하므로 별도의 접근 검증이 필요 없다.
     */
    @Transactional(readOnly = true)
    public List<WorkspaceResult> readMine(Long memberId) {
        Map<Long, Permission> permissions = participantReader.readAllByMember(memberId).stream()
                .collect(Collectors.toMap(Participant::getWorkspaceId, Participant::getPermission));

        return workspaceReader.readAll(permissions.keySet()).stream()
                .map(workspace -> WorkspaceResult.of(workspace, permissions.get(workspace.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResult read(Long workspaceId, Long memberId) {
        Permission permission = workspaceAccessValidator.validateParticipant(workspaceId, memberId);
        Workspace workspace = workspaceReader.read(workspaceId);

        return WorkspaceResult.of(workspace, permission);
    }

    @Transactional
    public void rename(RenameWorkspaceCommand command) {
        workspaceAccessValidator.validateAtLeast(command.workspaceId(), command.memberId(), Permission.ADMIN);
        Workspace workspace = workspaceReader.read(command.workspaceId());

        workspaceUpdater.rename(workspace, command.name());
    }

    @Transactional
    public void delete(Long workspaceId, Long memberId) {
        workspaceAccessValidator.validateAtLeast(workspaceId, memberId, Permission.OWNER);
        Workspace workspace = workspaceReader.read(workspaceId);

        workspaceRemover.remove(workspace);
    }
}
