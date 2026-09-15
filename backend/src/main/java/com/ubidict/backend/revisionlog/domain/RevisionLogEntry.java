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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 개정 이력 하나에 속한 실제 변경 항목. */
@Getter
@Entity
@Table(name = "revision_log_entry")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RevisionLogEntry extends BaseEntity {

    public static final int SUBJECT_MAX_LENGTH = 100;
    public static final int DETAIL_MAX_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long revisionLogId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private RevisionLogChangeType changeType;

    @Column(nullable = false, updatable = false, length = SUBJECT_MAX_LENGTH)
    private String subject;

    @Column(updatable = false, length = SUBJECT_MAX_LENGTH)
    private String subjectEnglishName;

    @Column(updatable = false, length = SUBJECT_MAX_LENGTH)
    private String replacement;

    @Column(updatable = false, length = DETAIL_MAX_LENGTH)
    private String detail;

    private RevisionLogEntry(
            Long revisionLogId,
            RevisionLogChangeType changeType,
            String subject,
            String subjectEnglishName,
            String replacement,
            String detail) {
        this.revisionLogId = revisionLogId;
        this.changeType = changeType;
        this.subject = subject;
        this.subjectEnglishName = subjectEnglishName;
        this.replacement = replacement;
        this.detail = detail;
    }

    public static RevisionLogEntry term(
            Long revisionLogId,
            RevisionLogChangeType changeType,
            String subject,
            String subjectEnglishName,
            String detail) {
        return new RevisionLogEntry(revisionLogId, changeType, subject, subjectEnglishName, null, detail);
    }

    public static RevisionLogEntry replacement(Long revisionLogId, String originTerm, String suggestionTerm) {
        return new RevisionLogEntry(
                revisionLogId, RevisionLogChangeType.CHANGED, originTerm, null, suggestionTerm, null);
    }
}
