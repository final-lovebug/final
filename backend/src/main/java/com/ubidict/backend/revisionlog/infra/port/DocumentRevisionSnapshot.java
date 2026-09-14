package com.ubidict.backend.revisionlog.infra.port;

/** 문서 개정안이 확정한 결과 버전과 그 원본 초안 식별자. */
public record DocumentRevisionSnapshot(
        Long workspaceId, Long documentId, Long draftDocumentId, int resultVersionNo, Long performedBy) {}
