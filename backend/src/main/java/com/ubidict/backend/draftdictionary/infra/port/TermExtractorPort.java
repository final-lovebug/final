package com.ubidict.backend.draftdictionary.infra.port;

import java.util.List;

public interface TermExtractorPort {

    List<ExtractedTerm> extract(List<Long> sourceDocumentIds, List<TermSnapshot> activeTerms);
}
