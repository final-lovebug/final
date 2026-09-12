package com.ubidict.backend.draftdictionary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.CandidateTermStatus;
import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import com.ubidict.backend.draftdictionary.service.model.AddCandidateTermCommand;
import com.ubidict.backend.draftdictionary.service.model.CompleteExamineCommand;
import com.ubidict.backend.draftdictionary.service.model.CreateDraftDictionaryCommand;
import com.ubidict.backend.draftdictionary.service.model.DecideCandidateTermCommand;
import com.ubidict.backend.draftdictionary.service.model.DraftDictionaryResult;
import com.ubidict.backend.draftdictionary.service.model.ExamineProgressResult;
import com.ubidict.backend.draftdictionary.service.model.RequestDictionaryReviewCommand;
import com.ubidict.backend.draftdictionary.service.model.UpdateSourceDocumentsCommand;
import com.ubidict.backend.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DraftDictionaryServiceTest extends IntegrationTestSupport {

    private static final Long WORKSPACE_ID = 1L;
    private static final Long MEMBER_ID = 2L;

    @Autowired
    private DraftDictionaryService draftDictionaryService;

    @Autowired
    private CandidateTermService candidateTermService;

    @Autowired
    private DraftDictionaryRepository draftDictionaryRepository;

    @DisplayName("사전집이 없는 첫 회차의 사전 초안을 생성한다.")
    @Test
    void create() {
        DraftDictionaryResult result = createDraft(List.of(10L));

        assertThat(result.draftDictionaryId()).isNotNull();
        assertThat(result.workspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(result.dictionaryId()).isNull();
        assertThat(result.status()).isEqualTo(DraftDictionaryStatus.EXAMINING);
        assertThat(result.createdBy()).isEqualTo(MEMBER_ID);
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

    @DisplayName("중복된 유래 문서로 사전 초안을 생성할 수 없다.")
    @Test
    void create_duplicateSourceDocument() {
        assertThatThrownBy(() -> createDraft(List.of(10L, 10L)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_DUPLICATE_SOURCE_DOCUMENT);
    }

    @DisplayName("미판정 후보어가 없으면 교정을 완료한다.")
    @Test
    void completeExamine() {
        Long draftDictionaryId = createDraft(List.of(10L)).draftDictionaryId();
        Long candidateTermId = addCandidate(draftDictionaryId, "보류어", "보류 정의");
        decide(candidateTermId, CandidateTermStatus.ON_HOLD);

        DraftDictionaryResult result =
                draftDictionaryService.completeExamine(new CompleteExamineCommand(draftDictionaryId, MEMBER_ID));

        assertThat(result.status()).isEqualTo(DraftDictionaryStatus.EXAMINED);
    }

    @DisplayName("미판정 후보어가 있으면 교정을 완료할 수 없다.")
    @Test
    void completeExamine_pendingCandidate() {
        Long draftDictionaryId = createDraft(List.of(10L)).draftDictionaryId();
        addCandidate(draftDictionaryId, "미판정어", "미판정 정의");

        assertThatThrownBy(() -> draftDictionaryService.completeExamine(
                        new CompleteExamineCommand(draftDictionaryId, MEMBER_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_CANDIDATE_TERM_UNDECIDED_EXISTS);
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

    @DisplayName("교정 완료된 초안에 실제 변경 항목이 있으면 리뷰 요청 상태로 전이한다.")
    @Test
    void requestReview() {
        Long draftDictionaryId = createDraft(List.of(10L)).draftDictionaryId();
        Long candidateTermId = addCandidate(draftDictionaryId, "신규어", "신규 정의");
        decide(candidateTermId, CandidateTermStatus.REGISTRATION_APPROVED);
        draftDictionaryService.completeExamine(new CompleteExamineCommand(draftDictionaryId, MEMBER_ID));

        DraftDictionaryResult result =
                draftDictionaryService.requestReview(new RequestDictionaryReviewCommand(draftDictionaryId, MEMBER_ID));

        assertThat(result.status()).isEqualTo(DraftDictionaryStatus.REVIEW_REQUESTED);
    }

    @DisplayName("교정 완료 전에는 리뷰를 요청할 수 없다.")
    @Test
    void requestReview_notExamined() {
        Long draftDictionaryId = createDraft(List.of(10L)).draftDictionaryId();

        assertThatThrownBy(() -> draftDictionaryService.requestReview(
                        new RequestDictionaryReviewCommand(draftDictionaryId, MEMBER_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NOT_EXAMINED);
    }

    @DisplayName("이전 버전과 달라진 등재 대상이 없으면 리뷰를 요청할 수 없다.")
    @Test
    void requestReview_noChangedItem() {
        Long draftDictionaryId = createDraft(List.of(10L)).draftDictionaryId();
        draftDictionaryService.completeExamine(new CompleteExamineCommand(draftDictionaryId, MEMBER_ID));

        assertThatThrownBy(() -> draftDictionaryService.requestReview(
                        new RequestDictionaryReviewCommand(draftDictionaryId, MEMBER_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NO_CHANGED_ITEM);
    }

    @DisplayName("이미 리뷰를 요청한 초안은 다시 요청할 수 없다.")
    @Test
    void requestReview_alreadyRequested() {
        Long draftDictionaryId = createDraft(List.of(10L)).draftDictionaryId();
        Long candidateTermId = addCandidate(draftDictionaryId, "신규어", "신규 정의");
        decide(candidateTermId, CandidateTermStatus.REGISTRATION_APPROVED);
        draftDictionaryService.completeExamine(new CompleteExamineCommand(draftDictionaryId, MEMBER_ID));
        draftDictionaryService.requestReview(new RequestDictionaryReviewCommand(draftDictionaryId, MEMBER_ID));

        assertThatThrownBy(() -> draftDictionaryService.requestReview(
                        new RequestDictionaryReviewCommand(draftDictionaryId, MEMBER_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_ALREADY_REVIEW_REQUESTED);
    }

    @DisplayName("후보어 상태별 교정 진행률을 조회한다.")
    @Test
    void readExamineProgress() {
        Long draftDictionaryId = createDraft(List.of(10L)).draftDictionaryId();
        addCandidate(draftDictionaryId, "미판정어", "정의1");
        Long approvedId = addCandidate(draftDictionaryId, "승인어", "정의2");
        Long rejectedId = addCandidate(draftDictionaryId, "거절어", "정의3");
        Long holdId = addCandidate(draftDictionaryId, "보류어", "정의4");
        decide(approvedId, CandidateTermStatus.REGISTRATION_APPROVED);
        candidateTermService.decide(
                new DecideCandidateTermCommand(rejectedId, MEMBER_ID, CandidateTermStatus.REJECTED, "오등록", null));
        decide(holdId, CandidateTermStatus.ON_HOLD);

        ExamineProgressResult result = draftDictionaryService.readExamineProgress(draftDictionaryId, MEMBER_ID);

        assertThat(result.total()).isEqualTo(4);
        assertThat(result.pending()).isOne();
        assertThat(result.approved()).isOne();
        assertThat(result.rejected()).isOne();
        assertThat(result.onHold()).isOne();
        assertThat(result.kept()).isZero();
        assertThat(result.merged()).isZero();
    }

    private DraftDictionaryResult createDraft(List<Long> sourceDocumentIds) {
        return draftDictionaryService.create(
                new CreateDraftDictionaryCommand(WORKSPACE_ID, null, sourceDocumentIds, MEMBER_ID));
    }

    private Long addCandidate(Long draftDictionaryId, String form, String definition) {
        return candidateTermService
                .add(new AddCandidateTermCommand(
                        draftDictionaryId, form, definition, null, List.of(10L), 1, List.of("문맥"), MEMBER_ID))
                .candidateTermId();
    }

    private void decide(Long candidateTermId, CandidateTermStatus status) {
        candidateTermService.decide(new DecideCandidateTermCommand(candidateTermId, MEMBER_ID, status, null, null));
    }
}
