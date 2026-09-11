package com.ubidict.backend.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.dictionary.fixture.DictionaryFixture;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.exception.DocumentErrorCode;
import com.ubidict.backend.document.fixture.DocumentFixture;
import com.ubidict.backend.document.fixture.DocumentVersionFixture;
import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.document.infra.DocumentVersionRepository;
import com.ubidict.backend.document.infra.LabelRepository;
import com.ubidict.backend.document.service.model.CreateDocumentCommand;
import com.ubidict.backend.document.service.model.DocumentResult;
import com.ubidict.backend.document.service.model.DocumentSummaryResult;
import com.ubidict.backend.document.service.model.DocumentVersionResult;
import com.ubidict.backend.document.service.model.DocumentVersionSummaryResult;
import com.ubidict.backend.document.service.model.EditDocumentContentCommand;
import com.ubidict.backend.document.service.model.LabelResult;
import com.ubidict.backend.document.service.model.UpdateDocumentCommand;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.crossdomain.dictionary.mode=real")
class DocumentServiceTest extends IntegrationTestSupport {

    private static final Long OWNER_ID = 1L;
    private static final Long REGULAR_ID = 2L;
    private static final Long STRANGER_ID = 3L;
    private static final String TITLE = "결제 도메인 설계";

    @Autowired
    private DocumentService documentService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentVersionRepository documentVersionRepository;

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private LabelRepository labelRepository;

    private Long workspaceId;

    @BeforeEach
    void createWorkspace() {
        workspaceId = openWorkspace("개발팀", OWNER_ID);
        joinAs(workspaceId, REGULAR_ID, Permission.REGULAR);
    }

    @DisplayName("문서를 만들면 v1 버전이 함께 발행된다.")
    @Test
    void create() {
        // when
        DocumentResult result = create(TITLE, "회원은 결제할 수 있다.", List.of());

        // then
        assertThat(result.currentVersionNo()).isEqualTo(1);
        assertThat(documentVersionRepository.findByDocumentIdAndVersionVersionNo(result.documentId(), 1))
                .isPresent();
    }

    /**
     * 본문이 버전에만 있다는 결정의 전제. 상세 응답의 content는 문서가 아니라 최신 확정 버전에서 온다.
     */
    @DisplayName("문서 본문은 v1 버전에서 읽어 온다.")
    @Test
    void create_contentComesFromVersion() {
        // given
        DocumentResult created = create(TITLE, "회원은 결제할 수 있다.", List.of());

        // when
        DocumentResult result = documentService.read(workspaceId, created.documentId(), OWNER_ID);

        // then
        assertThat(result.content()).isEqualTo("회원은 결제할 수 있다.");
    }

