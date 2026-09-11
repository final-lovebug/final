package com.ubidict.backend.document.infra.adapter;

import com.ubidict.backend.document.infra.port.DraftDictionaryQueryPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.crossdomain.draft-dictionary.mode", havingValue = "stub", matchIfMissing = true)
public class DraftDictionaryQueryStub implements DraftDictionaryQueryPort {
    @Override
    public boolean isSourceOfOngoingDraft(Long documentId) {
        return false;
    }
}
