package com.ubidict.backend.document.service;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.CommonErrorCode;
import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.domain.DocumentVersion;
import com.ubidict.backend.document.domain.Label;
import com.ubidict.backend.document.exception.DocumentErrorCode;
import com.ubidict.backend.document.implement.DocumentAlignmentReader;
import com.ubidict.backend.document.implement.DocumentAppender;
import com.ubidict.backend.document.implement.DocumentEditGuard;
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
import com.ubidict.backend.document.service.model.EditDocumentContentCommand;
import com.ubidict.backend.document.service.model.LabelResult;
import com.ubidict.backend.document.service.model.UpdateDocumentCommand;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문서 CRUD와 버전 이력 조회.
 *
 * <p>본문은 확정 버전에만 있고, 직접 편집은 새 버전을 즉시 발행한다.
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

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
    private final DocumentAlignmentReader documentAlignmentReader;
    private final DocumentEditGuard documentEditGuard;

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
    public PageResult<DocumentSummaryResult> readAll(
            Long workspaceId, Long memberId, String labelName, int page, int size, String sort) {
        validatePagination(page, size);
        workspaceAccessValidator.validateParticipant(workspaceId, memberId);

        String[] sortParts = parseDocumentSort(sort);
        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        "desc".equalsIgnoreCase(sortParts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC,
                        sortParts[0]));
        PageResult<Document> documentPage = documentReader.readPage(
                workspaceId, labelName == null ? null : Label.normalizeName(labelName), pageable);
        List<Document> documents = documentPage.content();
        List<Long> documentIds = documents.stream().map(Document::getId).toList();

        Map<Long, DocumentVersionSummary> currentVersions = documentVersionReader.readCurrentSummaries(documentIds);
        Map<Long, List<Long>> labelIds = documentLabelReader.readLabelIds(documentIds);
        Map<Long, String> labelNames = labelReader.readNames(
                labelIds.values().stream().flatMap(List::stream).distinct().toList());
        Integer activeDictionaryVersionNo = activeDictionaryVersionNo(workspaceId);

        return documentPage.map(document -> DocumentSummaryResult.of(
                document,
                currentVersions.get(document.getId()),
                namesOf(labelIds.getOrDefault(document.getId(), List.of()), labelNames),
                activeDictionaryVersionNo));
    }

    public List<DocumentSummaryResult> readAll(Long workspaceId, Long memberId, String labelName) {
        return readAll(workspaceId, memberId, labelName, 0, 100, "createdAt,desc")
                .content();
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
    public DocumentResult editContent(EditDocumentContentCommand command) {
        workspaceAccessValidator.validateParticipant(command.workspaceId(), command.memberId());
        Document document = documentReader.read(command.documentId(), command.workspaceId());
        documentEditGuard.validate(document.getId());
        DocumentVersion previous = documentVersionReader.readCurrent(document);
        document.publishNext(command.memberId());
        DocumentVersion version =
                documentVersionAppender.appendEdited(document, previous, command.content(), command.memberId());
        log.info(
                "[DocumentService.editContent] Document content edited documentId={}, workspaceId={}, versionNo={}",
                document.getId(),
                document.getWorkspaceId(),
                version.versionNo());
        return DocumentResult.of(
                document, version, readLabelNames(document), activeDictionaryVersionNo(command.workspaceId()));
    }

    /** 리뷰 승인이 확정한 교정 본문을 다음 문서 버전으로 발행한다. */
    @Transactional
    public int publishRevised(
            Long documentId, int baseVersionNo, String body, int dictionaryVersionNo, Long publishedBy) {
        Document document = documentReader.read(documentId);
        workspaceAccessValidator.validateAtLeast(document.getWorkspaceId(), publishedBy, Permission.ADMIN);
        if (document.getCurrentVersionNo() != baseVersionNo) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_VERSION_CONFLICT);
        }

        DocumentVersion previous = documentVersionReader.read(document, baseVersionNo);
        int resultVersionNo = document.publishNext(publishedBy);
        documentVersionAppender.appendRevised(document, previous, body, dictionaryVersionNo, publishedBy);
        return resultVersionNo;
    }

    @Transactional
    public void delete(Long workspaceId, Long documentId, Long memberId) {
        workspaceAccessValidator.validateAtLeast(workspaceId, memberId, Permission.ADMIN);

        Document document = documentReader.read(documentId, workspaceId);
        documentRemover.remove(document);
        log.info("[DocumentService.delete] Document deleted documentId={}, workspaceId={}", documentId, workspaceId);
    }

    /**
     * 문서 소속을 먼저 확인한다. 확인하지 않으면 다른 워크스페이스 문서의 버전이 새어 나간다.
     */
    @Transactional(readOnly = true)
    public PageResult<DocumentVersionSummaryResult> readVersions(
            Long workspaceId, Long documentId, Long memberId, int page, int size) {
        validatePagination(page, size);
        workspaceAccessValidator.validateParticipant(workspaceId, memberId);

        Document document = documentReader.read(documentId, workspaceId);

        return documentVersionReader
                .readHistoryPage(
                        document, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "version.versionNo")))
                .map(DocumentVersionSummaryResult::from);
    }

    public List<DocumentVersionSummaryResult> readVersions(Long workspaceId, Long documentId, Long memberId) {
        return readVersions(workspaceId, documentId, memberId, 0, 100).content();
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

    private Integer activeDictionaryVersionNo(Long workspaceId) {
        return documentAlignmentReader.activeVersionNo(workspaceId).orElse(null);
    }

    private static void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
    }

    private static String[] parseDocumentSort(String sort) {
        String[] parts = sort == null ? new String[0] : sort.split(",", -1);
        if (parts.length != 2
                || (!parts[0].equals("createdAt") && !parts[0].equals("title"))
                || (!parts[1].equalsIgnoreCase("asc") && !parts[1].equalsIgnoreCase("desc"))) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        return parts;
    }
}
