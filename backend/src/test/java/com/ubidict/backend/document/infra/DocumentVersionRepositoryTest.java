package com.ubidict.backend.document.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.domain.DocumentVersion;
import com.ubidict.backend.document.fixture.DocumentFixture;
import com.ubidict.backend.document.fixture.DocumentVersionFixture;
import com.ubidict.backend.support.RepositoryTestSupport;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class DocumentVersionRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private DocumentVersionRepository documentVersionRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Long workspaceId;

    @BeforeEach
    void createWorkspace() {
        workspaceId =
                workspaceRepository.save(WorkspaceFixture.workspace().build()).getId();
        em.flush();
    }

    /**
     * 본문 상한이 10,000자다. text 컬럼(65,535바이트)이 한글 3바이트 기준으로도 충분한지 실제 스키마에서 확인한다.
     */
    @DisplayName("10,000자 본문이 잘리지 않고 저장된다.")
    @Test
    void save_bodyAtMaxLength() {
        // given
        Long documentId = saveDocument();

        // when
        DocumentVersion saved = documentVersionRepository.save(
                DocumentVersion.publishFirst(documentId, DocumentFixture.MAX_LENGTH_BODY, 1L));
        em.flush();
        em.clear();

        // then
        DocumentVersion found =
                documentVersionRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getBody()).hasSize(DocumentVersion.BODY_MAX_LENGTH);
    }

    @DisplayName("같은 문서에 같은 버전 번호를 넣으면 유니크 제약에 걸린다.")
    @Test
    void save_versionNoIsDuplicated() {
        // given
        Long documentId = saveDocument();
        documentVersionRepository.save(DocumentVersion.publishFirst(documentId, "첫 버전", 1L));
        em.flush();

        // when & then
        assertThatThrownBy(() -> {
                    documentVersionRepository.save(DocumentVersion.publishFirst(documentId, "같은 번호", 1L));
                    em.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("다른 문서라면 같은 버전 번호가 허용된다.")
    @Test
    void save_versionNoIsReusedAcrossDocuments() {
        // given
        Long documentId = saveDocument();
        Long otherDocumentId = saveDocument();

        // when
        documentVersionRepository.save(DocumentVersion.publishFirst(documentId, "첫 버전", 1L));
        documentVersionRepository.save(DocumentVersion.publishFirst(otherDocumentId, "첫 버전", 1L));
        em.flush();
        em.clear();

        // then
        assertThat(documentVersionRepository.findSummariesByDocumentId(documentId))
                .hasSize(1);
        assertThat(documentVersionRepository.findSummariesByDocumentId(otherDocumentId))
                .hasSize(1);
    }

    @DisplayName("버전 이력은 버전 번호 내림차순으로 조회된다.")
    @Test
    void findSummariesByDocumentId() {
        // given
        Long documentId = saveDocument();
        saveVersions(documentId, 1, 2, 3);

        // when
        List<DocumentVersionSummary> summaries = documentVersionRepository.findSummariesByDocumentId(documentId);

        // then
        assertThat(summaries).extracting(DocumentVersionSummary::versionNo).containsExactly(3, 2, 1);
    }

    @DisplayName("최신 버전 요약 조회는 문서마다 현재 버전 하나씩만 준다.")
    @Test
    void findCurrentSummaries() {
        // given
        Long documentId = saveDocument(2);
        saveVersions(documentId, 1, 2);

        // when
        List<DocumentVersionSummary> summaries = documentVersionRepository.findCurrentSummaries(List.of(documentId));

        // then
        assertThat(summaries).extracting(DocumentVersionSummary::versionNo).containsExactly(2);
    }

    private Long saveDocument() {
        return saveDocument(1);
    }

    private Long saveDocument(Integer currentVersionNo) {
        Document document = documentRepository.save(DocumentFixture.document()
                .workspaceId(workspaceId)
                .currentVersionNo(currentVersionNo)
                .build());
        em.flush();

        return document.getId();
    }

    private void saveVersions(Long documentId, int... versionNos) {
        for (int versionNo : versionNos) {
            documentVersionRepository.save(DocumentVersionFixture.documentVersion()
                    .documentId(documentId)
                    .versionNo(versionNo)
                    .build());
        }
        em.flush();
        em.clear();
    }
}
