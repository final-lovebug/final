package com.ubidict.backend.revisionlog.infra.port;

import java.util.List;
import java.util.Optional;

public interface DictionaryTermQueryPort {

    Optional<Long> findDictionaryIdByVersion(Long workspaceId, int versionNo);

    List<TermSnapshot> readTerms(Long dictionaryId);

    Optional<DictionaryVersionSnapshot> readVersion(Long workspaceId, int versionNo);
}
