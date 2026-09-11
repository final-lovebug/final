package com.ubidict.backend.document.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.fixture.DictionaryFixture;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import com.ubidict.backend.support.RepositoryTestSupport;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DictionaryQueryAdapterTest extends RepositoryTestSupport {

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private DictionaryQueryAdapter adapter;
    private Long workspaceId;

    @BeforeEach
    void setUp() {
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().build());
        workspaceId = workspace.getId();
        adapter = new DictionaryQueryAdapter(dictionaryRepository);
    }

    @Test
    @DisplayName("보관 버전은 활성 버전으로 읽히지 않는다.")
    void activeVersionNo_returnsActiveOnly() {
        // given
        Dictionary archived = DictionaryFixture.dictionary()
                .workspaceId(workspaceId)
                .versionNo(1)
                .build();
        archived.archive();
        dictionaryRepository.save(archived);
        dictionaryRepository.save(DictionaryFixture.dictionary()
                .workspaceId(workspaceId)
                .versionNo(2)
                .build());
        em.flush();
        em.clear();

        // when & then
        assertThat(adapter.activeVersionNo(workspaceId)).contains(2);
    }

    @Test
    @DisplayName("사전집이 없으면 빈 Optional을 반환한다.")
    void activeVersionNo_dictionaryIsAbsent() {
        assertThat(adapter.activeVersionNo(workspaceId)).isEmpty();
    }
}
