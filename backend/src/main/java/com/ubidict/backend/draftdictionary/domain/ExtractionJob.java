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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "in_progress_flag"}))
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

    /**
     * 진행 중({@code PENDING}·{@code RUNNING})이면 1, 끝났으면 {@code null}. 「워크스페이스당 진행 중 작업은 1개」를 DB로 보장하기 위한
     * 컬럼이다({@code uk_extraction_job_workspace_in_progress}).
     *
     * <p><b>사전 검사만으로는 막지 못한다</b> — {@code ExtractionJobCreationPolicyValidator}는 락 없는 스냅샷 읽기라 동시 요청 둘을
     * 모두 통과시키고, 그러면 작업이 둘 생겨 AI 워커가 <b>LLM 을 두 번 호출</b>한다. 콜백 멱등(D-72)은 같은 작업의 중복 콜백만 막으므로
     * 이 경로에는 듣지 않는다.
     *
     * <p>{@code status}를 그대로 유니크에 넣으면 끝난 작업끼리 충돌한다. MySQL 은 UNIQUE 에서 NULL 을 서로 다른 값으로 보므로,
     * 끝난 작업은 NULL 이라 몇 개든 쌓이고 진행 중인 작업은 1 이라 두 개째가 막힌다.
     *
     * <p><b>{@code dictionary.active_flag}와 달리 생성 컬럼이 아니다.</b> 같은 식을 생성 컬럼으로 두면 H2 위에서 도는 테스트가
     * 삽입 시점에 깨진다. 대신 {@link #changeStatus}가 상태와 함께 갱신한다 — 상태를 바꾸는 경로는 그 메서드 하나뿐이다.
     */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "in_progress_flag")
    private Integer inProgressFlag;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private ExtractionJob(Long workspaceId, Long dictionaryId, List<Long> sourceDocumentIds, Long requestedBy) {
        validate(workspaceId, sourceDocumentIds, requestedBy);
        this.workspaceId = workspaceId;
        this.dictionaryId = dictionaryId;
        this.sourceDocumentIds = new ArrayList<>(sourceDocumentIds);
        this.requestedBy = requestedBy;
        changeStatus(ExtractionJobStatus.PENDING);
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
        changeStatus(ExtractionJobStatus.RUNNING);
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
        changeStatus(ExtractionJobStatus.SUCCEEDED);
    }

    public void fail(String failureReason) {
        if (status == ExtractionJobStatus.FAILED) return;
        if (!isInProgress()) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_INVALID_STATUS);
        }
        String normalized =
                failureReason == null || failureReason.isBlank() ? "용어 추출 작업 처리에 실패했습니다." : failureReason.strip();
        this.failureReason = normalized.substring(0, Math.min(normalized.length(), 1000));
        changeStatus(ExtractionJobStatus.FAILED);
    }

    public boolean isInProgress() {
        return status == ExtractionJobStatus.PENDING || status == ExtractionJobStatus.RUNNING;
    }

    /**
     * 상태를 바꾸는 유일한 경로. {@link #inProgressFlag}를 함께 갱신해 둘이 어긋나지 않게 한다.
     */
    private void changeStatus(ExtractionJobStatus next) {
        this.status = next;
        this.inProgressFlag = next == ExtractionJobStatus.PENDING || next == ExtractionJobStatus.RUNNING ? 1 : null;
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
