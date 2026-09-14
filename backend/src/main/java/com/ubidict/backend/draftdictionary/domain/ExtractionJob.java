package com.ubidict.backend.draftdictionary.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExtractionJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long workspaceId;

    @Column(updatable = false)
    private Long dictionaryId;

    @ElementCollection
    @CollectionTable(name = "extraction_job_source_document", joinColumns = @JoinColumn(name = "extraction_job_id"))
    @Column(name = "document_id", nullable = false)
    private List<Long> sourceDocumentIds = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Long requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExtractionJobStatus status;

    private Long draftDictionaryId;

    /**
     * 워커에게 발행할 때 만든 UUIDv4. 콜백이 같은 값을 돌려주는지로 호출자를 확인한다(D-70).
     *
     * <p>작업 하나에만 쓰이는 1회용 토큰이라, 새어 나가도 그 작업 외에는 영향이 없다.
     */
    @Column(length = 36)
    private String requestId;

    @Column(length = 1000)
    private String failureReason;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private ExtractionJob(Long workspaceId, Long dictionaryId, List<Long> sourceDocumentIds, Long requestedBy) {
        validate(workspaceId, sourceDocumentIds, requestedBy);
        this.workspaceId = workspaceId;
        this.dictionaryId = dictionaryId;
        this.sourceDocumentIds = new ArrayList<>(sourceDocumentIds);
        this.requestedBy = requestedBy;
        this.status = ExtractionJobStatus.PENDING;
        this.createdBy = requestedBy;
    }

    public static ExtractionJob create(
            Long workspaceId, Long dictionaryId, List<Long> sourceDocumentIds, Long requestedBy) {
        return new ExtractionJob(workspaceId, dictionaryId, sourceDocumentIds, requestedBy);
    }

    /**
     * 워커에게 작업을 넘겼다고 표시한다. 상관 식별자를 기록하면서 {@code PENDING -> RUNNING}으로 옮긴다.
     *
     * <p>같은 {@code requestId}로 다시 부르면 아무 일도 하지 않는다 — 발행이 재시도돼도 안전해야 한다.
     */
    public void markDispatching(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_INVALID_REQUEST);
        }
        if (status == ExtractionJobStatus.RUNNING && requestId.equals(this.requestId)) return;
        validateStatus(ExtractionJobStatus.PENDING);
        this.requestId = requestId;
        this.status = ExtractionJobStatus.RUNNING;
    }

    /**
     * 콜백이 들고 온 상관 식별자가 이 작업의 것인지 본다.
     *
     * <p>길이에 따라 조기 반환하지 않는 {@link MessageDigest#isEqual}을 쓴다 — UUID는 추측 공간이 넓어 실익이 크지 않지만, 비교 시간에서
     * 정보가 새지 않게 하는 비용이 사실상 0이다.
     */
    public boolean matchesRequestId(String candidate) {
        if (this.requestId == null || candidate == null) return false;
        return MessageDigest.isEqual(
                this.requestId.getBytes(StandardCharsets.UTF_8), candidate.getBytes(StandardCharsets.UTF_8));
    }

    public void succeed(Long draftDictionaryId) {
        if (status == ExtractionJobStatus.SUCCEEDED && this.draftDictionaryId.equals(draftDictionaryId)) return;
        validateStatus(ExtractionJobStatus.RUNNING);
        if (draftDictionaryId == null) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_INVALID_STATUS);
        }
        this.draftDictionaryId = draftDictionaryId;
        this.failureReason = null;
        this.status = ExtractionJobStatus.SUCCEEDED;
    }

    public void fail(String failureReason) {
        if (status == ExtractionJobStatus.FAILED) return;
        if (!isInProgress()) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_INVALID_STATUS);
        }
        String normalized =
                failureReason == null || failureReason.isBlank() ? "용어 추출 작업 처리에 실패했습니다." : failureReason.strip();
        this.failureReason = normalized.substring(0, Math.min(normalized.length(), 1000));
        this.status = ExtractionJobStatus.FAILED;
    }

    public boolean isInProgress() {
        return status == ExtractionJobStatus.PENDING || status == ExtractionJobStatus.RUNNING;
    }

    private void validateStatus(ExtractionJobStatus expected) {
        if (status != expected) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_INVALID_STATUS);
        }
    }

    private static void validate(Long workspaceId, List<Long> sourceDocumentIds, Long requestedBy) {
        if (workspaceId == null
                || requestedBy == null
                || sourceDocumentIds == null
                || sourceDocumentIds.isEmpty()
                || sourceDocumentIds.stream().anyMatch(id -> id == null)
                || new HashSet<>(sourceDocumentIds).size() != sourceDocumentIds.size()) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_INVALID_REQUEST);
        }
    }
}
