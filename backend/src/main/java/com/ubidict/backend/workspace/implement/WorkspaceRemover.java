package com.ubidict.backend.workspace.implement;

import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 소프트 삭제다. 참여자 행은 함께 지우지 않는다 — 모든 조회가 워크스페이스에서 먼저 막히므로 남아 있어도 새어 나가지 않는다.
 */
@Component
@RequiredArgsConstructor
public class WorkspaceRemover {

    private final WorkspaceRepository workspaceRepository;

    public void remove(Workspace workspace) {
        workspace.delete();
        workspaceRepository.save(workspace);
    }
}