    @DisplayName("본문이 10,000자를 넘으면 문서가 만들어지지 않는다.")
    @Test
    void create_contentIsTooLong() {
        // when & then
        assertThatThrownBy(() -> create(TITLE, DocumentFixture.TOO_LONG_BODY, List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DocumentErrorCode.DOCUMENT_INVALID_CONTENT);
    }

    @DisplayName("참여자가 아니면 권한 부족이 아니라 조회 실패로 응답한다.")
    @Test
    void create_memberIsNotParticipant() {
        // when & then
        assertThatThrownBy(() -> documentService.create(
                        new CreateDocumentCommand(workspaceId, TITLE, "본문", List.of(), STRANGER_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    /**
     * 소프트 삭제는 참여자 행을 남긴다. document는 워크스페이스를 읽을 이유가 없어 이 구멍을 밟기 쉽다.
     */
    @DisplayName("삭제된 워크스페이스의 문서는 조회되지 않는다.")
    @Test
    void read_workspaceIsDeleted() {
        // given
        DocumentResult created = create(TITLE, "본문", List.of());
        deleteWorkspace(workspaceId);

        // when & then
        assertThatThrownBy(() -> documentService.read(workspaceId, created.documentId(), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    @DisplayName("다른 워크스페이스의 문서는 조회되지 않는다.")
    @Test
    void read_documentBelongsToOtherWorkspace() {
        // given
        DocumentResult created = create(TITLE, "본문", List.of());
        Long otherWorkspaceId = openWorkspace("플랫폼팀", OWNER_ID);

        // when & then
        assertThatThrownBy(() -> documentService.read(otherWorkspaceId, created.documentId(), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DocumentErrorCode.DOCUMENT_NOT_FOUND);
    }

    @DisplayName("목록에는 내 워크스페이스의 문서만 나온다.")
    @Test
    void readAll() {
        // given
        create(TITLE, "본문", List.of());
        Long otherWorkspaceId = openWorkspace("플랫폼팀", OWNER_ID);
        documentService.create(new CreateDocumentCommand(otherWorkspaceId, "남의 문서", "본문", List.of(), OWNER_ID));

        // when
        List<DocumentSummaryResult> results = documentService.readAll(workspaceId, OWNER_ID, null);

        // then
        assertThat(results).extracting(DocumentSummaryResult::title).containsExactly(TITLE);
    }

    @DisplayName("활성 사전집이 없으면 정렬된 것으로 응답한다.")
    @Test
    void readAll_dictionaryIsAbsent() {
        // given
        create(TITLE, "본문", List.of());

        // when
        List<DocumentSummaryResult> results = documentService.readAll(workspaceId, OWNER_ID, null);

        // then
        assertThat(results).allMatch(DocumentSummaryResult::aligned);
    }

    @DisplayName("사전집이 새 버전을 발행하면 문서가 정렬되지 않은 것으로 바뀐다.")
    @Test
    void readAll_alignedIsFalseWhenDictionaryIsRevised() {
        // given
        saveDocumentWithVersion(1, false);
        saveActiveDictionary(2);

        // when
        List<DocumentSummaryResult> results = documentService.readAll(workspaceId, OWNER_ID, null);

        // then
        assertThat(results)
                .singleElement()
                .extracting(DocumentSummaryResult::aligned)
                .isEqualTo(false);
    }

    @DisplayName("문서 상세는 활성 사전집과 같은 기준 버전을 정렬된 것으로 응답한다.")
    @Test
    void read_alignedWithActiveDictionary() {
        // given
        Document document = saveDocumentWithVersion(2, false);
        saveActiveDictionary(2);

        // when
        DocumentResult result = documentService.read(workspaceId, document.getId(), OWNER_ID);

        // then
        assertThat(result.aligned()).isTrue();
        assertThat(result.dictionaryVersionNo()).isEqualTo(2);
    }

    @DisplayName("라벨을 붙이면 목록에 함께 나온다.")
    @Test
    void create_withLabels() {
        // when
        DocumentResult result = create(TITLE, "본문", List.of("설계", "결제"));

        // then
        assertThat(result.labels()).containsExactly("결제", "설계");
    }

    @DisplayName("없는 이름을 붙이면 라벨이 새로 만들어진다.")
    @Test
    void create_labelIsCreated() {
        // when
        create(TITLE, "본문", List.of("설계"));

        // then
        assertThat(labelRepository.findAllByWorkspaceIdOrderByNameAsc(workspaceId))
                .hasSize(1);
    }

    @DisplayName("이미 있는 이름을 붙이면 라벨을 재사용한다.")
    @Test
    void create_labelIsReused() {
        // given
        create("첫 문서", "본문", List.of("설계"));

        // when
        create("둘째 문서", "본문", List.of("설계"));

        // then
        assertThat(labelRepository.findAllByWorkspaceIdOrderByNameAsc(workspaceId))
                .hasSize(1);
    }

    @DisplayName("라벨이 6개면 예외가 발생한다.")
    @Test
    void create_labelLimitIsExceeded() {
        // given
        List<String> tooMany = List.of("하나", "둘", "셋", "넷", "다섯", "여섯");

        // when & then
        assertThatThrownBy(() -> create(TITLE, "본문", tooMany))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DocumentErrorCode.DOCUMENT_LABEL_LIMIT_EXCEEDED);
    }

    @DisplayName("라벨 필터는 해당 라벨이 붙은 문서만 준다.")
    @Test
    void readAll_filterByLabel() {
        // given
        create("설계 문서", "본문", List.of("설계"));
        create("정산 문서", "본문", List.of("정산"));

        // when
        List<DocumentSummaryResult> results = documentService.readAll(workspaceId, OWNER_ID, "설계");

        // then
        assertThat(results).extracting(DocumentSummaryResult::title).containsExactly("설계 문서");
    }

    @DisplayName("제목을 바꾸면 최종수정자가 갱신된다.")
    @Test
    void update() {
        // given
        DocumentResult created = create(TITLE, "본문", List.of());

        // when
        documentService.update(
                new UpdateDocumentCommand(workspaceId, created.documentId(), "정산 도메인 설계", List.of(), REGULAR_ID));

        // then
        DocumentResult result = documentService.read(workspaceId, created.documentId(), OWNER_ID);
        assertThat(result.title()).isEqualTo("정산 도메인 설계");
    }

    @DisplayName("라벨을 교체하면 이전 라벨이 떨어지고 새 라벨이 붙는다.")
    @Test
    void update_replacesLabels() {
        // given
        DocumentResult created = create(TITLE, "본문", List.of("설계"));

        // when
        documentService.update(
                new UpdateDocumentCommand(workspaceId, created.documentId(), TITLE, List.of("정산"), OWNER_ID));

        // then
        assertThat(documentService
                        .read(workspaceId, created.documentId(), OWNER_ID)
                        .labels())
                .containsExactly("정산");
    }

    @DisplayName("문서에서 라벨을 떼도 라벨 자체는 워크스페이스에 남는다.")
    @Test
    void update_detachedLabelSurvives() {
        // given
        DocumentResult created = create(TITLE, "본문", List.of("설계"));

        // when
        documentService.update(
                new UpdateDocumentCommand(workspaceId, created.documentId(), TITLE, List.of(), OWNER_ID));

        // then
        assertThat(documentService.readLabels(workspaceId, OWNER_ID))
                .extracting(LabelResult::name)
                .containsExactly("설계");
    }

    @DisplayName("본문을 편집하면 새 버전이 발행된다.")
    @Test
    void editContent_publishesNewVersion() {
        // given
        DocumentResult created = create(TITLE, "첫 본문", List.of());

        // when
        DocumentResult result = documentService.editContent(
                new EditDocumentContentCommand(workspaceId, created.documentId(), "편집한 본문", REGULAR_ID));

        // then
        assertThat(result.currentVersionNo()).isEqualTo(2);
        assertThat(result.content()).isEqualTo("편집한 본문");
        assertThat(result.edited()).isTrue();
        assertThat(documentVersionRepository.findByDocumentIdAndVersionVersionNo(created.documentId(), 2))
                .isPresent();
    }

    @DisplayName("직접 편집본은 이전 버전의 기준 사전집 버전을 승계한다.")
    @Test
    void editContent_inheritsDictionaryVersionNo() {
        // given
        Document document = saveDocumentWithVersion(3, false);

        // when
        DocumentResult result = documentService.editContent(
                new EditDocumentContentCommand(workspaceId, document.getId(), "편집한 본문", OWNER_ID));

        // then
        assertThat(result.dictionaryVersionNo()).isEqualTo(3);
        assertThat(documentVersionRepository
                        .findByDocumentIdAndVersionVersionNo(document.getId(), 2)
                        .orElseThrow()
                        .getDictionaryVersionNo())
                .isEqualTo(3);
    }

    @DisplayName("REGULAR는 문서를 삭제할 수 없다.")
    @Test
    void delete_permissionIsBelowAdmin() {
        // given
        DocumentResult created = create(TITLE, "본문", List.of());

        // when & then
        assertThatThrownBy(() -> documentService.delete(workspaceId, created.documentId(), REGULAR_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_ADMIN_REQUIRED);
    }

    @DisplayName("ADMIN 이상이 삭제하면 이후 조회에서 사라진다.")
    @Test
    void delete() {
        // given
        DocumentResult created = create(TITLE, "본문", List.of());

        // when
        documentService.delete(workspaceId, created.documentId(), OWNER_ID);

        // then
        assertThatThrownBy(() -> documentService.read(workspaceId, created.documentId(), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DocumentErrorCode.DOCUMENT_NOT_FOUND);
    }

    /**
     * 소프트 삭제이므로 확정된 버전 이력은 보존된다. 조회가 문서에서 먼저 막힐 뿐이다.
     */
    @DisplayName("문서를 삭제해도 버전 행은 남는다.")
    @Test
    void delete_versionsSurvive() {
        // given
        DocumentResult created = create(TITLE, "본문", List.of());

        // when
        documentService.delete(workspaceId, created.documentId(), OWNER_ID);

        // then
        assertThat(documentVersionRepository.findSummariesByDocumentId(created.documentId()))
                .hasSize(1);
    }

    @DisplayName("버전 이력은 버전 번호 내림차순으로 조회된다.")
    @Test
    void readVersions() {
        // given
        DocumentResult created = create(TITLE, "본문", List.of());
        publishSecondVersion(created.documentId());

        // when
        List<DocumentVersionSummaryResult> results =
                documentService.readVersions(workspaceId, created.documentId(), OWNER_ID);

        // then
        assertThat(results).extracting(DocumentVersionSummaryResult::versionNo).containsExactly(2, 1);
    }

    @DisplayName("특정 버전을 조회하면 그 시점 본문이 나온다.")
    @Test
    void readVersion() {
        // given
        DocumentResult created = create(TITLE, "첫 본문", List.of());

        // when
        DocumentVersionResult result = documentService.readVersion(workspaceId, created.documentId(), 1, OWNER_ID);

        // then
        assertThat(result.body()).isEqualTo("첫 본문");
    }

    @DisplayName("없는 버전 번호를 조회하면 예외가 발생한다.")
    @Test
    void readVersion_versionIsAbsent() {
        // given
        DocumentResult created = create(TITLE, "본문", List.of());

        // when & then
        assertThatThrownBy(() -> documentService.readVersion(workspaceId, created.documentId(), 99, OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DocumentErrorCode.DOCUMENT_VERSION_NOT_FOUND);
    }

    @DisplayName("다른 워크스페이스 문서의 버전 이력은 조회되지 않는다.")
    @Test
    void readVersions_documentBelongsToOtherWorkspace() {
        // given
        DocumentResult created = create(TITLE, "본문", List.of());
        Long otherWorkspaceId = openWorkspace("플랫폼팀", OWNER_ID);

        // when & then
        assertThatThrownBy(() -> documentService.readVersions(otherWorkspaceId, created.documentId(), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DocumentErrorCode.DOCUMENT_NOT_FOUND);
    }

    private DocumentResult create(String title, String content, List<String> labels) {
        return documentService.create(new CreateDocumentCommand(workspaceId, title, content, labels, OWNER_ID));
    }

    /**
     * 특정 버전 이력 조회만 검증하므로 편집 유스케이스를 거치지 않고 v2를 저장한다.
     */
    private void publishSecondVersion(Long documentId) {
        documentVersionRepository.save(DocumentVersionFixture.documentVersion()
                .documentId(documentId)
                .versionNo(2)
                .body("둘째 본문")
                .build());
    }

    private Document saveDocumentWithVersion(int dictionaryVersionNo, boolean edited) {
        Document document = documentRepository.save(
                DocumentFixture.document().workspaceId(workspaceId).build());
        documentVersionRepository.save(DocumentVersionFixture.documentVersion()
                .documentId(document.getId())
                .dictionaryVersionNo(dictionaryVersionNo)
                .edited(edited)
                .build());
        return document;
    }

    private void saveActiveDictionary(int versionNo) {
        dictionaryRepository.save(DictionaryFixture.dictionary()
                .workspaceId(workspaceId)
                .versionNo(versionNo)
                .build());
    }

    private Long openWorkspace(String name, Long ownerId) {
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().name(name).build());
        joinAs(workspace.getId(), ownerId, Permission.OWNER);

        return workspace.getId();
    }

    private void joinAs(Long workspaceId, Long memberId, Permission permission) {
        participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(memberId)
                .permission(permission)
                .build());
    }

    private void deleteWorkspace(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow();
        workspace.delete();
        workspaceRepository.save(workspace);
    }
}
