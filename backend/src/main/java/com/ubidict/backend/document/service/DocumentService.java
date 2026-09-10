package com.ubidict.backend.document.service;

import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.domain.DocumentVersion;
import com.ubidict.backend.document.domain.Label;
import com.ubidict.backend.document.implement.DocumentAppender;
import com.ubidict.backend.document.implement.DocumentLabelReader;
import com.ubidict.backend.document.implement.DocumentLabelWriter;
import com.ubidict.backend.document.implement.DocumentReader;
import com.ubidict.backend.document.implement.DocumentRemover;
import com.ubidict.backend.document.implement.DocumentUpdater;
import com.ubidict.backend.document.implement.DocumentVersionAppender;
import com.ubidict.backend.document.implement.DocumentVersionReader;
import com.ubidict.backend.document.implement.LabelAppender;
import com.ubidict.backend.document.implement.LabelReader;
import com.ubidict.backend.document.infra.DocumentVersionSummary;
import com.ubidict.backend.document.service.model.CreateDocumentCommand;
import com.ubidict.backend.document.service.model.DocumentResult;
import com.ubidict.backend.document.service.model.DocumentSummaryResult;
import com.ubidict.backend.document.service.model.DocumentVersionResult;
import com.ubidict.backend.document.service.model.DocumentVersionSummaryResult;
import com.ubidict.backend.document.service.model.LabelResult;
import com.ubidict.backend.document.service.model.UpdateDocumentCommand;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문서 CRUD와 버전 이력 조회.
 *
 * <p>본문을 바꾸는 유스케이스가 없다. 본문은 확정 버전에만 있고, 바뀌는 경로는 대조 → 초안 → 리뷰 → 반영이다. 문서 편집도 그 경로를 타므로 draftdocument
 * 도메인이 담당한다.
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentReader documentReader;
    private final DocumentAppender documentAppender;
    private final DocumentUpdater documentUpdater;
    private final DocumentRemover documentRemover;
    private final DocumentVersionReader documentVersionReader;
    private final DocumentVersionAppender documentVersionAppender;
    private final DocumentLabelReader documentLabelReader;
    private final DocumentLabelWriter documentLabelWriter;
    private final LabelReader labelReader;
    private final LabelAppender labelAppender;
    private final WorkspaceAccessValidator workspaceAccessValidator;

    /**
     * 문서와 v1 버전을 한 트랜잭션에서 만든다. 본문이 버전에만 있으므로 v1이 빠지면 본문 없는 문서가 남는다.
     */
    @Transactional
    public DocumentResult create(CreateDocumentCommand command) {
        workspaceAccessValidator.validateParticipant(command.workspaceId(), command.memberId());

        Document document = documentAppender.append(command.workspaceId(), command.title(), command.memberId());
        DocumentVersion version = documentVersionAppender.appendFirst(document, command.content(), command.memberId());
        List<Label> labels = attachLabels(document, command.labels(), command.memberId());

        return DocumentResult.of(document, version, names(labels), activeDictionaryVersionNo(command.workspaceId()));
    }

    /**
     * 문서 수와 무관하게 질의는 네 번이다. 문서 목록, 문서별 최신 버전, 문서별 라벨 연결, 라벨 이름.
     */
    @Transactional(readOnly = true)
    public List<DocumentSummaryResult> readAll(Long workspaceId, Long memberId, String labelName) {
        workspaceAccessValidator.validateParticipant(workspaceId, memberId);

        List<Document> documents = labelName == null
                ? documentReader.readAll(workspaceId)
                : documentReader.readAllByLabel(workspaceId, Label.normalizeName(labelName));
        List<Long> documentIds = documents.stream().map(Document::getId).toList();

        Map<Long, DocumentVersionSummary> currentVersions = documentVersionReader.readCurrentSummaries(documentIds);
        Map<Long, List<Long>> labelIds = documentLabelReader.readLabelIds(documentIds);
        Map<Long, String> labelNames = labelReader.readNames(
                labelIds.values().stream().flatMap(List::stream).distinct().toList());
        Integer activeDictionaryVersionNo = activeDictionaryVersionNo(workspaceId);

        return documents.stream()
                .map(document -> DocumentSummaryResult.of(
                        document,
                        currentVersions.get(document.getId()),
                        namesOf(labelIds.getOrDefault(document.getId(), List.of()), labelNames),
                        activeDictionaryVersionNo))
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResult read(Long workspaceId, Long documentId, Long memberId) {
        workspaceAccessValidator.validateParticipant(workspaceId, memberId);

        Document document = documentReader.read(documentId, workspaceId);
        DocumentVersion version = documentVersionReader.readCurrent(document);

        return DocumentResult.of(document, version, readLabelNames(document), activeDictionaryVersionNo(workspaceId));
    }

    @Transactional
    public void update(UpdateDocumentCommand command) {
        workspaceAccessValidator.validateParticipant(command.workspaceId(), command.memberId());

        Document document = documentReader.read(command.documentId(), command.workspaceId());
        documentUpdater.rename(document, command.title(), command.memberId());
        attachLabels(document, command.labels(), command.memberId());
    }

    @Transactional
    public void delete(Long workspaceId, Long documentId, Long memberId) {
        workspaceAccessValidator.validateAtLeast(workspaceId, memberId, Permission.ADMIN);

        Document document = documentReader.read(documentId, workspaceId);
        documentRemover.remove(document);
    }

    /**
     * 문서 소속을 먼저 확인한다. 확인하지 않으면 다른 워크스페이스 문서의 버전이 새어 나간다.
     */
    @Transactional(readOnly = true)
    public List<DocumentVersionSummaryResult> readVersions(Long workspaceId, Long documentId, Long memberId) {
        workspaceAccessValidator.validateParticipant(workspaceId, memberId);

        Document document = documentReader.read(documentId, workspaceId);

        return documentVersionReader.readHistory(document).stream()
                .map(DocumentVersionSummaryResult::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentVersionResult readVersion(Long workspaceId, Long documentId, int versionNo, Long memberId) {
        workspaceAccessValidator.validateParticipant(workspaceId, memberId);

        Document document = documentReader.read(documentId, workspaceId);

        return DocumentVersionResult.from(documentVersionReader.read(document, versionNo));
    }

    @Transactional(readOnly = true)
    public List<LabelResult> readLabels(Long workspaceId, Long memberId) {
        workspaceAccessValidator.validateParticipant(workspaceId, memberId);

        return labelReader.readAll(workspaceId).stream().map(LabelResult::from).toList();
    }

    private List<Label> attachLabels(Document document, List<String> labelNames, Long memberId) {
        List<Label> labels = labelAppender.appendMissing(document.getWorkspaceId(), labelNames, memberId);
        documentLabelWriter.replace(document, labels, memberId);

        return labels;
    }

    private List<String> readLabelNames(Document document) {
        List<Long> labelIds = documentLabelReader.readLabelIds(document);

        return namesOf(labelIds, labelReader.readNames(labelIds));
    }

    private static List<String> namesOf(List<Long> labelIds, Map<Long, String> labelNames) {
        return labelIds.stream()
                .map(labelNames::get)
                .filter(java.util.Objects::nonNull)
                .sorted()
                .toList();
    }

    private static List<String> names(List<Label> labels) {
        return labels.stream().map(Label::getName).sorted().toList();
    }

    /**
     * 활성 사전집의 버전 번호. outdated 판정의 기준값이다.
     *
     * <p>TODO(REQ-DIC-001): dictionary 도메인이 붙으면 DictionaryService.readActive로 교체한다. 사전집이 없는 동안은 null이
     * 맞는 값이며, 이때 모든 문서의 outdated는 false다 — 갱신할 대상이 없기 때문이다.
     */
    private Integer activeDictionaryVersionNo(Long workspaceId) {
        return null;
    }
}
