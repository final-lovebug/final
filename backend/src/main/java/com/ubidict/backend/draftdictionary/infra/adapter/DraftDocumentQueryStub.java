package com.ubidict.backend.draftdictionary.infra.adapter;

import com.ubidict.backend.draftdictionary.infra.port.DraftDocumentQueryPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 진행 중인 문서 초안이 없다고 답하는 스텁. 상호 배타가 걸리지 않아 초안 생성이 항상 허용된다.
 */
@Component("draftDocumentQueryStubForDraftDictionary")
@ConditionalOnProperty(name = "app.crossdomain.draft-document.mode", havingValue = "stub", matchIfMissing = true)
public class DraftDocumentQueryStub implements DraftDocumentQueryPort {

    @Override
    public boolean hasOngoingDraft(Long workspaceId) {
        return false;
    }
}
