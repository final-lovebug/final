package com.ubidict.backend.draftdictionary.infra.port;

/**
 * 워크스페이스에 진행 중인 문서 초안이 있는지 본다.
 *
 * <p>G-14의 상호 배타를 위한 것이다. {@code DraftDocument}에는 workspaceId가 없어 어댑터가 문서를 거쳐 찾는다(D-37).
 */
public interface DraftDocumentQueryPort {
    boolean hasOngoingDraft(Long workspaceId);
}
