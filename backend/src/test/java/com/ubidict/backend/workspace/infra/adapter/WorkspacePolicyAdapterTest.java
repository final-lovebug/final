package com.ubidict.backend.workspace.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.support.RepositoryTestSupport;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WorkspacePolicyAdapterTest extends RepositoryTestSupport {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @DisplayName("리뷰 종류에 맞는 룰셋의 필수 리뷰어 수를 읽는다.")
    @Test
    void requiredReviewerCount_readsRuleSetByType() {
        // given
        Workspace workspace = workspaceRepository.save(
                WorkspaceFixture.workspace().ruleSet(2, 3).build());
        em.flush();
        em.clear();
        WorkspacePolicyAdapter adapter = new WorkspacePolicyAdapter(workspaceRepository);

        // when & then
        assertThat(adapter.requiredReviewerCount(workspace.getId(), ReviewRequestType.DOCUMENT))
                .isEqualTo(2);
        assertThat(adapter.requiredReviewerCount(workspace.getId(), ReviewRequestType.DICTIONARY))
                .isEqualTo(3);
    }

    @DisplayName("삭제된 워크스페이스의 룰셋은 조회되지 않는다.")
    @Test
    void requiredReviewerCount_workspaceIsDeleted() {
        // given
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().build());
        workspace.delete();
        em.flush();
        em.clear();
        WorkspacePolicyAdapter adapter = new WorkspacePolicyAdapter(workspaceRepository);

        // when & then
        assertThatThrownBy(() -> adapter.requiredReviewerCount(workspace.getId(), ReviewRequestType.DOCUMENT))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }
}
