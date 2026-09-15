package com.ubidict.backend.workspace.implement;

import com.ubidict.backend.workspace.domain.RuleSet;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 이름 검증은 Workspace가 스스로 한다. 여기서는 저장만 책임진다.
 */
@Component
@RequiredArgsConstructor
public class WorkspaceUpdater {

    private final WorkspaceRepository workspaceRepository;

    public void rename(Workspace workspace, String name) {
        workspace.rename(name);
        workspaceRepository.save(workspace);
    }

    public void changeRuleSet(Workspace workspace, RuleSet ruleSet) {
        workspace.changeRuleSet(ruleSet);
        workspaceRepository.save(workspace);
    }
}
