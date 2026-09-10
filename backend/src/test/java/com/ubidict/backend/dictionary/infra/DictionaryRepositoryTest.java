package com.ubidict.backend.dictionary.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.fixture.DictionaryFixture;
import com.ubidict.backend.support.RepositoryTestSupport;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class DictionaryRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @DisplayName("사전집을 저장하면 버전 값 객체가 컬럼 두 개로 저장된다.")
    @Test
    void save_versionIsEmbedded() {
        // given
        Long workspaceId = saveWorkspace();

        // when
        Dictionary saved = dictionaryRepository.save(
                DictionaryFixture.dictionary().workspaceId(workspaceId).build());
        em.flush();
        em.clear();

        // then
        Dictionary found = dictionaryRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getVersion().versionNo()).isEqualTo(1);
        assertThat(found.getVersion().publishedAt()).isNotNull();
        assertThat(found.getStatus()).isEqualTo(DictionaryStatus.ACTIVE);
    }

    @DisplayName("워크스페이스의 활성 사전집만 조회한다.")
    @Test
    void findByWorkspaceIdAndStatus() {
        // given
        Long workspaceId = saveWorkspace();
        saveDictionary(workspaceId, 1, DictionaryStatus.ARCHIVED);
        Dictionary active = saveDictionary(workspaceId, 2, DictionaryStatus.ACTIVE);
        em.flush();
        em.clear();

        // when
        Optional<Dictionary> found =
                dictionaryRepository.findByWorkspaceIdAndStatus(workspaceId, DictionaryStatus.ACTIVE);

        // then
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getId()).isEqualTo(active.getId());
    }

    @DisplayName("버전 번호로 사전집을 조회한다.")
    @Test
    void findByWorkspaceIdAndVersionVersionNo() {
        // given
        Long workspaceId = saveWorkspace();
        saveDictionary(workspaceId, 1, DictionaryStatus.ARCHIVED);
        saveDictionary(workspaceId, 2, DictionaryStatus.ACTIVE);
        em.flush();
        em.clear();

        // when
        Optional<Dictionary> found = dictionaryRepository.findByWorkspaceIdAndVersionVersionNo(workspaceId, 1);

        // then
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getStatus()).isEqualTo(DictionaryStatus.ARCHIVED);
    }

    @DisplayName("버전 이력은 최신 버전이 먼저 나온다.")
    @Test
    void findAllByWorkspaceIdOrderByVersionVersionNoDesc() {
        // given
        Long workspaceId = saveWorkspace();
        saveDictionary(workspaceId, 1, DictionaryStatus.ARCHIVED);
        saveDictionary(workspaceId, 2, DictionaryStatus.ARCHIVED);
        saveDictionary(workspaceId, 3, DictionaryStatus.ACTIVE);
        em.flush();
        em.clear();

        // when
        List<Dictionary> versions = dictionaryRepository.findAllByWorkspaceIdOrderByVersionVersionNoDesc(workspaceId);

        // then
        assertThat(versions).extracting(Dictionary::versionNo).containsExactly(3, 2, 1);
    }

    /**
     * "활성 사전집은 워크스페이스당 1개"를 DB가 보장하는지 확인한다. active_flag 생성 컬럼이 걸려 있어야 통과한다.
     */
    @DisplayName("한 워크스페이스에 활성 사전집이 둘이면 저장할 수 없다.")
    @Test
    void save_activeDictionaryIsDuplicated() {
        // given
        Long workspaceId = saveWorkspace();
        saveDictionary(workspaceId, 1, DictionaryStatus.ACTIVE);
        Dictionary another = DictionaryFixture.dictionary()
                .workspaceId(workspaceId)
                .versionNo(2)
                .build();

        // when & then
        assertThatThrownBy(() -> {
                    dictionaryRepository.save(another);
                    em.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * 보관 행은 NULL로 계산되어 유니크 판정에서 제외된다.
     */
    @DisplayName("보관된 사전집은 한 워크스페이스에 여러 개 쌓인다.")
    @Test
    void save_archivedDictionariesAccumulate() {
        // given
        Long workspaceId = saveWorkspace();

        // when
        saveDictionary(workspaceId, 1, DictionaryStatus.ARCHIVED);
        saveDictionary(workspaceId, 2, DictionaryStatus.ARCHIVED);
        saveDictionary(workspaceId, 3, DictionaryStatus.ARCHIVED);
        em.flush();
        em.clear();

        // then
        assertThat(dictionaryRepository.findAllByWorkspaceIdOrderByVersionVersionNoDesc(workspaceId))
                .hasSize(3);
    }

    @DisplayName("한 워크스페이스에 같은 버전 번호가 둘이면 저장할 수 없다.")
    @Test
    void save_versionNoIsDuplicated() {
        // given
        Long workspaceId = saveWorkspace();
        saveDictionary(workspaceId, 1, DictionaryStatus.ARCHIVED);
        Dictionary another = DictionaryFixture.dictionary()
                .workspaceId(workspaceId)
                .versionNo(1)
                .status(DictionaryStatus.ARCHIVED)
                .build();

        // when & then
        assertThatThrownBy(() -> {
                    dictionaryRepository.save(another);
                    em.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("다른 워크스페이스라면 같은 버전 번호를 쓸 수 있다.")
    @Test
    void save_versionNoIsUniquePerWorkspace() {
        // given
        Long workspaceId = saveWorkspace();
        Long otherWorkspaceId = saveWorkspace();
        saveDictionary(workspaceId, 1, DictionaryStatus.ACTIVE);

        // when
        saveDictionary(otherWorkspaceId, 1, DictionaryStatus.ACTIVE);
        em.flush();
        em.clear();

        // then
        assertThat(dictionaryRepository.findByWorkspaceIdAndVersionVersionNo(otherWorkspaceId, 1))
                .isPresent();
    }

    private Dictionary saveDictionary(Long workspaceId, int versionNo, DictionaryStatus status) {
        return dictionaryRepository.save(DictionaryFixture.dictionary()
                .workspaceId(workspaceId)
                .versionNo(versionNo)
                .status(status)
                .build());
    }

    private Long saveWorkspace() {
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().build());

        return workspace.getId();
    }
}
