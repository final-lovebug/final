package com.ubidict.backend.reviewrequest.infra.port;

/**
 * 문서 개정안을 조립하는 데 필요한 초안의 값만 담는다.
 *
 * <p>{@code workspaceId}는 초안이 들고 있지 않다 — DraftDocument에는 documentId만 있다(D-37). 어댑터가 문서를 거쳐 해석해 채운다.
 */
public record DraftDocumentSnapshot(
        Long draftDocumentId, Long documentId, Long workspaceId, int baseVersionNo, String draftBody) {}
