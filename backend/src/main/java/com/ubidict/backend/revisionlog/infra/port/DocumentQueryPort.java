package com.ubidict.backend.revisionlog.infra.port;

public interface DocumentQueryPort {

    long countAlignedBelow(Long workspaceId, int dictionaryVersionNo);
}
