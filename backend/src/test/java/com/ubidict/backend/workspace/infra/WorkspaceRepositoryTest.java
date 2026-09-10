package com.ubidict.backend.workspace.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.support.RepositoryTestSupport;
import com.ubidict.backend.workspace.domain.RuleSet;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WorkspaceRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @DisplayName("워크스페이스를 저장하면 룰셋이 컬럼으로 함께 저장된다.")
    @Test
    void save_ruleSetIsEmbedded() {
        // given
        Workspace workspace =
                WorkspaceFixture.workspace().ruleSet(new RuleSet(2, 3)).build();

        // when
        Workspace saved = workspaceRepository.save(workspace);
        em.flush();
        em.clear();

        // then
        Workspace found = workspaceRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getRuleSet()).isEqualTo(new RuleSet(2, 3));
    }

    @DisplayName("워크스페이스를 저장하면 생성 시각과 수정 시각이 채워진다.")
    @Test
    void save_auditingFieldsAreSet() {
        // given
        Workspace workspace = WorkspaceFixture.workspace().build();

        // when
        Workspace saved = workspaceRepository.save(workspace);
        em.flush();

        // then
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getDeletedAt()).isNull();
    }

    @DisplayName("삭제하지 않은 워크스페이스는 식별자로 조회된다.")
    @Test
    void findByIdAndDeletedAtIsNull() {
        // given
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().build());
        em.flush();
        em.clear();

        // when
        Optional<Workspace> found = workspaceRepository.findByIdAndDeletedAtIsNull(workspace.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getName()).isEqualTo("개발팀");
    }

    @DisplayName("소프트 삭제된 워크스페이스는 식별자로 조회되지 않는다.")
    @Test
    void findByIdAndDeletedAtIsNull_workspaceIsDeleted() {
        // given
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().build());
        workspace.delete();
        em.flush();
        em.clear();

        // when
        Optional<Workspace> found = workspaceRepository.findByIdAndDeletedAtIsNull(workspace.getId());

        // then
        assertThat(found).isEmpty();
    }

    @DisplayName("식별자 목록으로 조회하면 소프트 삭제된 워크스페이스는 빠진다.")
    @Test
    void findAllByIdInAndDeletedAtIsNull() {
        // given
        Workspace alive = workspaceRepository.save(
                WorkspaceFixture.workspace().name("개발팀").build());
        Workspace deleted = workspaceRepository.save(
                WorkspaceFixture.workspace().name("폐쇄팀").build());
        deleted.delete();
        em.flush();
        em.clear();

        // when
        List<Workspace> found =
                workspaceRepository.findAllByIdInAndDeletedAtIsNull(List.of(alive.getId(), deleted.getId()));

        // then
        assertThat(found).extracting(Workspace::getId).containsExactly(alive.getId());
    }
}
