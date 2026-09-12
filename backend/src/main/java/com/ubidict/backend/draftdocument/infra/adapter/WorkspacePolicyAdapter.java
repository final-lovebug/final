package com.ubidict.backend.draftdocument.infra.adapter;

import com.ubidict.backend.draftdocument.infra.port.WorkspacePolicyPort;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("workspacePolicyAdapterForDraftDocument")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.workspace.mode", havingValue = "real")
public class WorkspacePolicyAdapter implements WorkspacePolicyPort {

    private final ParticipantRepository participantRepository;

    @Override
    public boolean isParticipant(Long workspaceId, Long memberId) {
        return participantRepository
                .findByWorkspaceIdAndMemberIdAndDeletedAtIsNull(workspaceId, memberId)
                .isPresent();
    }
}
