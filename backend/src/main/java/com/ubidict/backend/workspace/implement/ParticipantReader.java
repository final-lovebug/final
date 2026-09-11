package com.ubidict.backend.workspace.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParticipantReader {

    private final ParticipantRepository participantRepository;

    public Optional<Participant> readOptional(Long workspaceId, Long memberId) {
        return participantRepository.findByWorkspaceIdAndMemberIdAndDeletedAtIsNull(workspaceId, memberId);
    }

    public List<Participant> readAllByMember(Long memberId) {
        return participantRepository.findAllByMemberIdAndDeletedAtIsNull(memberId);
    }

    public List<Participant> readAllByWorkspace(Long workspaceId) {
        return participantRepository.findAllByWorkspaceIdAndDeletedAtIsNull(workspaceId);
    }

    public Participant readById(Long participantId, Long workspaceId) {
        return participantRepository
                .findByIdAndWorkspaceIdAndDeletedAtIsNull(participantId, workspaceId)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_PARTICIPANT_NOT_FOUND));
    }
}
