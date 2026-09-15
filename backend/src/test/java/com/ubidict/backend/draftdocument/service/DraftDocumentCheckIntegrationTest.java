package com.ubidict.backend.draftdocument.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.fixture.DictionaryFixture;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.fixture.DocumentFixture;
import com.ubidict.backend.document.fixture.DocumentVersionFixture;
import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.document.infra.DocumentVersionRepository;
import com.ubidict.backend.draftdictionary.fixture.DraftDictionaryFixture;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.infra.CheckJobRepository;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import com.ubidict.backend.draftdocument.service.model.CheckJobResult;
import com.ubidict.backend.draftdocument.service.model.CreateCheckJobCommand;
import com.ubidict.backend.support.AsyncWaits;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 워커가 없는 환경의 기본 경로. 인프로세스 대역이 빈 결과로 작업을 끝낸다(D-74).
 *
 * <p>실제 큐를 거치는 왕복은 {@code DraftDocumentCheckWorkerRoundTripTest}가 본다.
 */
class DraftDocumentCheckIntegrationTest extends IntegrationTestSupport {

    private static final Long MEMBER_ID = 7L;

    @Autowired
    private DraftDocumentCheckService checkService;

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
    private DraftDictionaryRepository draftDictionaryRepository;

    @DisplayName("대조 요청 트랜잭션이 커밋되면 비동기로 초안을 만들고 작업을 완료한다.")
    @Test
    void request_completesAfterCommit() {
        Long documentId = saveCheckTarget();

        CheckJobResult requested = checkService.request(new CreateCheckJobCommand(documentId, MEMBER_ID));

        AsyncWaits.awaitInProcess().untilAsserted(() -> {
            CheckJob completed = job(requested.checkJobId());
            assertThat(completed.getStatus()).isEqualTo(CheckJobStatus.SUCCEEDED);
            assertThat(completed.getDraftDocumentId()).isNotNull();
        });

        assertThat(draftDocumentRepository.findByIdAndDeletedAtIsNull(
                        job(requested.checkJobId()).getDraftDocumentId()))
                .isPresent();
    }

    @DisplayName("사전집 초안이 진행 중이어도 문서를 갱신할 수 있고, 대조에 쓴 사전집 버전이 초안에 남는다(D-93).")
    @Test
    void request_allowedWhileDraftDictionaryIsOngoing() {
        Long documentId = saveCheckTarget();
        Long workspaceId = documentRepository
                .findByIdAndDeletedAtIsNull(documentId)
                .orElseThrow()
                .getWorkspaceId();
        draftDictionaryRepository.save(DraftDictionaryFixture.draftDictionary()
                .workspaceId(workspaceId)
                .createdBy(MEMBER_ID)
                .build());

        CheckJobResult requested = checkService.request(new CreateCheckJobCommand(documentId, MEMBER_ID));

        AsyncWaits.awaitInProcess()
                .untilAsserted(() ->
                        assertThat(job(requested.checkJobId()).getStatus()).isEqualTo(CheckJobStatus.SUCCEEDED));
        DraftDocument draftDocument = draftDocumentRepository
                .findByIdAndDeletedAtIsNull(job(requested.checkJobId()).getDraftDocumentId())
                .orElseThrow();
        assertThat(draftDocument.getDictionaryVersionNo()).isEqualTo(1);
    }

    private Long saveCheckTarget() {
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
        Dictionary dictionary = DictionaryFixture.dictionary()
                .workspaceId(workspace.getId())
                .createdBy(MEMBER_ID)
                .build();
        dictionaryRepository.save(dictionary);
        return document.getId();
    }

    private CheckJob job(Long checkJobId) {
        return checkJobRepository.findByIdAndDeletedAtIsNull(checkJobId).orElseThrow();
    }
}
