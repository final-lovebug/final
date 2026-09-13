package com.ubidict.backend.reviewrequest.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.dictionary.domain.NewTerm;
import com.ubidict.backend.dictionary.service.DictionaryService;
import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.document.infra.DocumentVersionRepository;
import com.ubidict.backend.document.service.DocumentService;
import com.ubidict.backend.document.service.model.CreateDocumentCommand;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestStatus;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.RevisionDocument;
import com.ubidict.backend.reviewrequest.infra.ReviewRequestRepository;
import com.ubidict.backend.reviewrequest.infra.RevisionDocumentRepository;
import com.ubidict.backend.reviewrequest.service.model.PerformReviseCommand;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RevisePublishIntegrationTest extends IntegrationTestSupport {

    private static final Long OWNER_ID = 1L;

    @Autowired
    private ReviseService reviseService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentVersionRepository documentVersionRepository;

    @Autowired
    private DraftDocumentRepository draftDocumentRepository;

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    @Autowired
    private RevisionDocumentRepository revisionDocumentRepository;

    @DisplayName("문서 개정안을 발행하면 원본을 수정하지 않고 새 문서 버전을 만든다.")
    @Test
    void perform_createsNewDocumentVersion() {
        // given
        Workspace workspace = workspaceRepository.save(Workspace.create("개발팀", OWNER_ID));
        participantRepository.save(Participant.owner(workspace.getId(), OWNER_ID));
        dictionaryService.publish(workspace.getId(), 0, List.of(new NewTerm("회원", "Member", "가입한 주체")), OWNER_ID);
        var document = documentService.create(
                new CreateDocumentCommand(workspace.getId(), "정책", "기존 본문", List.of(), OWNER_ID));
        DraftDocument draft = draftDocumentRepository.save(
                DraftDocument.create(document.documentId(), 1, "개정 본문", OWNER_ID, OWNER_ID));
        ReviewRequest request = reviewRequestRepository.save(
                ReviewRequest.create(workspace.getId(), ReviewRequestType.DOCUMENT, "리뷰", null, OWNER_ID, OWNER_ID));
        RevisionDocument revision = revisionDocumentRepository.save(
                RevisionDocument.create(request.getId(), document.documentId(), 1, draft.getId(), "개정 본문", OWNER_ID));

        // when
        var result = reviseService.perform(new PerformReviseCommand(request.getId(), OWNER_ID));

        // then
        assertThat(result.resultVersionNo()).isEqualTo(2);
        assertThat(documentRepository
                        .findById(document.documentId())
                        .orElseThrow()
                        .getCurrentVersionNo())
                .isEqualTo(2);
        assertThat(documentVersionRepository.findByDocumentIdAndVersionVersionNo(document.documentId(), 1))
                .get()
                .extracting(version -> version.getBody())
                .isEqualTo("기존 본문");
        assertThat(documentVersionRepository.findByDocumentIdAndVersionVersionNo(document.documentId(), 2))
                .get()
                .extracting(version -> version.getBody())
                .isEqualTo("개정 본문");
        assertThat(revisionDocumentRepository
                        .findById(revision.getId())
                        .orElseThrow()
                        .getResultVersionNo())
                .isEqualTo(2);
        assertThat(reviewRequestRepository
                        .findById(request.getId())
                        .orElseThrow()
                        .getStatus())
                .isEqualTo(ReviewRequestStatus.REVISED);
    }
}
