package com.ubidict.backend.revisionlog.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 확정된 대상 버전과 직전 버전의 차이를 발행 시점에 고정한 기록.
 *
 * <p>사전집과 문서는 같은 타임라인 모양을 쓰므로 한 테이블에 저장한다. 축별 정적 팩터리로만 만들게 해 사전집 전용 값과 문서 전용 값이 섞이지 않게 한다(D-57).
 */
@Getter
@Entity
@Table(
        name = "revision_log",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "target_type", "target_id", "version_no"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RevisionLog extends BaseEntity {

    public static final int SUMMARY_MAX_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private RevisionLogTargetType targetType;

    @Column(nullable = false, updatable = false)
    private Long targetId;

    @Column(nullable = false, updatable = false)
    private int versionNo;

    @Column(updatable = false)
    private Integer previousVersionNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private RevisionOrigin origin;

    @Column(nullable = false, updatable = false, length = SUMMARY_MAX_LENGTH)
    private String summary;

    @Column(nullable = false, updatable = false)
    private int addedCount;

    @Column(nullable = false, updatable = false)
    private int changedCount;

    @Column(nullable = false, updatable = false)
    private int removedCount;

    @Column(nullable = false, updatable = false)
    private Long publishedBy;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false, length = 20)
    private RevisionLogGrade grade;

    @Column(nullable = false, updatable = false)
    private int affectedDocumentCount;

    @Column(updatable = false)
    private Integer baseDictionaryVersionNo;

    private RevisionLog(
            Long workspaceId,
            RevisionLogTargetType targetType,
            Long targetId,
            int versionNo,
            Integer previousVersionNo,
            RevisionOrigin origin,
            String summary,
            int addedCount,
            int changedCount,
            int removedCount,
            Long publishedBy,
            OffsetDateTime publishedAt,
            RevisionLogGrade grade,
            int affectedDocumentCount,
            Integer baseDictionaryVersionNo) {
        this.workspaceId = workspaceId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.versionNo = versionNo;
        this.previousVersionNo = previousVersionNo;
        this.origin = origin;
        this.summary = summary;
        this.addedCount = addedCount;
        this.changedCount = changedCount;
        this.removedCount = removedCount;
        this.publishedBy = publishedBy;
        this.publishedAt = publishedAt;
        this.grade = grade;
        this.affectedDocumentCount = affectedDocumentCount;
        this.baseDictionaryVersionNo = baseDictionaryVersionNo;
    }

    public static RevisionLog forDictionary(
            Long workspaceId,
            Long dictionaryId,
            int versionNo,
            Integer previousVersionNo,
            RevisionLogGrade grade,
            int addedCount,
            int changedCount,
            int removedCount,
            int affectedDocumentCount,
            String summary,
            Long publishedBy,
            OffsetDateTime publishedAt) {
        return new RevisionLog(
                workspaceId,
                RevisionLogTargetType.DICTIONARY,
                dictionaryId,
                versionNo,
                previousVersionNo,
                RevisionOrigin.REVIEW_REVISE,
                summary,
                addedCount,
                changedCount,
                removedCount,
                publishedBy,
                publishedAt,
                grade,
                affectedDocumentCount,
                null);
    }

    public static RevisionLog forDocument(
            Long workspaceId,
            Long documentId,
            int versionNo,
            Integer previousVersionNo,
            RevisionOrigin origin,
            Integer baseDictionaryVersionNo,
            int changedCount,
            String summary,
            Long publishedBy,
            OffsetDateTime publishedAt) {
        return new RevisionLog(
                workspaceId,
                RevisionLogTargetType.DOCUMENT,
                documentId,
                versionNo,
                previousVersionNo,
                origin,
                summary,
                0,
                changedCount,
                0,
                publishedBy,
                publishedAt,
                null,
                0,
                baseDictionaryVersionNo);
    }

    public boolean isDictionary() {
        return targetType == RevisionLogTargetType.DICTIONARY;
    }

    public boolean isFirstVersion() {
        return previousVersionNo == null;
    }
}
