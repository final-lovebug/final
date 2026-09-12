package com.ubidict.backend.draftdocument.infra.adapter;

import com.ubidict.backend.draftdocument.infra.port.CheckSuggestion;
import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import com.ubidict.backend.draftdocument.infra.port.TermCheckerPort;
import com.ubidict.backend.draftdocument.infra.port.TermSnapshot;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ai.checker.mode", havingValue = "stub", matchIfMissing = true)
public class TermCheckerStub implements TermCheckerPort {

    @Override
    public List<CheckSuggestion> check(DocumentSnapshot document, List<TermSnapshot> activeTerms) {
        return List.of();
    }
}
