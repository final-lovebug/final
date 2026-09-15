package com.ubidict.backend.draftdictionary.infra.port;

import java.util.List;
import java.util.Optional;

public interface DictionaryTermQueryPort {
    List<TermSnapshot> readActiveTerms(Long workspaceId);

    Optional<Integer> activeVersionNo(Long workspaceId);
}
