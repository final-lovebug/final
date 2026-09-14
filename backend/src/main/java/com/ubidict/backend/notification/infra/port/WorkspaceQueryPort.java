package com.ubidict.backend.notification.infra.port;

import java.util.List;

/**
 * 반영완료 알림을 워크스페이스 참여자 전원에게 보내기 위해 참여자 목록을 조회한다.
 *
 * <p>참여 여부·권한 <b>검증</b>은 이 포트를 쓰지 않는다. 그쪽은 {@code WorkspaceAccessValidator}를 직접 주입한다(D-19).
 */
public interface WorkspaceQueryPort {

    List<Long> participantMemberIds(Long workspaceId);
}
