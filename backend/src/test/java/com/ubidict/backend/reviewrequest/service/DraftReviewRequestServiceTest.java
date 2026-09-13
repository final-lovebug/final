package com.ubidict.backend.reviewrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.fixture.DocumentFixture;
import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.draftdictionary.domain.CandidateTermStatus;
import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.fixture.DraftDictionaryFixture;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import com.ubidict.backend.draftdictionary.service.CandidateTermService;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryService;
import com.ubidict.backend.draftdictionary.service.model.AddCandidateTermCommand;
import com.ubidict.backend.draftdictionary.service.model.CompleteExamineCommand;
import com.ubidict.backend.draftdictionary.service.model.DecideCandidateTermCommand;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.fixture.DraftDocumentFixture;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestStatus;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.infra.ReviewerRepository;
import com.ubidict.backend.reviewrequest.infra.RevisionDictionaryRepository;
import com.ubidict.backend.reviewrequest.infra.RevisionDocumentRepository;
import com.ubidict.backend.reviewrequest.service.model.RequestDictionaryReviewCommand;
import com.ubidict.backend.reviewrequest.service.model.RequestDocumentReviewCommand;
import com.ubidict.backend.reviewrequest.service.model.ReviewRequestResult;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 초안 → 리뷰 요청 진입점(T-INT-5). 리뷰 준비 판정을 초안 도메인에 위임하는 경로까지 실제 어댑터로 관통한다.
 */
class DraftReviewRequestServiceTest extends IntegrationTestSupport {

    private static final Long ADMIN_ID = 2L;
    private static final Long REGULAR_ID = 3L;
    private static final Long STRANGER_ID = 4L;

    @Autowired
    private DraftReviewRequestService draftReviewRequestService;

    @Autowired
    private RevisionService revisionService;

    @Autowired
    private DraftDictionaryService draftDictionaryService;

    @Autowired
    private CandidateTermService candidateTermService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DraftDocumentRepository draftDocumentRepository;

    @Autowired
    private DraftDictionaryRepository draftDictionaryRepository;

    @Autowired
    private RevisionDocumentRepository revisionDocumentRepository;

    @Autowired
    private RevisionDictionaryRepository revisionDictionaryRepository;

    @Autowired
    private ReviewerRepository reviewerRepository;

    private Long workspaceId;

