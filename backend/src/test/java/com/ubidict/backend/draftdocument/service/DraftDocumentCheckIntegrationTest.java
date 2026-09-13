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
import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import com.ubidict.backend.draftdocument.infra.CheckJobRepository;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import com.ubidict.backend.draftdocument.service.model.CheckJobResult;
import com.ubidict.backend.draftdocument.service.model.CreateCheckJobCommand;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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

    @DisplayName("대조 요청 트랜잭션이 커밋되면 비동기로 초안을 만들고 작업을 완료한다.")
    @Test
    void request_completesAfterCommit() throws InterruptedException {
        Long documentId = saveCheckTarget();

        CheckJobResult requested = checkService.request(new CreateCheckJobCommand(documentId, MEMBER_ID));
        CheckJob completed = waitForTerminal(requested.checkJobId());

        assertThat(completed.getStatus()).isEqualTo(CheckJobStatus.SUCCEEDED);
        assertThat(completed.getDraftDocumentId()).isNotNull();
        assertThat(draftDocumentRepository.findByIdAndDeletedAtIsNull(completed.getDraftDocumentId()))
                .isPresent();
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

    private CheckJob waitForTerminal(Long checkJobId) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            CheckJob checkJob =
                    checkJobRepository.findByIdAndDeletedAtIsNull(checkJobId).orElseThrow();
            if (!checkJob.isInProgress()) {
                return checkJob;
            }
            Thread.sleep(50);
        }
        return checkJobRepository.findByIdAndDeletedAtIsNull(checkJobId).orElseThrow();
    }
}
