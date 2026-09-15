package com.ubidict.backend.draftdocument.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long documentId;

    @Column(nullable = false, updatable = false)
    private Long requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CheckJobStatus status;

    private Long draftDocumentId;

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

    private CheckJob(Long documentId, Long requestedBy) {
        if (documentId == null || requestedBy == null) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_REQUEST);
        }
        this.documentId = documentId;
        this.requestedBy = requestedBy;
        this.status = CheckJobStatus.PENDING;
        this.createdBy = requestedBy;
    }

    public static CheckJob create(Long documentId, Long requestedBy) {
        return new CheckJob(documentId, requestedBy);
    }

    /**
     * 워커에게 작업을 넘겼다고 표시한다. 상관 식별자를 기록하면서 {@code PENDING -> RUNNING}으로 옮긴다.
     *
     * <p>같은 {@code requestId}로 다시 부르면 아무 일도 하지 않는다 — 발행이 재시도돼도 안전해야 한다.
     */
    public void markDispatching(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_REQUEST);
        }
        if (status == CheckJobStatus.RUNNING && requestId.equals(this.requestId)) {
            return;
        }
        validateStatus(CheckJobStatus.PENDING);
        this.requestId = requestId;
        this.status = CheckJobStatus.RUNNING;
    }

    /**
     * 콜백이 들고 온 상관 식별자가 이 작업의 것인지 본다.
     *
     * <p>길이에 따라 조기 반환하지 않는 {@link MessageDigest#isEqual}을 쓴다 — UUID는 추측 공간이 넓어 실익이 크지 않지만, 비교 시간에서
     * 정보가 새지 않게 하는 비용이 사실상 0이다.
     */
    public boolean matchesRequestId(String candidate) {
        if (this.requestId == null || candidate == null) {
            return false;
        }
        return MessageDigest.isEqual(
                this.requestId.getBytes(StandardCharsets.UTF_8), candidate.getBytes(StandardCharsets.UTF_8));
    }

    public void succeed(Long draftDocumentId) {
        if (status == CheckJobStatus.SUCCEEDED && this.draftDocumentId.equals(draftDocumentId)) {
            return;
        }
        validateStatus(CheckJobStatus.RUNNING);
        if (draftDocumentId == null) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_STATUS);
        }
        this.draftDocumentId = draftDocumentId;
        this.failureReason = null;
        this.status = CheckJobStatus.SUCCEEDED;
    }

    public void fail(String failureReason) {
        if (status == CheckJobStatus.FAILED) {
            return;
        }
        if (status != CheckJobStatus.PENDING && status != CheckJobStatus.RUNNING) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_STATUS);
        }
        String normalized =
                failureReason == null || failureReason.isBlank() ? "대조 작업 처리에 실패했습니다." : failureReason.strip();
        this.failureReason = normalized.substring(0, Math.min(normalized.length(), 1000));
        this.status = CheckJobStatus.FAILED;
    }

    public boolean isInProgress() {
        return status == CheckJobStatus.PENDING || status == CheckJobStatus.RUNNING;
    }

    private void validateStatus(CheckJobStatus expected) {
        if (status != expected) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_STATUS);
        }
    }
}
