package com.ubidict.backend.draftdictionary.service;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 워커가 없는 환경의 기본 경로. 인프로세스 대역이 빈 결과로 작업을 끝낸다(D-74).
 *
 * <p>실제 큐를 거치는 왕복은 {@code DraftDictionaryExtractionWorkerRoundTripTest}가 본다.
 */
class DraftDictionaryExtractionIntegrationTest extends IntegrationTestSupport {

    private static final Long MEMBER_ID = 7L;

    @Autowired
    private DraftDictionaryExtractionService extractionService;

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

    @DisplayName("추출 요청 트랜잭션이 커밋되면 비동기로 사전 초안을 만들고 작업을 완료한다.")
    @Test
    void request_completesAfterCommit() {
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

        ExtractionJobResult requested = extractionService.request(
                new CreateExtractionJobCommand(workspace.getId(), null, List.of(document.getId()), MEMBER_ID));

        AsyncWaits.awaitInProcess().untilAsserted(() -> {
            ExtractionJob completed = job(requested.extractionJobId());
            assertThat(completed.getStatus()).isEqualTo(ExtractionJobStatus.SUCCEEDED);
            assertThat(completed.getDraftDictionaryId()).isNotNull();
        });

        assertThat(draftDictionaryRepository.findByIdAndDeletedAtIsNull(
                        job(requested.extractionJobId()).getDraftDictionaryId()))
                .isPresent();
    }

    private ExtractionJob job(Long extractionJobId) {
        return extractionJobRepository
                .findByIdAndDeletedAtIsNull(extractionJobId)
                .orElseThrow();
    }
}
