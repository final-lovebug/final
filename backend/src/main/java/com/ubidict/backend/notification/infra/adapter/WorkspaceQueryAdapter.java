package com.ubidict.backend.notification.infra.adapter;

import com.ubidict.backend.notification.infra.port.WorkspaceQueryPort;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.workspace.mode", havingValue = "real")
public class WorkspaceQueryAdapter implements WorkspaceQueryPort {

    private final ParticipantRepository participantRepository;

    @Override
    public List<Long> participantMemberIds(Long workspaceId) {
        return participantRepository.findAllByWorkspaceIdAndDeletedAtIsNull(workspaceId).stream()
                .map(Participant::getMemberId)
                .toList();
    }
}
