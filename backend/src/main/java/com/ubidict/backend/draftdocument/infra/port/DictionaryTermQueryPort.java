package com.ubidict.backend.draftdocument.infra.port;

import java.util.List;

public interface DictionaryTermQueryPort {
    List<TermSnapshot> readActiveTerms(Long workspaceId);
}
