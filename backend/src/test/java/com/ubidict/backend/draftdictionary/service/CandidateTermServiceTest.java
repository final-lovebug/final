package com.ubidict.backend.draftdictionary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.domain.CandidateTermStatus;
import com.ubidict.backend.draftdictionary.domain.CandidateTermType;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.fixture.DraftDictionaryFixture;
import com.ubidict.backend.draftdictionary.infra.CandidateTermRepository;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import com.ubidict.backend.draftdictionary.service.model.AddCandidateTermCommand;
import com.ubidict.backend.draftdictionary.service.model.CandidateTermResult;
import com.ubidict.backend.draftdictionary.service.model.CompleteExamineCommand;
import com.ubidict.backend.draftdictionary.service.model.DecideCandidateTermCommand;
import com.ubidict.backend.draftdictionary.service.model.EditCandidateTermCommand;
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

class CandidateTermServiceTest extends IntegrationTestSupport {

    private static final Long WORKSPACE_ID = 1L;
    private static final Long MEMBER_ID = 2L;

    @Autowired
    private CandidateTermService candidateTermService;

    @Autowired
    private DraftDictionaryService draftDictionaryService;

    @Autowired
    private DraftDictionaryRepository draftDictionaryRepository;

    @Autowired
    private CandidateTermRepository candidateTermRepository;

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
                .memberId(3L)
                .permission(Permission.REGULAR)
                .build());
    }

    @DisplayName("후보어를 등재 승인하고 판정자를 저장한다.")
    @Test
    void decide_registrationApproval() {
        Long candidateTermId = addCandidate(createDraft(), "승인어", "승인 정의");

        CandidateTermResult result = candidateTermService.decide(new DecideCandidateTermCommand(
                candidateTermId, MEMBER_ID, CandidateTermStatus.REGISTRATION_APPROVED, null, null));

        CandidateTerm saved = find(candidateTermId);
        assertThat(result.status()).isEqualTo(CandidateTermStatus.REGISTRATION_APPROVED);
        assertThat(saved.getHandledBy()).isEqualTo(MEMBER_ID);
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isAfterOrEqualTo(saved.getCreatedAt());
    }

    @DisplayName("후보어를 동의어로 편입하고 대상 용어를 저장한다.")
    @Test
    void decide_synonymMerge() {
        Long candidateTermId = addCandidate(createDraft(), "동의어", "동의어 정의");

        candidateTermService.decide(new DecideCandidateTermCommand(
                candidateTermId, MEMBER_ID, CandidateTermStatus.MERGED_AS_SYNONYM, null, 99L));

        CandidateTerm saved = find(candidateTermId);
        assertThat(saved.getStatus()).isEqualTo(CandidateTermStatus.MERGED_AS_SYNONYM);
        assertThat(saved.getHandledBy()).isEqualTo(MEMBER_ID);
        assertThat(saved.getMergeTargetTermId()).isEqualTo(99L);
    }

    @DisplayName("후보어를 거절하고 공백을 제거한 사유를 저장한다.")
    @Test
    void decide_rejection() {
        Long candidateTermId = addCandidate(createDraft(), "거절어", "거절 정의");

        candidateTermService.decide(new DecideCandidateTermCommand(
                candidateTermId, MEMBER_ID, CandidateTermStatus.REJECTED, "  오등록  ", null));

        CandidateTerm saved = find(candidateTermId);
        assertThat(saved.getStatus()).isEqualTo(CandidateTermStatus.REJECTED);
        assertThat(saved.getHandledBy()).isEqualTo(MEMBER_ID);
        assertThat(saved.getRejectReason()).isEqualTo("오등록");
    }

    @DisplayName("후보어를 보류하고 판정자를 저장한다.")
    @Test
    void decide_hold() {
        Long candidateTermId = addCandidate(createDraft(), "보류어", "보류 정의");

        candidateTermService.decide(
                new DecideCandidateTermCommand(candidateTermId, MEMBER_ID, CandidateTermStatus.ON_HOLD, null, null));

        CandidateTerm saved = find(candidateTermId);
        assertThat(saved.getStatus()).isEqualTo(CandidateTermStatus.ON_HOLD);
        assertThat(saved.getHandledBy()).isEqualTo(MEMBER_ID);
    }

    @DisplayName("교정 중에는 이미 판정한 후보어를 다시 판정할 수 있다.")
    @Test
    void decide_redecision() {
        Long candidateTermId = addCandidate(createDraft(), "재판정어", "재판정 정의");
        candidateTermService.decide(
                new DecideCandidateTermCommand(candidateTermId, 3L, CandidateTermStatus.REJECTED, "오등록", null));

        candidateTermService.decide(new DecideCandidateTermCommand(
                candidateTermId, MEMBER_ID, CandidateTermStatus.REGISTRATION_APPROVED, null, null));

        CandidateTerm saved = find(candidateTermId);
        assertThat(saved.getStatus()).isEqualTo(CandidateTermStatus.REGISTRATION_APPROVED);
        assertThat(saved.getHandledBy()).isEqualTo(MEMBER_ID);
        assertThat(saved.getRejectReason()).isNull();
    }

    @DisplayName("교정 완료 후에는 후보어 판정을 변경할 수 없다.")
    @Test
    void decide_examinedDraft() {
        Long draftDictionaryId = createDraft();
        Long candidateTermId = addCandidate(draftDictionaryId, "완료어", "완료 정의");
        candidateTermService.decide(
                new DecideCandidateTermCommand(candidateTermId, MEMBER_ID, CandidateTermStatus.ON_HOLD, null, null));
        draftDictionaryService.completeExamine(new CompleteExamineCommand(draftDictionaryId, MEMBER_ID));

        assertThatThrownBy(() -> candidateTermService.decide(new DecideCandidateTermCommand(
                        candidateTermId, MEMBER_ID, CandidateTermStatus.REGISTRATION_APPROVED, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NOT_EXAMINABLE);
    }

    @DisplayName("교정 완료 후에는 후보어 추가, 수정, 삭제를 할 수 없다.")
    @Test
    void mutate_examinedDraft() {
        Long draftDictionaryId = createDraft();
        Long candidateTermId = addCandidate(draftDictionaryId, "완료어", "완료 정의");
        candidateTermService.decide(
                new DecideCandidateTermCommand(candidateTermId, MEMBER_ID, CandidateTermStatus.ON_HOLD, null, null));
        draftDictionaryService.completeExamine(new CompleteExamineCommand(draftDictionaryId, MEMBER_ID));

        assertNotExaminable(() -> candidateTermService.add(addCommand(draftDictionaryId, "추가어", "추가 정의")));
        assertNotExaminable(() ->
                candidateTermService.edit(new EditCandidateTermCommand(candidateTermId, "수정어", null, null, MEMBER_ID)));
        assertNotExaminable(() -> candidateTermService.delete(candidateTermId, MEMBER_ID));
    }

    @DisplayName("등록할 때 고른 분류와 등록자를 함께 돌려준다.")
    @Test
    void add_returnsTypeAndCreatedBy() {
        Long draftDictionaryId = createDraft();

        CandidateTermResult result = candidateTermService.add(new AddCandidateTermCommand(
                draftDictionaryId,
                "이용 보류",
                "계정을 잠시 멈춘 상태.",
                null,
                List.of(100L),
                1,
                List.of("문맥"),
                List.of("휴면 전환"),
                MEMBER_ID,
                CandidateTermType.SYNONYM));

        assertThat(result.type()).isEqualTo(CandidateTermType.SYNONYM);
        assertThat(result.createdBy()).isEqualTo(MEMBER_ID);
        assertThat(find(result.candidateTermId()).getType()).isEqualTo(CandidateTermType.SYNONYM);
    }

    @DisplayName("분류 없이 등록하면 비어 있다 — 추출이 만든 후보어에는 분류가 없다.")
    @Test
    void add_typeIsNullWhenNotChosen() {
        Long draftDictionaryId = createDraft();

        CandidateTermResult result = candidateTermService.add(addCommand(draftDictionaryId, "주문", "확정된 구매 건."));

        assertThat(result.type()).isNull();
    }

    @DisplayName("수정할 때 분류를 바꿀 수 있고, 넘기지 않으면 이전 분류가 남는다.")
    @Test
    void edit_changesTypeOnlyWhenGiven() {
        Long draftDictionaryId = createDraft();
        Long candidateTermId = candidateTermService
                .add(new AddCandidateTermCommand(
                        draftDictionaryId,
                        "결제 취소",
                        "승인된 결제를 되돌리는 처리.",
                        null,
                        List.of(100L),
                        1,
                        List.of("문맥"),
                        List.of(),
                        MEMBER_ID,
                        CandidateTermType.VARIANT))
                .candidateTermId();

        CandidateTermResult changed = candidateTermService.edit(
                new EditCandidateTermCommand(candidateTermId, null, null, null, MEMBER_ID, CandidateTermType.SYNONYM));
        assertThat(changed.type()).isEqualTo(CandidateTermType.SYNONYM);

        CandidateTermResult untouched = candidateTermService.edit(
                new EditCandidateTermCommand(candidateTermId, "결제 취소됨", null, null, MEMBER_ID));
        assertThat(untouched.type()).isEqualTo(CandidateTermType.SYNONYM);
    }

    /** 초안 생성 진입점은 비동기 추출 작업뿐이므로(D-45) 후보어만 보는 테스트는 초안을 직접 만든다. */
    private Long createDraft() {
        return draftDictionaryRepository
                .save(DraftDictionaryFixture.draftDictionary()
                        .workspaceId(WORKSPACE_ID)
                        .sourceDocumentIds(List.of(100L))
                        .createdBy(MEMBER_ID)
                        .build())
                .getId();
    }

    private Long addCandidate(Long draftDictionaryId, String form, String definition) {
        return candidateTermService
                .add(addCommand(draftDictionaryId, form, definition))
                .candidateTermId();
    }

    private AddCandidateTermCommand addCommand(Long draftDictionaryId, String form, String definition) {
        return new AddCandidateTermCommand(
                draftDictionaryId, form, definition, null, List.of(100L), 1, List.of("문맥"), MEMBER_ID);
    }

    private CandidateTerm find(Long candidateTermId) {
        return candidateTermRepository
                .findByIdAndDeletedAtIsNull(candidateTermId)
                .orElseThrow();
    }

    private static void assertNotExaminable(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NOT_EXAMINABLE);
    }
}
