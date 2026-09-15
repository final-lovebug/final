package com.ubidict.backend.draftdictionary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.fixture.DraftDictionaryFixture;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import com.ubidict.backend.draftdictionary.service.model.AddCandidateTermCommand;
import com.ubidict.backend.draftdictionary.service.model.CompleteExamineCommand;
import com.ubidict.backend.draftdictionary.service.model.DraftDictionaryResult;
import com.ubidict.backend.draftdictionary.service.model.DraftDictionarySearchQuery;
import com.ubidict.backend.draftdictionary.service.model.UpdateSourceDocumentsCommand;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Permission;
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

class DraftDictionaryServiceTest extends IntegrationTestSupport {

    private static final Long WORKSPACE_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long REGULAR_ID = 3L;
    private static final Long STRANGER_ID = 4L;

    @Autowired
    private DraftDictionaryService draftDictionaryService;

    @Autowired
    private CandidateTermService candidateTermService;

    @Autowired
    private DraftDictionaryRepository draftDictionaryRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @BeforeEach
    void createWorkspace() {
        Long workspaceId =
                workspaceRepository.save(WorkspaceFixture.workspace().build()).getId();
        participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(MEMBER_ID)
                .permission(Permission.ADMIN)
                .build());
        participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(REGULAR_ID)
                .permission(Permission.REGULAR)
                .build());
    }

    @DisplayName("워크스페이스 기준으로 진행 중 사전 초안 목록을 조회한다(T-INT-20).")
    @Test
    void search() {
        createDraft(List.of(10L));

        var result = draftDictionaryService.search(
                new DraftDictionarySearchQuery(WORKSPACE_ID, null, 0, 20, "createdAt,desc", MEMBER_ID));

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isOne();
    }

    @DisplayName("상태로 필터링해 사전 초안 목록을 조회한다.")
    @Test
    void search_filterByStatus() {
        Long draftDictionaryId = createDraft(List.of(10L)).draftDictionaryId();
        draftDictionaryService.completeExamine(new CompleteExamineCommand(draftDictionaryId, MEMBER_ID));

        var examining = draftDictionaryService.search(new DraftDictionarySearchQuery(
                WORKSPACE_ID, DraftDictionaryStatus.EXAMINING, 0, 20, "createdAt,desc", MEMBER_ID));
        var examined = draftDictionaryService.search(new DraftDictionarySearchQuery(
                WORKSPACE_ID, DraftDictionaryStatus.EXAMINED, 0, 20, "createdAt,desc", MEMBER_ID));

        assertThat(examining.content()).isEmpty();
        assertThat(examined.content()).hasSize(1);
    }

    @DisplayName("참여자가 아니면 목록을 조회할 수 없다.")
    @Test
    void search_notParticipant() {
        createDraft(List.of(10L));

        assertThatThrownBy(() -> draftDictionaryService.search(
                        new DraftDictionarySearchQuery(WORKSPACE_ID, null, 0, 20, "createdAt,desc", STRANGER_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    @DisplayName("사전 초안의 유래 문서 목록을 교체한다.")
    @Test
    void updateSourceDocuments() {
        DraftDictionaryResult created = createDraft(List.of(10L));

        DraftDictionaryResult updated = draftDictionaryService.updateSourceDocuments(
                new UpdateSourceDocumentsCommand(created.draftDictionaryId(), List.of(20L, 30L), MEMBER_ID));

        assertThat(updated.sourceDocumentIds()).containsExactly(20L, 30L);
        assertThat(draftDictionaryService
                        .read(created.draftDictionaryId(), MEMBER_ID)
                        .sourceDocumentIds())
                .containsExactlyInAnyOrder(20L, 30L);
    }

    @DisplayName("사전 초안을 삭제하면 이후 조회할 수 없다.")
    @Test
    void delete() {
        DraftDictionaryResult created = createDraft(List.of(10L));

        draftDictionaryService.delete(created.draftDictionaryId(), MEMBER_ID);

        assertThat(draftDictionaryRepository.findByIdAndDeletedAtIsNull(created.draftDictionaryId()))
                .isEmpty();
    }

    @DisplayName("판정하지 않은 후보어만 있어도 대표어와 정의가 채워져 있으면 교정을 완료한다.")
    @Test
    void completeExamine() {
        // 교정 완료 조건은 판정 여부가 아니라 후보어가 온전한지다(D-87). 초안 화면에서 개별 판정을
        // 걷어냈으므로 새 후보어는 계속 PENDING이고, 예전 조건을 그대로 두면 흐름이 영구히 막힌다.
        Long draftDictionaryId = createDraft(List.of(10L)).draftDictionaryId();
        addCandidate(draftDictionaryId, "미판정어", "미판정 정의");

        DraftDictionaryResult result =
                draftDictionaryService.completeExamine(new CompleteExamineCommand(draftDictionaryId, MEMBER_ID));

        assertThat(result.status()).isEqualTo(DraftDictionaryStatus.EXAMINED);
    }

    @DisplayName("정의가 빈 후보어가 있으면 교정을 완료할 수 없다.")
    @Test
    void completeExamine_definitionMissing() {
        Long draftDictionaryId = createDraft(List.of(10L)).draftDictionaryId();
        addCandidate(draftDictionaryId, "정의없는어", null);

        assertThatThrownBy(() -> draftDictionaryService.completeExamine(
                        new CompleteExamineCommand(draftDictionaryId, MEMBER_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_CANDIDATE_DEFINITION_REQUIRED);
    }

    @DisplayName("이미 교정 완료된 초안을 다시 완료할 수 없다.")
    @Test
    void completeExamine_alreadyExamined() {
        Long draftDictionaryId = createDraft(List.of(10L)).draftDictionaryId();
        draftDictionaryService.completeExamine(new CompleteExamineCommand(draftDictionaryId, MEMBER_ID));

        assertThatThrownBy(() -> draftDictionaryService.completeExamine(
                        new CompleteExamineCommand(draftDictionaryId, MEMBER_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_ALREADY_EXAMINED);
    }

    @DisplayName("교정 완료된 초안에 실제 변경 항목이 있으면 리뷰 요청 자격이 있다.")
    @Test
    void validateReviewReadiness() {
        Long draftDictionaryId = createDraft(List.of(10L)).draftDictionaryId();
        addCandidate(draftDictionaryId, "신규어", "신규 정의");
        draftDictionaryService.completeExamine(new CompleteExamineCommand(draftDictionaryId, MEMBER_ID));

        assertThatCode(() -> draftDictionaryService.validateReviewReadiness(draftDictionaryId))
                .doesNotThrowAnyException();
    }

    @DisplayName("교정 완료 전에는 리뷰 요청 자격이 없다.")
    @Test
    void validateReviewReadiness_notExamined() {
        Long draftDictionaryId = createDraft(List.of(10L)).draftDictionaryId();

        assertThatThrownBy(() -> draftDictionaryService.validateReviewReadiness(draftDictionaryId))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NOT_EXAMINED);
    }

    @DisplayName("이전 버전과 달라진 등재 대상이 없으면 리뷰 요청 자격이 없다.")
    @Test
    void validateReviewReadiness_noChangedItem() {
        Long draftDictionaryId = createDraft(List.of(10L)).draftDictionaryId();
        draftDictionaryService.completeExamine(new CompleteExamineCommand(draftDictionaryId, MEMBER_ID));

        assertThatThrownBy(() -> draftDictionaryService.validateReviewReadiness(draftDictionaryId))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NO_CHANGED_ITEM);
    }

    /**
     * 초안을 만드는 진입점은 비동기 추출 작업뿐이므로(D-45) 교정 이후 흐름만 보는 테스트는 초안을 직접 만든다. 생성 자체는
     * DraftDictionaryExtractionExecutionServiceTest가, 유래 문서 검증은 DraftDictionaryTest가 검증한다.
     */
    private DraftDictionaryResult createDraft(List<Long> sourceDocumentIds) {
        DraftDictionary draftDictionary = draftDictionaryRepository.save(DraftDictionaryFixture.draftDictionary()
                .workspaceId(WORKSPACE_ID)
                .sourceDocumentIds(sourceDocumentIds)
                .createdBy(MEMBER_ID)
                .build());
        return draftDictionaryService.read(draftDictionary.getId(), MEMBER_ID);
    }

    private Long addCandidate(Long draftDictionaryId, String form, String definition) {
        return candidateTermService
                .add(new AddCandidateTermCommand(
                        draftDictionaryId, form, definition, null, List.of(10L), 1, List.of("문맥"), MEMBER_ID))
                .candidateTermId();
    }
}
