package com.ubidict.backend.draftdocument.infra.port;

public interface WorkspacePolicyPort {

    boolean isParticipant(Long workspaceId, Long memberId);
}
