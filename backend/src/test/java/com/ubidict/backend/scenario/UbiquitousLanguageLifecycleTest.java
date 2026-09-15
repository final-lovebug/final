package com.ubidict.backend.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.service.DictionaryService;
import com.ubidict.backend.dictionary.service.model.DictionaryResult;
import com.ubidict.backend.dictionary.service.model.DictionarySearchQuery;
import com.ubidict.backend.dictionary.service.model.TermResult;
import com.ubidict.backend.document.service.DocumentService;
import com.ubidict.backend.document.service.model.CreateDocumentCommand;
import com.ubidict.backend.document.service.model.DocumentResult;
import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.domain.ExtractionJobStatus;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import com.ubidict.backend.draftdictionary.infra.ExtractionJobRepository;
import com.ubidict.backend.draftdictionary.service.CandidateTermService;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryExtractionService;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryService;
import com.ubidict.backend.draftdictionary.service.model.AddCandidateTermCommand;
import com.ubidict.backend.draftdictionary.service.model.CreateExtractionJobCommand;
import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import com.ubidict.backend.draftdocument.infra.CheckJobRepository;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import com.ubidict.backend.draftdocument.service.DraftDocumentCheckService;
import com.ubidict.backend.draftdocument.service.DraftDocumentService;
import com.ubidict.backend.draftdocument.service.model.CreateCheckJobCommand;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.reviewrequest.service.DraftReviewRequestService;
import com.ubidict.backend.reviewrequest.service.ReviewService;
import com.ubidict.backend.reviewrequest.service.ReviseService;
import com.ubidict.backend.reviewrequest.service.model.PerformReviseCommand;
import com.ubidict.backend.reviewrequest.service.model.RequestDictionaryReviewCommand;
import com.ubidict.backend.reviewrequest.service.model.RequestDocumentReviewCommand;
import com.ubidict.backend.reviewrequest.service.model.ReviewRequestResult;
import com.ubidict.backend.reviewrequest.service.model.SubmitReviewCommand;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.service.InvitationService;
import com.ubidict.backend.workspace.service.WorkspaceService;
import com.ubidict.backend.workspace.service.model.CreateWorkspaceCommand;
import com.ubidict.backend.workspace.service.model.IssueInvitationCommand;
import com.ubidict.backend.workspace.service.model.UpdateRuleSetCommand;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 여섯 도메인을 관통하는 시나리오. 어느 도메인의 것도 아니므로 scenario 패키지에 둔다(docs/TEST.md).
 *
 * <p><b>서비스 진입점만으로 흐름을 엮는다.</b> 리포지토리에 직접 seeding 하면 그 지점의 생성 정책 검증과 이벤트 발행을 건너뛰어, 도메인 사이의 배선이 끊겨 있어도
 * 초록이 된다 — 이 테스트가 막으려는 것이 바로 그 상황이다(Y-31).
 *
 * <p>추출·대조 워커는 스텁이라 빈 결과를 준다(app.ai.*=stub, D-35). 그래서 후보어는 교정 중에 손으로 등재하고, 문서 대조는 제안어 없이 초안만 만든다 — 검증 대상은
 * LLM 품질이 아니라 <b>작업 접수 → 비동기 실행 → 초안 → 리뷰 → 발행</b>의 배선이다.
 */
class UbiquitousLanguageLifecycleTest extends IntegrationTestSupport {

    private static final Long OWNER_ID = 1L;
    private static final Long REVIEWER_ID = 2L;
    private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DraftDictionaryExtractionService extractionService;

    @Autowired
    private DraftDictionaryService draftDictionaryService;

    @Autowired
    private CandidateTermService candidateTermService;

    @Autowired
    private DraftDocumentCheckService checkService;

    @Autowired
    private DraftDocumentService draftDocumentService;

    @Autowired
    private DraftReviewRequestService draftReviewRequestService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviseService reviseService;

    @Autowired
    private ExtractionJobRepository extractionJobRepository;

    @Autowired
    private CheckJobRepository checkJobRepository;

    @Autowired
    private DraftDictionaryRepository draftDictionaryRepository;

