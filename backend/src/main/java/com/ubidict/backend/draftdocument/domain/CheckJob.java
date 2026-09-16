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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "in_progress_flag"}))
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

    /**
     * 진행 중({@code PENDING}·{@code RUNNING})이면 1, 끝났으면 {@code null}. 「문서당 진행 중 작업은 1개」를 DB로 보장하기 위한
     * 컬럼이다({@code uk_check_job_document_in_progress}).
     *
     * <p><b>사전 검사만으로는 막지 못한다</b> — {@code CheckJobCreationPolicyValidator}는 락 없는 스냅샷 읽기라 동시 요청 둘을
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

    private CheckJob(Long documentId, Long requestedBy) {
        if (documentId == null || requestedBy == null) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_REQUEST);
        }
        this.documentId = documentId;
        this.requestedBy = requestedBy;
        changeStatus(CheckJobStatus.PENDING);
        this.createdBy = requestedBy;
    }

    public static CheckJob create(Long documentId, Long requestedBy) {
        return new CheckJob(documentId, requestedBy);
    }

    /** 접수 트랜잭션에서 아웃박스 payload와 같은 상관 식별자를 고정한다. */
    public void assignRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_REQUEST);
        }
        if (this.requestId != null && !requestId.equals(this.requestId)) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_STATUS);
        }
        this.requestId = requestId;
    }

    /** 아웃박스가 실제 발행을 시도할 때만 {@code PENDING -> RUNNING}으로 옮긴다. */
    public boolean prepareDispatch(String requestId) {
        if (!matchesRequestId(requestId)) return false;
        if (status == CheckJobStatus.RUNNING) return true;
        if (status != CheckJobStatus.PENDING) return false;
        changeStatus(CheckJobStatus.RUNNING);
        return true;
    }

    /** @deprecated 테스트·구 경로 호환용. 새 접수 흐름은 assign 후 아웃박스가 prepare한다. */
    @Deprecated
    public void markDispatching(String requestId) {
        assignRequestId(requestId);
        if (!prepareDispatch(requestId)) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_STATUS);
        }
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
        changeStatus(CheckJobStatus.SUCCEEDED);
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
        changeStatus(CheckJobStatus.FAILED);
    }

    public boolean isInProgress() {
        return status == CheckJobStatus.PENDING || status == CheckJobStatus.RUNNING;
    }

    /**
     * 상태를 바꾸는 유일한 경로. {@link #inProgressFlag}를 함께 갱신해 둘이 어긋나지 않게 한다.
     */
    private void changeStatus(CheckJobStatus next) {
        this.status = next;
        this.inProgressFlag = next == CheckJobStatus.PENDING || next == CheckJobStatus.RUNNING ? 1 : null;
    }

    private void validateStatus(CheckJobStatus expected) {
        if (status != expected) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_STATUS);
        }
    }
}
