package com.ubidict.backend.workspace.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 삭제되지 않은 워크스페이스만 읽는다. 삭제된 워크스페이스는 없는 것과 같게 다룬다.
 */
@Component
@RequiredArgsConstructor
public class WorkspaceReader {

    private final WorkspaceRepository workspaceRepository;

    public Workspace read(Long workspaceId) {
        return workspaceRepository
                .findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
    }

    public List<Workspace> readAll(Collection<Long> workspaceIds) {
        if (workspaceIds.isEmpty()) {
            return List.of();
        }

        return workspaceRepository.findAllByIdInAndDeletedAtIsNull(workspaceIds);
    }
}