    @Autowired
    private DraftDocumentRepository draftDocumentRepository;

    private Long workspaceId;
    private Long documentId;

    @BeforeEach
    void openWorkspace() {
        workspaceId = workspaceService
                .create(new CreateWorkspaceCommand("개발팀", OWNER_ID))
                .workspaceId();
        joinByInvitation();
        workspaceService.changeRuleSet(new UpdateRuleSetCommand(workspaceId, 1, 1, OWNER_ID));
        documentId = documentService
                .create(new CreateDocumentCommand(workspaceId, "결제 도메인 설계", "회원은 결제수단을 선택한다.", List.of("설계"), OWNER_ID))
                .documentId();
    }

    @DisplayName("문서를 올려 용어를 추출하고 리뷰를 통과시키면 첫 사전집이 태어난다.")
    @Test
    void firstDictionaryIsBornFromDocuments() throws InterruptedException {
        // given — 사전집이 없는 워크스페이스의 문서는 기준 버전이 null이라 정렬된 상태다(G-12)
        assertThat(documentService.read(workspaceId, documentId, OWNER_ID))
                .extracting(DocumentResult::aligned, DocumentResult::dictionaryVersionNo)
                .containsExactly(true, null);

        // when — 추출 작업을 접수하면 커밋 후 비동기로 초안이 만들어진다
        Long draftDictionaryId = extractTerms(List.of(documentId));
        registerTerm(draftDictionaryId, "결제수단", "값을 지불하는 방법");
        completeDictionaryExamine(draftDictionaryId);
        ReviewRequestResult review =
                draftReviewRequestService.requestDictionaryReview(new RequestDictionaryReviewCommand(
                        draftDictionaryId, "첫 사전집 리뷰", null, List.of(REVIEWER_ID), OWNER_ID));
        approve(review.reviewRequestId());
        int versionNo = reviseService
                .perform(new PerformReviseCommand(review.reviewRequestId(), OWNER_ID))
                .resultVersionNo();

        // then
        assertThat(versionNo).isEqualTo(1);
        DictionaryResult active = dictionaryService.readActive(workspaceId, OWNER_ID, defaultQuery());
        assertThat(active.status()).isEqualTo(DictionaryStatus.ACTIVE);
        // 초안에 남아 있는 후보어 전부가 실린다 — 판정으로 걸러내지 않는다(D-88). 빼고 싶은 후보어는
        // 초안에서 삭제한다. 인프로세스 추출 대역은 테스트용 고정 후보어를 반환하므로
        // 첫 사전집에는 인프로세스 추출 대역이 반환한 고정 후보어도 함께 실린다.
        assertThat(active.terms().content())
                .extracting(TermResult::preferredForm)
                .containsExactly("결제", "결제수단", "주문");
        assertThat(waitForDictionaryDraftStatus(draftDictionaryId, DraftDictionaryStatus.REVISED))
                .isEqualTo(DraftDictionaryStatus.REVISED);
    }

    @DisplayName("사전집이 생기면 문서가 정렬되지 않은 상태가 되고, 갱신을 거치면 다시 정렬된다.")
    @Test
    void documentIsRealignedByUpdateFlow() throws InterruptedException {
        // given — 첫 사전집을 발행하면 기존 문서의 기준 버전(null)이 활성 버전(1)과 어긋난다
        publishFirstDictionary();
        assertThat(documentService.read(workspaceId, documentId, OWNER_ID).aligned())
                .isFalse();

        // when — 대조 작업을 접수해 초안을 만들고 리뷰를 거쳐 반영한다
        Long draftDocumentId = realignDocument();

        // then — 새 버전은 발행 시점 활성 사전집 버전을 기준으로 삼고 사람이 손댄 흔적이 없다(G-7·G-10)
        DocumentResult updated = documentService.read(workspaceId, documentId, OWNER_ID);
        assertThat(updated)
                .extracting(
                        DocumentResult::currentVersionNo,
                        DocumentResult::dictionaryVersionNo,
                        DocumentResult::edited,
                        DocumentResult::aligned)
                .containsExactly(2, 1, false, true);
        assertThat(documentService
                        .readVersion(workspaceId, documentId, 1, OWNER_ID)
                        .body())
                .isEqualTo("회원은 결제수단을 선택한다.");
        assertThat(waitForDocumentDraftStatus(draftDocumentId, DraftDocumentStatus.REVISED))
                .isEqualTo(DraftDocumentStatus.REVISED);
    }

