package com.ubidict.backend.workspace.implement;

import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceAppender {

    private final WorkspaceRepository workspaceRepository;

    public Workspace append(String name, Long memberId) {
        return workspaceRepository.save(Workspace.create(name, memberId));
    }
}
