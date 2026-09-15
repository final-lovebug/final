package com.ubidict.backend.draftdocument.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.common.infra.ai.LlmJobRequest;
import com.ubidict.backend.common.infra.ai.LlmJobType;
import com.ubidict.backend.common.infra.ai.LlmMode;
import com.ubidict.backend.dictionary.fixture.DictionaryFixture;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.fixture.DocumentFixture;
import com.ubidict.backend.document.fixture.DocumentVersionFixture;
import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.document.infra.DocumentVersionRepository;
import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import com.ubidict.backend.draftdocument.infra.CheckJobRepository;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import com.ubidict.backend.draftdocument.service.model.CheckJobResult;
import com.ubidict.backend.draftdocument.service.model.CreateCheckJobCommand;
import com.ubidict.backend.support.AsyncWaits;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.support.ai.FakeLlmWorker;
import com.ubidict.backend.support.ai.FakeLlmWorkerConfiguration;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * 접수 → 실제 SQS 발행 → 워커 → 완료 콜백까지의 왕복(D-74·D-75).
 *
 * <p>대조는 추출과 달리 <b>문서 버전</b>이 계약에 실린다 — 워커가 읽은 본문과 결과를 굳힐 때의 본문이 같아야 제안어 앵커가 유효하기 때문이다.
 */
@Import(FakeLlmWorkerConfiguration.class)
@TestPropertySource(
        properties = {
            "app.ai.dispatch.mode=sqs",
            "app.ai.mode=stub",
            // 회수 대상 판정을 즉시 만족시킨다. 스케줄러는 test 프로파일에서 꺼져 있으므로
            // expireStale()을 직접 부르는 테스트에만 영향을 준다.
            "app.ai.timeout.job=PT0S"
        })
@Disabled("SQS 왕복은 인메모리 워커 테스트로 대체한다.")
class DraftDocumentCheckWorkerRoundTripTest extends IntegrationTestSupport {

    private static final Long MEMBER_ID = 7L;

    @Autowired
    private DraftDocumentCheckService checkService;

    @Autowired
    private DraftDocumentCheckTimeoutService timeoutService;

    @Autowired
    private CheckJobRepository checkJobRepository;

    @Autowired
    private DraftDocumentRepository draftDocumentRepository;

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
    private FakeLlmWorker fakeLlmWorker;

    @BeforeEach
    void resetWorker() {
        fakeLlmWorker.reset();
    }

    @DisplayName("대조를 접수하면 읽어야 할 문서 버전과 함께 큐로 나가고, 워커의 콜백이 초안을 만든다.")
    @Test
    void roundTrip() {
        Long documentId = fixture();

        CheckJobResult requested = checkService.request(new CreateCheckJobCommand(documentId, MEMBER_ID));

        AsyncWaits.awaitRoundTrip().untilAsserted(() -> {
            CheckJob job = job(requested.checkJobId());
            assertThat(job.getStatus()).isEqualTo(CheckJobStatus.SUCCEEDED);
            assertThat(job.getDraftDocumentId()).isNotNull();
        });

        assertThat(draftDocumentRepository.findByIdAndDeletedAtIsNull(
                        job(requested.checkJobId()).getDraftDocumentId()))
                .isPresent();

        List<LlmJobRequest> received = fakeLlmWorker.received();
        assertThat(received).hasSize(1);
        LlmJobRequest sent = received.getFirst();
        assertThat(sent.contractVersion()).isEqualTo(LlmJobRequest.CONTRACT_VERSION);
        assertThat(sent.jobType()).isEqualTo(LlmJobType.DOCUMENT_CHECK);
        assertThat(sent.jobId()).isEqualTo(requested.checkJobId());
        assertThat(sent.documentId()).isEqualTo(documentId);
        assertThat(sent.documentVersionNo()).isEqualTo(1);
        assertThat(sent.mode()).isEqualTo(LlmMode.STUB);
        assertThat(job(requested.checkJobId()).matchesRequestId(sent.requestId()))
                .isTrue();
    }

