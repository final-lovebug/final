package com.ubidict.backend.draftdictionary.infra.adapter;

import com.ubidict.backend.draftdictionary.infra.port.ExtractedTerm;
import com.ubidict.backend.draftdictionary.infra.port.TermExtractorPort;
import com.ubidict.backend.draftdictionary.infra.port.TermSnapshot;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ai.extractor.mode", havingValue = "stub", matchIfMissing = true)
public class TermExtractorStub implements TermExtractorPort {

    @Override
    public List<ExtractedTerm> extract(List<Long> sourceDocumentIds, List<TermSnapshot> activeTerms) {
        return List.of();
    }
}
