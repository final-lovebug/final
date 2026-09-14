package com.ubidict.backend.revisionlog.infra.port;

import java.util.List;

public interface DocumentQueryPort {

    List<DocumentVersionSnapshot> readVersions(Long documentId);

    long countAlignedBelow(Long workspaceId, int dictionaryVersionNo);
}
