package com.ubidict.backend.draftdocument.infra.adapter;

import com.ubidict.backend.draftdocument.infra.port.WorkspacePolicyPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("workspacePolicyStubForDraftDocument")
@ConditionalOnProperty(name = "app.crossdomain.workspace.mode", havingValue = "stub", matchIfMissing = true)
public class WorkspacePolicyStub implements WorkspacePolicyPort {

    @Override
    public boolean isParticipant(Long workspaceId, Long memberId) {
        return true;
    }
}
