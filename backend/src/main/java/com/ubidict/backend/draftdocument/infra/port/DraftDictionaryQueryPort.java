package com.ubidict.backend.draftdocument.infra.port;

/**
 * 워크스페이스에 진행 중인 사전 초안이 있는지 본다.
 *
 * <p>G-14의 상호 배타를 위한 것이다. 초안 하나만 조회해도 초안과 개정안을 동시에 덮는다(D-22).
 */
public interface DraftDictionaryQueryPort {
    boolean hasOngoingDraft(Long workspaceId);
}
