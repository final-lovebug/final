package com.ubidict.backend.document.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.document.infra.LabelRepository;
import com.ubidict.backend.document.service.model.CreateDocumentCommand;
import com.ubidict.backend.document.service.model.DocumentResult;
import com.ubidict.backend.document.service.model.DocumentSummaryResult;
import com.ubidict.backend.document.service.model.UpdateDocumentCommand;
import com.ubidict.backend.support.MySqlIntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 라벨 대소문자 정합(`D-94`) 중 <b>DB가 판정 주체인 것</b>만 모았다.
 *
 * <p>{@link DocumentServiceTest}에서 떼어 온 이유는 하나다 — 이 셋은 MySQL의 {@code utf8mb4_0900_ai_ci}가 대소문자를
 * 접는다는 사실에 기대므로 H2에서는 통과할 수 없다. 나머지 문서 서비스 테스트는 H2로 충분하고 훨씬 빠르므로 그대로 두고,
 * 컨테이너가 필요한 것만 여기로 옮겼다.
 *
 * <p>자바 쪽 접기 규칙({@code Label.matchKey})은 단위 테스트가 따로 본다.
 */
class DocumentLabelCaseInsensitivityTest extends MySqlIntegrationTestSupport {

    private static final Long OWNER_ID = 1L;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private LabelRepository labelRepository;

    private Long workspaceId;

    @BeforeEach
    void createWorkspace() {
        Workspace workspace = workspaceRepository.save(
                WorkspaceFixture.workspace().name("개발팀").build());
        participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspace.getId())
                .memberId(OWNER_ID)
                .permission(Permission.OWNER)
                .build());
        workspaceId = workspace.getId();
    }

    /**
     * Y-36의 재현 케이스. 고치기 전에는 uk_label_workspace_name 위반이 세션을 rollback-only로 만들어 문서 생성 전체가
     * COMMON_INTERNAL_ERROR(500)로 죽었다 — 본문 길이와는 무관한 실패였다.
     */
    @DisplayName("대소문자만 다른 라벨을 붙여도 문서가 만들어지고 라벨은 재사용된다.")
    @Test
    void create_labelIsReusedIgnoringCase() {
        // given
        create("첫 문서", "본문", List.of("api"));

        // when
        DocumentResult result = create("둘째 문서", "본문", List.of("API"));

        // then
        assertThat(result.labels()).containsExactly("api");
        assertThat(labelRepository.findAllByWorkspaceIdOrderByNameAsc(workspaceId))
                .hasSize(1);
    }

    @DisplayName("라벨 수정에서도 대소문자만 다른 이름은 기존 라벨을 재사용한다.")
    @Test
    void update_labelIsReusedIgnoringCase() {
        // given
        create("첫 문서", "본문", List.of("api"));
        DocumentResult target = create("둘째 문서", "본문", List.of());

        // when
        documentService.update(
                new UpdateDocumentCommand(workspaceId, target.documentId(), "둘째 문서", List.of("Api"), OWNER_ID));

        // then
        assertThat(documentService
                        .read(workspaceId, target.documentId(), OWNER_ID)
                        .labels())
                .containsExactly("api");
        assertThat(labelRepository.findAllByWorkspaceIdOrderByNameAsc(workspaceId))
                .hasSize(1);
    }

    @DisplayName("라벨 필터는 대소문자를 구분하지 않는다.")
    @Test
    void readAll_filterByLabelIgnoringCase() {
        // given
        create("설계 문서", "본문", List.of("api"));
        create("정산 문서", "본문", List.of("정산"));

        // when
        List<DocumentSummaryResult> results = documentService.readAll(workspaceId, OWNER_ID, "API");

        // then
        assertThat(results).extracting(DocumentSummaryResult::title).containsExactly("설계 문서");
    }

    private DocumentResult create(String title, String content, List<String> labels) {
        return documentService.create(new CreateDocumentCommand(workspaceId, title, content, labels, OWNER_ID));
    }
}
