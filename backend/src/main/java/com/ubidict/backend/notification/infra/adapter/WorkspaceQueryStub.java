package com.ubidict.backend.notification.infra.adapter;

import com.ubidict.backend.notification.infra.port.WorkspaceQueryPort;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.crossdomain.workspace.mode", havingValue = "stub", matchIfMissing = true)
public class WorkspaceQueryStub implements WorkspaceQueryPort {

    @Override
    public List<Long> participantMemberIds(Long workspaceId) {
        return List.of();
    }
}
