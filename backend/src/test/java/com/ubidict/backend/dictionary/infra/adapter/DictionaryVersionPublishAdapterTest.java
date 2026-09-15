package com.ubidict.backend.dictionary.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.exception.DictionaryErrorCode;
import com.ubidict.backend.dictionary.fixture.TermFixture;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import com.ubidict.backend.dictionary.infra.TermRepository;
import com.ubidict.backend.dictionary.service.DictionaryService;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DictionaryVersionPublishAdapterTest extends IntegrationTestSupport {

    private static final Long PUBLISHED_BY = 1L;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    private DictionaryVersionPublishAdapter adapter;

    @BeforeEach
    void setUpAdapter() {
        adapter = new DictionaryVersionPublishAdapter(dictionaryService);
    }

    @DisplayName("사전집이 없으면 첫 버전을 발행한다.")
    @Test
    void publish_firstVersion() {
        Long workspaceId = createWorkspace();

        int resultVersionNo = adapter.publish(workspaceId, 0, TermFixture.snapshots("회원", "문서"), PUBLISHED_BY);

        var active = dictionaryRepository
                .findByWorkspaceIdAndStatus(workspaceId, DictionaryStatus.ACTIVE)
                .orElseThrow();
        assertThat(resultVersionNo).isEqualTo(1);
        assertThat(active.getCreatedBy()).isEqualTo(PUBLISHED_BY);
        assertThat(termRepository.findAllByDictionaryIdOrderByPreferredFormAsc(active.getId()))
                .allSatisfy(term -> assertThat(term.getCreatedBy()).isEqualTo(PUBLISHED_BY));
    }

    @DisplayName("새 버전을 발행하면 이전 활성 사전집이 보관 상태로 내려간다.")
    @Test
    void publish_nextVersionArchivesPrevious() {
        Long workspaceId = createWorkspace();
        adapter.publish(workspaceId, 0, TermFixture.snapshots("회원"), PUBLISHED_BY);

        int resultVersionNo = adapter.publish(workspaceId, 1, TermFixture.snapshots("회원", "문서"), PUBLISHED_BY);

        assertThat(resultVersionNo).isEqualTo(2);
        assertThat(dictionaryRepository.findAllByWorkspaceIdOrderByVersionVersionNoDesc(workspaceId))
                .extracting(dictionary -> dictionary.getStatus())
                .containsExactly(DictionaryStatus.ACTIVE, DictionaryStatus.ARCHIVED);
    }

    @DisplayName("현재 활성 버전과 기준 버전이 다르면 409로 거절한다.")
    @Test
    void publish_baseVersionNoMismatch() {
        Long workspaceId = createWorkspace();
        adapter.publish(workspaceId, 0, TermFixture.snapshots("회원"), PUBLISHED_BY);

        assertThatThrownBy(() -> adapter.publish(workspaceId, 0, TermFixture.snapshots("문서"), PUBLISHED_BY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DictionaryErrorCode.DICTIONARY_VERSION_CONFLICT);
    }

    @DisplayName("용어가 없는 사전집 버전은 발행할 수 없다.")
    @Test
    void publish_emptyTerms() {
        Long workspaceId = createWorkspace();

        assertThatThrownBy(() -> adapter.publish(workspaceId, 0, List.of(), PUBLISHED_BY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DictionaryErrorCode.DICTIONARY_EMPTY_TERMS);
        assertThat(dictionaryRepository.findAll()).isEmpty();
    }

    private Long createWorkspace() {
        Long workspaceId =
                workspaceRepository.save(WorkspaceFixture.workspace().build()).getId();
        participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(PUBLISHED_BY)
                .permission(Permission.OWNER)
                .build());
        return workspaceId;
    }
}