    @DisplayName("같은 성공 콜백이 두 번 와도 문서 초안은 하나만 생긴다.")
    @Test
    void roundTrip_duplicateCallback() {
        fakeLlmWorker.scenario(FakeLlmWorker.Scenario.SUCCESS_TWICE);
        Long documentId = fixture();

        CheckJobResult requested = checkService.request(new CreateCheckJobCommand(documentId, MEMBER_ID));

        AsyncWaits.awaitRoundTrip()
                .untilAsserted(() ->
                        assertThat(job(requested.checkJobId()).getStatus()).isEqualTo(CheckJobStatus.SUCCEEDED));
        assertThat(draftDocumentRepository.findAll()).hasSize(1);
    }

    @DisplayName("워커가 실패를 알리면 사유가 남고 초안은 만들어지지 않는다.")
    @Test
    void roundTrip_failureCallback() {
        fakeLlmWorker.scenario(FakeLlmWorker.Scenario.FAILURE);
        Long documentId = fixture();

        CheckJobResult requested = checkService.request(new CreateCheckJobCommand(documentId, MEMBER_ID));

        AsyncWaits.awaitRoundTrip().untilAsserted(() -> {
            CheckJob job = job(requested.checkJobId());
            assertThat(job.getStatus()).isEqualTo(CheckJobStatus.FAILED);
            assertThat(job.getFailureReason()).isEqualTo("모델 응답이 스키마를 만족하지 않습니다.");
        });
        assertThat(draftDocumentRepository.findAll()).isEmpty();
    }

    @DisplayName("상관 식별자가 맞지 않는 콜백은 작업을 끝내지 못한다.")
    @Test
    void roundTrip_wrongRequestId() {
        fakeLlmWorker.scenario(FakeLlmWorker.Scenario.WRONG_REQUEST_ID);
        Long documentId = fixture();

        CheckJobResult requested = checkService.request(new CreateCheckJobCommand(documentId, MEMBER_ID));

        AsyncWaits.awaitRoundTrip()
                .untilAsserted(() -> assertThat(fakeLlmWorker.received()).hasSize(1));
        assertThat(job(requested.checkJobId()).getStatus()).isEqualTo(CheckJobStatus.RUNNING);
        assertThat(draftDocumentRepository.findAll()).isEmpty();
    }

    @DisplayName("콜백이 끝내 오지 않으면 스위퍼가 작업을 회수해 그 문서의 대조를 풀어 준다.")
    @Test
    void roundTrip_silentWorker() {
        fakeLlmWorker.scenario(FakeLlmWorker.Scenario.SILENT);
        Long documentId = fixture();

        CheckJobResult requested = checkService.request(new CreateCheckJobCommand(documentId, MEMBER_ID));
        AsyncWaits.awaitRoundTrip().untilAsserted(() -> {
            assertThat(fakeLlmWorker.received()).hasSize(1);
            assertThat(job(requested.checkJobId()).getStatus()).isEqualTo(CheckJobStatus.RUNNING);
        });

        assertThat(timeoutService.expireStale()).isEqualTo(1);

        CheckJob expired = job(requested.checkJobId());
        assertThat(expired.getStatus()).isEqualTo(CheckJobStatus.FAILED);
        assertThat(expired.getFailureReason()).isEqualTo("AI 작업이 제한 시간 안에 완료되지 않았습니다.");
        // 회수됐으므로 같은 문서에 다시 요청할 수 있다.
        assertThat(checkService
                        .request(new CreateCheckJobCommand(documentId, MEMBER_ID))
                        .checkJobId())
                .isNotNull();
    }

    private CheckJob job(Long checkJobId) {
        return checkJobRepository.findByIdAndDeletedAtIsNull(checkJobId).orElseThrow();
    }

    private Long fixture() {
        Workspace workspace = workspaceRepository.save(
                WorkspaceFixture.workspace().createdBy(MEMBER_ID).build());
        participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspace.getId())
                .memberId(MEMBER_ID)
                .build());
        Document document = documentRepository.save(DocumentFixture.document()
                .workspaceId(workspace.getId())
                .createdBy(MEMBER_ID)
                .build());
        documentVersionRepository.save(DocumentVersionFixture.documentVersion()
                .documentId(document.getId())
                .body("회원은 결제할 수 있다.")
                .createdBy(MEMBER_ID)
                .build());
        dictionaryRepository.save(DictionaryFixture.dictionary()
                .workspaceId(workspace.getId())
                .createdBy(MEMBER_ID)
                .build());
        return document.getId();
    }
}
