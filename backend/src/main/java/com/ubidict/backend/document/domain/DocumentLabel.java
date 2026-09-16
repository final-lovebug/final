package com.ubidict.backend.document.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 문서와 라벨의 연결.
 *
 * <p>문서에서 라벨을 떼는 것은 이 행을 지우는 것이고, 라벨 자체는 워크스페이스에 남는다.
 */
@Getter
@Entity
@Table(
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_document_label",
                        columnNames = {"document_id", "label_id"}),
        // 라벨로 문서를 거꾸로 찾는 조회. 유니크 인덱스의 선두 컬럼이 document_id 라 그쪽으로는 타지 못한다.
        indexes = @Index(name = "idx_document_label_label", columnList = "label_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentLabel extends BaseEntity {

    /**
     * 문서당 라벨 개수 상한. 목록에서 한눈에 읽히는 수를 넘지 않게 한다.
     */
    public static final int MAX_LABELS_PER_DOCUMENT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long documentId;

    @Column(nullable = false, updatable = false)
    private Long labelId;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private DocumentLabel(Long documentId, Long labelId, Long createdBy) {
        this.documentId = documentId;
        this.labelId = labelId;
        this.createdBy = createdBy;
    }

    public static DocumentLabel of(Long documentId, Long labelId, Long memberId) {
        return new DocumentLabel(documentId, labelId, memberId);
    }
}
