package com.ubidict.backend.revisionlog.infra.port;

import java.util.List;

public interface DraftDocumentQueryPort {

    List<AppliedSuggestion> readAppliedSuggestions(Long draftDocumentId);
}