    @DisplayName("워크스페이스에 진행 중인 초안 흐름은 문서든 사전이든 하나뿐이다.")
    @Test
    void draftFlowsAreMutuallyExclusiveAcrossDomains() throws InterruptedException {
        // given — 문서 초안이 진행 중이다
        publishFirstDictionary();
        checkDocument(documentId);

        // when & then — 같은 워크스페이스의 추출 접수가 막힌다(G-14)
        assertThatThrownBy(() -> extractionService.request(
                        new CreateExtractionJobCommand(workspaceId, null, List.of(documentId), OWNER_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_ALREADY_EXISTS);
    }

    @DisplayName("갱신을 거친 문서는 다시 추출 대상이 되고, 그 사전 초안이 진행 중이어도 문서 대조 접수는 열려 있다.")
    @Test
    void documentCheckIsAllowedWhileDictionaryDraftIsOngoing() throws InterruptedException {
        // given — 사전집 v1 이후에는 갱신을 거쳐 기준 버전이 맞춰진 문서만 추출 대상이다(G-12)
        publishFirstDictionary();
        realignDocument();
        extractTerms(List.of(documentId));

        // when — 사전집 초안이 진행 중이어도 갱신을 접수할 수 있다(D-93)
        Long draftDocumentId = checkDocument(documentId);

        // then — 그 초안은 대조에 쓴 사전집 버전(v1)을 들고 있고, 발행본이 그 버전을 따른다
        assertThat(draftDocumentRepository
                        .findByIdAndDeletedAtIsNull(draftDocumentId)
                        .orElseThrow()
                        .getDictionaryVersionNo())
                .isEqualTo(1);
    }

    /** 링크 복사 초대(inviteeEmail = null)는 특정 대상이 없으므로 수락 시점의 회원으로만 참여를 검사한다(D-39). */
    private void joinByInvitation() {
        String token = invitationService
                .issue(new IssueInvitationCommand(workspaceId, null, Permission.REGULAR, OWNER_ID))
                .token();
        invitationService.accept(token, REVIEWER_ID);
    }

    /** 정족수 1을 채운다. 지정 리뷰어 여부와 무관하게 참여자의 판정이 산입된다(G-4). */
    private void approve(Long reviewRequestId) {
        reviewService.submit(new SubmitReviewCommand(reviewRequestId, REVIEWER_ID, 0, ReviewVerdict.APPROVED));
    }

    /** 대조 → 교정 완료 → 리뷰 요청 → 승인 → 반영. 끝나면 문서가 활성 사전집 버전에 맞춰진다(G-7). */
    private Long realignDocument() throws InterruptedException {
        Long draftDocumentId = checkDocument(documentId);
        completeDocumentExamine(draftDocumentId);
        ReviewRequestResult review = draftReviewRequestService.requestDocumentReview(
                new RequestDocumentReviewCommand(draftDocumentId, "문서 갱신 리뷰", null, List.of(REVIEWER_ID), OWNER_ID));
        approve(review.reviewRequestId());
        reviseService.perform(new PerformReviseCommand(review.reviewRequestId(), OWNER_ID));
        waitForDocumentDraftStatus(draftDocumentId, DraftDocumentStatus.REVISED);

        return draftDocumentId;
    }

    private void completeDictionaryExamine(Long draftDictionaryId) {
        draftDictionaryService.completeExamine(
                new com.ubidict.backend.draftdictionary.service.model.CompleteExamineCommand(
                        draftDictionaryId, OWNER_ID));
    }

    private void completeDocumentExamine(Long draftDocumentId) {
        draftDocumentService.completeExamine(
                new com.ubidict.backend.draftdocument.service.model.CompleteExamineCommand(draftDocumentId, OWNER_ID));
    }

    private void publishFirstDictionary() throws InterruptedException {
        Long draftDictionaryId = extractTerms(List.of(documentId));
        registerTerm(draftDictionaryId, "결제수단", "값을 지불하는 방법");
        completeDictionaryExamine(draftDictionaryId);
        ReviewRequestResult review = draftReviewRequestService.requestDictionaryReview(
                new RequestDictionaryReviewCommand(draftDictionaryId, "첫 사전집 리뷰", null, List.of(), OWNER_ID));
        approve(review.reviewRequestId());
        reviseService.perform(new PerformReviseCommand(review.reviewRequestId(), OWNER_ID));
        waitForDictionaryDraftStatus(draftDictionaryId, DraftDictionaryStatus.REVISED);
    }

    private Long extractTerms(List<Long> sourceDocumentIds) throws InterruptedException {
        Long extractionJobId = extractionService
                .request(new CreateExtractionJobCommand(workspaceId, null, sourceDocumentIds, OWNER_ID))
                .extractionJobId();
        ExtractionJob job = waitFor(
                () -> extractionJobRepository
                        .findByIdAndDeletedAtIsNull(extractionJobId)
                        .orElseThrow(),
                candidate -> !candidate.isInProgress());
        assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.SUCCEEDED);

        return job.getDraftDictionaryId();
    }

    private Long checkDocument(Long targetDocumentId) throws InterruptedException {
        Long checkJobId = checkService
                .request(new CreateCheckJobCommand(targetDocumentId, OWNER_ID))
                .checkJobId();
        CheckJob job = waitFor(
                () -> checkJobRepository.findByIdAndDeletedAtIsNull(checkJobId).orElseThrow(),
                candidate -> !candidate.isInProgress());
        assertThat(job.getStatus()).isEqualTo(CheckJobStatus.SUCCEEDED);

        return job.getDraftDocumentId();
    }

    /** 추출 워커가 빈 결과를 주므로 등재할 용어는 교정 중에 손으로 넣는다(REQ-DIC-004의 구현 자리). */
    /** 판정은 발행 목록에 영향을 주지 않으므로(D-88) 등록만 한다. */
    private void registerTerm(Long draftDictionaryId, String form, String definition) {
        candidateTermService.add(new AddCandidateTermCommand(
                draftDictionaryId, form, definition, null, List.of(documentId), 1, List.of("문맥"), OWNER_ID));
    }

    private DraftDictionaryStatus waitForDictionaryDraftStatus(Long draftDictionaryId, DraftDictionaryStatus expected)
            throws InterruptedException {
        return waitFor(
                        () -> draftDictionaryRepository
                                .findByIdAndDeletedAtIsNull(draftDictionaryId)
                                .orElseThrow(),
                        draft -> draft.getStatus() == expected)
                .getStatus();
    }

    private DraftDocumentStatus waitForDocumentDraftStatus(Long draftDocumentId, DraftDocumentStatus expected)
            throws InterruptedException {
        return waitFor(
                        () -> draftDocumentRepository
                                .findByIdAndDeletedAtIsNull(draftDocumentId)
                                .orElseThrow(),
                        draft -> draft.getStatus() == expected)
                .getStatus();
    }

    /**
     * AFTER_COMMIT 비동기 리스너가 끝날 때까지 기다린다. 상한을 넘기면 마지막 상태를 그대로 돌려주고, 그 값이 기대와 다르면 호출한 쪽의 단정이 실패한다.
     */
    private <T> T waitFor(java.util.function.Supplier<T> read, java.util.function.Predicate<T> done)
            throws InterruptedException {
        Instant deadline = Instant.now().plus(ASYNC_TIMEOUT);
        T value = read.get();
        while (!done.test(value) && Instant.now().isBefore(deadline)) {
            Thread.sleep(50);
            value = read.get();
        }

        return value;
    }

    private DictionarySearchQuery defaultQuery() {
        return new DictionarySearchQuery(0, 20, "preferredForm,asc", null);
    }
}