    @BeforeEach
    void createWorkspace() {
        Workspace workspace = workspaceRepository.save(
                WorkspaceFixture.workspace().createdBy(ADMIN_ID).build());
        workspaceId = workspace.getId();
        participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(ADMIN_ID)
                .permission(Permission.ADMIN)
                .build());
        participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(REGULAR_ID)
                .permission(Permission.REGULAR)
                .build());
    }

    @DisplayName("교정 완료된 문서 초안으로 리뷰 요청과 개정안과 지정 리뷰어를 함께 만든다.")
    @Test
    void requestDocumentReview() {
        Long draftDocumentId = examinedDraftDocument("교정한 본문");

        ReviewRequestResult result = draftReviewRequestService.requestDocumentReview(new RequestDocumentReviewCommand(
                draftDocumentId, "정산 문서 리뷰", "개정안을 검토합니다.", List.of(REGULAR_ID), ADMIN_ID));

        assertThat(result.type()).isEqualTo(ReviewRequestType.DOCUMENT);
        assertThat(result.status()).isEqualTo(ReviewRequestStatus.PENDING_REVIEW);
        assertThat(result.workspaceId()).isEqualTo(workspaceId);
        assertThat(result.requesterId()).isEqualTo(ADMIN_ID);
        assertThat(revisionDocumentRepository.findByReviewRequestId(result.reviewRequestId()))
                .singleElement()
                .extracting(revision -> revision.getDraftDocumentId(), revision -> revision.getProposedBody())
                .containsExactly(draftDocumentId, "교정한 본문");
        assertThat(reviewerRepository.findByReviewRequestId(result.reviewRequestId()))
                .extracting(reviewer -> reviewer.getMemberId())
                .containsExactly(REGULAR_ID);
    }

    @DisplayName("리뷰 요청이 만들어지면 문서 초안이 리뷰요청됨으로 전이한다.")
    @Test
    void requestDocumentReview_transitionsDraft() throws InterruptedException {
        Long draftDocumentId = examinedDraftDocument("교정한 본문");

        draftReviewRequestService.requestDocumentReview(
                new RequestDocumentReviewCommand(draftDocumentId, "정산 문서 리뷰", null, List.of(), ADMIN_ID));

        assertThat(waitForDocumentStatus(draftDocumentId, DraftDocumentStatus.REVIEW_REQUESTED))
                .isEqualTo(DraftDocumentStatus.REVIEW_REQUESTED);
    }

    @DisplayName("교정 완료 전 문서 초안으로는 리뷰를 요청할 수 없다.")
    @Test
    void requestDocumentReview_draftIsExamining() {
        Long draftDocumentId = draftDocument(DraftDocumentStatus.EXAMINING, "교정 중 본문");

        assertThatThrownBy(() -> draftReviewRequestService.requestDocumentReview(
                        new RequestDocumentReviewCommand(draftDocumentId, "리뷰", null, List.of(), ADMIN_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_INVALID_STATUS_TRANSITION);
    }

    @DisplayName("같은 문서 초안으로 두 번 리뷰를 요청할 수 없다.")
    @Test
    void requestDocumentReview_alreadyRequested() {
        Long draftDocumentId = examinedDraftDocument("교정한 본문");
        draftReviewRequestService.requestDocumentReview(
                new RequestDocumentReviewCommand(draftDocumentId, "리뷰", null, List.of(), ADMIN_ID));

        assertThatThrownBy(() -> draftReviewRequestService.requestDocumentReview(
                        new RequestDocumentReviewCommand(draftDocumentId, "리뷰", null, List.of(), ADMIN_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ReviewRequestErrorCode.REVIEW_REQUEST_REVISION_ALREADY_EXISTS);
    }

    @DisplayName("비참여자에게는 초안의 존재를 드러내지 않고 404로 응답한다.")
    @Test
    void requestDocumentReview_notParticipant() {
        Long draftDocumentId = examinedDraftDocument("교정한 본문");

        assertThatThrownBy(() -> draftReviewRequestService.requestDocumentReview(
                        new RequestDocumentReviewCommand(draftDocumentId, "리뷰", null, List.of(), STRANGER_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    @DisplayName("없는 초안으로 리뷰를 요청하면 404로 응답한다.")
    @Test
    void requestDocumentReview_draftIsNotFound() {
        assertThatThrownBy(() -> draftReviewRequestService.requestDocumentReview(
                        new RequestDocumentReviewCommand(999L, "리뷰", null, List.of(), ADMIN_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ReviewRequestErrorCode.REVIEW_REQUEST_DRAFT_NOT_FOUND);
    }

    @DisplayName("첫 회차 사전 초안의 개정안은 사전집 식별자 없이 기준 버전 0으로 만들어진다.")
    @Test
    void requestDictionaryReview_firstVersion() {
        Long draftDictionaryId = examinedDraftDictionary();

        ReviewRequestResult result = draftReviewRequestService.requestDictionaryReview(
                new RequestDictionaryReviewCommand(draftDictionaryId, "사전집 리뷰", null, List.of(), ADMIN_ID));

        assertThat(result.type()).isEqualTo(ReviewRequestType.DICTIONARY);
        assertThat(revisionDictionaryRepository.findByReviewRequestId(result.reviewRequestId()))
                .singleElement()
                .extracting(
                        revision -> revision.getDraftDictionaryId(),
                        revision -> revision.getDictionaryId(),
                        revision -> revision.getBaseVersionNo())
                .containsExactly(draftDictionaryId, null, 0);
    }

    @DisplayName("사전 리뷰 요청은 ADMIN 이상만 할 수 있다.")
    @Test
    void requestDictionaryReview_regularPermission() {
        Long draftDictionaryId = examinedDraftDictionary();

        assertThatThrownBy(() -> draftReviewRequestService.requestDictionaryReview(
                        new RequestDictionaryReviewCommand(draftDictionaryId, "리뷰", null, List.of(), REGULAR_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_ADMIN_REQUIRED);
    }

    @DisplayName("이전 버전과 달라진 등재 대상이 없으면 사전 리뷰를 요청할 수 없다.")
    @Test
    void requestDictionaryReview_noChangedItem() {
        DraftDictionary draft = draftDictionaryRepository.save(DraftDictionaryFixture.draftDictionary()
                .workspaceId(workspaceId)
                .createdBy(ADMIN_ID)
                .build());
        draftDictionaryService.completeExamine(new CompleteExamineCommand(draft.getId(), ADMIN_ID));

        assertThatThrownBy(() -> draftReviewRequestService.requestDictionaryReview(
                        new RequestDictionaryReviewCommand(draft.getId(), "리뷰", null, List.of(), ADMIN_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NO_CHANGED_ITEM);
    }

    @DisplayName("교정 완료 전 사전 초안으로는 리뷰를 요청할 수 없다.")
    @Test
    void requestDictionaryReview_draftIsExamining() {
        DraftDictionary draft = draftDictionaryRepository.save(DraftDictionaryFixture.draftDictionary()
                .workspaceId(workspaceId)
                .createdBy(ADMIN_ID)
                .status(DraftDictionaryStatus.EXAMINING)
                .build());

        assertThatThrownBy(() -> draftReviewRequestService.requestDictionaryReview(
                        new RequestDictionaryReviewCommand(draft.getId(), "리뷰", null, List.of(), ADMIN_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NOT_EXAMINED);
    }

    @DisplayName("개정안 이력은 참여자만 조회할 수 있다.")
    @Test
    void revisionHistory_notParticipant() {
        Long draftDocumentId = examinedDraftDocument("교정한 본문");
        ReviewRequestResult created = draftReviewRequestService.requestDocumentReview(
                new RequestDocumentReviewCommand(draftDocumentId, "리뷰", null, List.of(), ADMIN_ID));

        assertThat(revisionService.documents(created.reviewRequestId(), null, ADMIN_ID))
                .hasSize(1);
        assertThatThrownBy(() -> revisionService.documents(created.reviewRequestId(), null, STRANGER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    @DisplayName("지정 리뷰어가 중복으로 넘어와도 한 번만 등록한다.")
    @Test
    void requestDocumentReview_duplicateReviewerInRequest() {
        Long draftDocumentId = examinedDraftDocument("교정한 본문");

        ReviewRequestResult result = draftReviewRequestService.requestDocumentReview(new RequestDocumentReviewCommand(
                draftDocumentId, "리뷰", null, List.of(REGULAR_ID, REGULAR_ID), ADMIN_ID));

        assertThat(reviewerRepository.findByReviewRequestId(result.reviewRequestId()))
                .hasSize(1);
    }

    @DisplayName("참여자가 아닌 회원을 리뷰어로 지정할 수 없다.")
    @Test
    void requestDocumentReview_reviewerIsNotParticipant() {
        Long draftDocumentId = examinedDraftDocument("교정한 본문");

        assertThatThrownBy(() -> draftReviewRequestService.requestDocumentReview(
                        new RequestDocumentReviewCommand(draftDocumentId, "리뷰", null, List.of(STRANGER_ID), ADMIN_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    private Long examinedDraftDocument(String draftBody) {
        return draftDocument(DraftDocumentStatus.EXAMINED, draftBody);
    }

    private Long draftDocument(DraftDocumentStatus status, String draftBody) {
        Document document = documentRepository.save(DocumentFixture.document()
                .workspaceId(workspaceId)
                .createdBy(ADMIN_ID)
                .build());
        DraftDocument draft = draftDocumentRepository.save(DraftDocumentFixture.draftDocument()
                .documentId(document.getId())
                .draftBody(draftBody)
                .requestedBy(ADMIN_ID)
                .status(status)
                .build());
        return draft.getId();
    }

    private Long examinedDraftDictionary() {
        DraftDictionary draft = draftDictionaryRepository.save(DraftDictionaryFixture.draftDictionary()
                .workspaceId(workspaceId)
                .createdBy(ADMIN_ID)
                .build());
        Long candidateTermId = candidateTermService
                .add(new AddCandidateTermCommand(
                        draft.getId(), "신규어", "신규 정의", null, List.of(10L), 1, List.of("문맥"), ADMIN_ID))
                .candidateTermId();
        candidateTermService.decide(new DecideCandidateTermCommand(
                candidateTermId, ADMIN_ID, CandidateTermStatus.REGISTRATION_APPROVED, null, null));
        draftDictionaryService.completeExamine(new CompleteExamineCommand(draft.getId(), ADMIN_ID));
        return draft.getId();
    }

    /** 초안 전이는 AFTER_COMMIT 비동기 리스너가 하므로 폴링으로 기다린다. */
    private DraftDocumentStatus waitForDocumentStatus(Long draftDocumentId, DraftDocumentStatus expected)
            throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            DraftDocumentStatus status = draftDocumentRepository
                    .findByIdAndDeletedAtIsNull(draftDocumentId)
                    .orElseThrow()
                    .getStatus();
            if (status == expected) {
                return status;
            }
            Thread.sleep(50);
        }
        return draftDocumentRepository
                .findByIdAndDeletedAtIsNull(draftDocumentId)
                .orElseThrow()
                .getStatus();
    }
}
