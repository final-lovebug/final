package com.ubidict.backend.draftdictionary.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.common.infra.ai.LlmJobRequest;
import com.ubidict.backend.common.infra.ai.LlmJobType;
import com.ubidict.backend.common.infra.ai.LlmMode;
import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.fixture.DocumentFixture;
import com.ubidict.backend.document.fixture.DocumentVersionFixture;
import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.document.infra.DocumentVersionRepository;
import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.domain.ExtractionJobStatus;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import com.ubidict.backend.draftdictionary.infra.ExtractionJobRepository;
import com.ubidict.backend.draftdictionary.service.model.CreateExtractionJobCommand;
import com.ubidict.backend.draftdictionary.service.model.ExtractionJobResult;
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
 * <p>LocalStack 위에서 <b>진짜 메시지</b>가 오간다. 인메모리 대역으로는 직렬화·큐 이름·계약 필드·중복 수신이 검증되지 않는데, 이 전환의 위험이 정확히
 * 거기에 있다.
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
class DraftDictionaryExtractionWorkerRoundTripTest extends IntegrationTestSupport {

    private static final Long MEMBER_ID = 7L;

    @Autowired
    private DraftDictionaryExtractionService extractionService;

    @Autowired
    private DraftDictionaryExtractionTimeoutService timeoutService;

    @Autowired
    private ExtractionJobRepository extractionJobRepository;

    @Autowired
    private DraftDictionaryRepository draftDictionaryRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentVersionRepository documentVersionRepository;

    @Autowired
    private FakeLlmWorker fakeLlmWorker;

    @BeforeEach
    void resetWorker() {
        fakeLlmWorker.reset();
    }

    @DisplayName("추출을 접수하면 전용 큐로 계약대로 나가고, 워커의 콜백이 사전 초안을 만든다.")
    @Test
    void roundTrip() {
        Long documentId = fixture();

        ExtractionJobResult requested = request(documentId);

        AsyncWaits.awaitRoundTrip().untilAsserted(() -> {
            ExtractionJob job = job(requested.extractionJobId());
            assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.SUCCEEDED);
            assertThat(job.getDraftDictionaryId()).isNotNull();
        });

        assertThat(draftDictionaryRepository.findByIdAndDeletedAtIsNull(
                        job(requested.extractionJobId()).getDraftDictionaryId()))
                .isPresent();

        List<LlmJobRequest> received = fakeLlmWorker.received();
        assertThat(received).hasSize(1);
        LlmJobRequest sent = received.getFirst();
        assertThat(sent.contractVersion()).isEqualTo(LlmJobRequest.CONTRACT_VERSION);
        assertThat(sent.jobType()).isEqualTo(LlmJobType.TERM_EXTRACTION);
        assertThat(sent.jobId()).isEqualTo(requested.extractionJobId());
        assertThat(sent.sourceDocumentIds()).containsExactly(documentId);
        assertThat(sent.mode()).isEqualTo(LlmMode.STUB);
        // 워커가 돌려준 상관 식별자가 작업 행의 것과 같아야 콜백이 받아들여진다(D-70).
        assertThat(job(requested.extractionJobId()).matchesRequestId(sent.requestId()))
                .isTrue();
    }

    @DisplayName("같은 성공 콜백이 두 번 와도 사전 초안은 하나만 생긴다.")
    @Test
    void roundTrip_duplicateCallback() {
        fakeLlmWorker.scenario(FakeLlmWorker.Scenario.SUCCESS_TWICE);
        Long documentId = fixture();

        ExtractionJobResult requested = request(documentId);

        AsyncWaits.awaitRoundTrip()
                .untilAsserted(() -> assertThat(job(requested.extractionJobId()).getStatus())
                        .isEqualTo(ExtractionJobStatus.SUCCEEDED));
        assertThat(draftDictionaryRepository.findAll()).hasSize(1);
    }

    @DisplayName("워커가 실패를 알리면 사유가 남고 초안은 만들어지지 않는다.")
    @Test
    void roundTrip_failureCallback() {
        fakeLlmWorker.scenario(FakeLlmWorker.Scenario.FAILURE);
        Long documentId = fixture();

        ExtractionJobResult requested = request(documentId);

        AsyncWaits.awaitRoundTrip().untilAsserted(() -> {
            ExtractionJob job = job(requested.extractionJobId());
            assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.FAILED);
            assertThat(job.getFailureReason()).isEqualTo("모델 응답이 스키마를 만족하지 않습니다.");
        });
        assertThat(draftDictionaryRepository.findAll()).isEmpty();
    }

    @DisplayName("상관 식별자가 맞지 않는 콜백은 작업을 끝내지 못한다.")
    @Test
    void roundTrip_wrongRequestId() {
        fakeLlmWorker.scenario(FakeLlmWorker.Scenario.WRONG_REQUEST_ID);
        Long documentId = fixture();

        ExtractionJobResult requested = request(documentId);

        AsyncWaits.awaitRoundTrip()
                .untilAsserted(() -> assertThat(fakeLlmWorker.received()).hasSize(1));
        assertThat(job(requested.extractionJobId()).getStatus()).isEqualTo(ExtractionJobStatus.RUNNING);
        assertThat(draftDictionaryRepository.findAll()).isEmpty();
    }

    @DisplayName("콜백이 끝내 오지 않으면 스위퍼가 작업을 회수해 다음 요청을 풀어 준다.")
    @Test
    void roundTrip_silentWorker() {
        fakeLlmWorker.scenario(FakeLlmWorker.Scenario.SILENT);
        Long documentId = fixture();

        ExtractionJobResult requested = request(documentId);
        AsyncWaits.awaitRoundTrip().untilAsserted(() -> {
            assertThat(fakeLlmWorker.received()).hasSize(1);
            assertThat(job(requested.extractionJobId()).getStatus()).isEqualTo(ExtractionJobStatus.RUNNING);
        });

        // 스케줄러는 test 프로파일에서 꺼져 있다. 회수 판단 자체를 직접 돌린다.
        assertThat(timeoutService.expireStale()).isEqualTo(1);

        ExtractionJob expired = job(requested.extractionJobId());
        assertThat(expired.getStatus()).isEqualTo(ExtractionJobStatus.FAILED);
        assertThat(expired.getFailureReason()).isEqualTo("AI 작업이 제한 시간 안에 완료되지 않았습니다.");
        // 회수됐으므로 같은 워크스페이스에 다시 요청할 수 있다.
        assertThat(request(documentId).extractionJobId()).isNotNull();
    }

    private ExtractionJobResult request(Long documentId) {
        Long workspaceId = documentRepository.findById(documentId).orElseThrow().getWorkspaceId();
        return extractionService.request(
                new CreateExtractionJobCommand(workspaceId, null, List.of(documentId), MEMBER_ID));
    }

    private ExtractionJob job(Long extractionJobId) {
        return extractionJobRepository
                .findByIdAndDeletedAtIsNull(extractionJobId)
                .orElseThrow();
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
                .dictionaryVersionNo(null)
                .edited(false)
                .createdBy(MEMBER_ID)
                .build());
        return document.getId();
    }
}
