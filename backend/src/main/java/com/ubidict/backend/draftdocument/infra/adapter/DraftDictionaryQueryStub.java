package com.ubidict.backend.draftdocument.infra.adapter;

import com.ubidict.backend.draftdocument.infra.port.DraftDictionaryQueryPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 진행 중인 사전 초안이 없다고 답하는 스텁. 상호 배타가 걸리지 않아 초안 생성이 항상 허용된다.
 */
@Component("draftDictionaryQueryStubForDraftDocument")
@ConditionalOnProperty(name = "app.crossdomain.draft-dictionary.mode", havingValue = "stub", matchIfMissing = true)
public class DraftDictionaryQueryStub implements DraftDictionaryQueryPort {

    @Override
    public boolean hasOngoingDraft(Long workspaceId) {
        return false;
    }
}
