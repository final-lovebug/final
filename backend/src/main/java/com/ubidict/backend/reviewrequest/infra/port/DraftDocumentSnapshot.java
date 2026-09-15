package com.ubidict.backend.reviewrequest.infra.port;

/**
 * 문서 개정안을 조립하는 데 필요한 초안의 값만 담는다.
 *
 * <p>{@code workspaceId}는 초안이 들고 있지 않다 — DraftDocument에는 documentId만 있다(D-37). 어댑터가 문서를 거쳐 해석해 채운다.
 *
 * <p>{@code dictionaryVersionNo}는 초안이 대조에 쓴 사전집 버전이고, 발행본에 그대로 찍힌다(D-93). <b>이 결정 이전에 만들어진 초안만
 * {@code null}</b>이며 그때는 발행 시점의 활성 버전을 쓴다.
 */
public record DraftDocumentSnapshot(
        Long draftDocumentId,
        Long documentId,
        Long workspaceId,
        int baseVersionNo,
        Integer dictionaryVersionNo,
        String draftBody) {}
