package com.ubidict.backend.draftdictionary.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.domain.Term;
import com.ubidict.backend.dictionary.fixture.DictionaryFixture;
import com.ubidict.backend.dictionary.fixture.TermFixture;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import com.ubidict.backend.dictionary.infra.TermRepository;
import com.ubidict.backend.draftdictionary.infra.port.TermSnapshot;
import com.ubidict.backend.support.RepositoryTestSupport;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(DictionaryTermQueryAdapter.class)
class DictionaryTermQueryAdapterTest extends RepositoryTestSupport {

    @Autowired
    private DictionaryTermQueryAdapter adapter;

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @DisplayName("활성 버전의 용어만 스냅샷으로 반환한다.")
    @Test
    void readActiveTerms_returnsActiveVersionOnly() {
        Long workspaceId = saveWorkspace();
        Long archivedId = saveDictionary(workspaceId, 1, DictionaryStatus.ARCHIVED);
        Long activeId = saveDictionary(workspaceId, 2, DictionaryStatus.ACTIVE);
        saveTerm(archivedId, "보관 용어", "Archived", "보관 정의");
        Term activeTerm = saveTerm(activeId, "활성 용어", "Active", "활성 정의");
        em.flush();
        em.clear();

        List<TermSnapshot> snapshots = adapter.readActiveTerms(workspaceId);

        assertThat(snapshots)
                .extracting(
                        TermSnapshot::termId,
                        TermSnapshot::preferredForm,
                        TermSnapshot::englishName,
                        TermSnapshot::definition)
                .containsExactly(tuple(activeTerm.getId(), "활성 용어", "Active", "활성 정의"));
    }

    @DisplayName("활성 사전집이 없으면 빈 목록을 반환한다.")
    @Test
    void readActiveTerms_dictionaryIsAbsent() {
        Long workspaceId = saveWorkspace();

        assertThat(adapter.readActiveTerms(workspaceId)).isEmpty();
    }

    private Term saveTerm(Long dictionaryId, String preferredForm, String englishName, String definition) {
        return termRepository.save(TermFixture.term()
                .dictionaryId(dictionaryId)
                .preferredForm(preferredForm)
                .englishName(englishName)
                .definition(definition)
                .build());
    }

    private Long saveDictionary(Long workspaceId, int versionNo, DictionaryStatus status) {
        Dictionary dictionary = dictionaryRepository.save(DictionaryFixture.dictionary()
                .workspaceId(workspaceId)
                .versionNo(versionNo)
                .status(status)
                .build());
        return dictionary.getId();
    }

    private Long saveWorkspace() {
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().build());
        return workspace.getId();
    }
}
