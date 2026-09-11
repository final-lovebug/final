package com.ubidict.backend.workspace.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 워크스페이스 접근 검증의 단일 지점(NFR-WS-001, NFR-USR-006).
 *
 * <p>참여자가 아니면 403이 아니라 404를 던진다. 403을 주면 그 워크스페이스가 존재한다는 사실이 드러난다.
 *
 * <p>워크스페이스 소프트 삭제 시 참여자 행은 남으므로, 참여 여부만 보면 삭제된 워크스페이스에 딸린 자원이 새어 나간다. 워크스페이스 생존 확인까지 이 클래스가 책임진다 — 사전집·문서처럼
 * 워크스페이스 자체를 읽을 이유가 없는 도메인도 같은 검증에 올라탄다.
 */
@Component
@RequiredArgsConstructor
public class WorkspaceAccessValidator {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceAccessValidator.class);

    private final WorkspaceReader workspaceReader;
    private final ParticipantReader participantReader;

    public Permission validateParticipant(Long workspaceId, Long memberId) {
        workspaceReader.read(workspaceId);

        return participantReader
                .readOptional(workspaceId, memberId)
                .map(Participant::getPermission)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
    }

    /**
     * 참여 여부를 먼저 보고(404) 그다음 서열을 본다(403). 순서를 뒤집으면 비참여자에게 403이 나가 존재가 드러난다.
     */
    public void validateAtLeast(Long workspaceId, Long memberId, Permission required) {
        Permission permission = validateParticipant(workspaceId, memberId);
        if (!permission.isAtLeast(required)) {
            log.warn("[WorkspaceAccessValidator.validateAtLeast] Insufficient permission workspaceId={}, memberId={}, required={}", workspaceId, memberId, required);
            throw new BusinessException(insufficientPermissionCode(required));
        }
    }

    private static WorkspaceErrorCode insufficientPermissionCode(Permission required) {
        return required == Permission.OWNER
                ? WorkspaceErrorCode.WORKSPACE_OWNER_REQUIRED
                : WorkspaceErrorCode.WORKSPACE_ADMIN_REQUIRED;
    }
}
