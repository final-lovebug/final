package com.ubidict.backend.document.domain;

import com.ubidict.backend.common.domain.AuditableEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.exception.DocumentErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 문서가 확정된 시점의 불변 스냅샷이자 <b>본문의 유일한 저장 위치</b>.
 *
 * <p>행이 만들어지는 순간이 곧 확정 순간이라 publishedAt은 항상 채워진다. 확정 뒤에는 아무도 고치지 않으므로 전 필드를 updatable = false로 못 박고, 별도 스냅샷
 * 테이블도 두지 않는다.
 *
 * <p>버전 번호와 확정일시는 {@link PublishedVersion} 값 객체로 묶는다. 사전집의 Dictionary가 DictionaryVersion을 품는 것과 같은 구조다 —
 * 둘은 함께 파생되고 같은 불변식을 공유한다. 호출자가 체이닝하지 않도록 versionNo()·publishedAt() 위임 메서드를 둔다.
 *
 * <p>BaseEntity를 상속하지 않는다. 삭제 경로가 없어 deletedAt이 필요 없다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentVersion extends AuditableEntity {

    public static final int BODY_MAX_LENGTH = 10_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long documentId;

    @Embedded
    private PublishedVersion version;

    @Lob
    @Column(nullable = false, updatable = false)
    private String body;

    /**
     * 이 버전이 통과한 사전집 버전. 대조를 거치지 않은 업로드본(v1)은 null이다.
     */
    @Column(updatable = false)
    private Integer dictionaryVersionNo;

    @Column(nullable = false, updatable = false)
    private boolean edited;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private DocumentVersion(
            Long documentId,
            PublishedVersion version,
            String body,
            Integer dictionaryVersionNo,
            boolean edited,
            Long createdBy) {
        this.documentId = documentId;
        this.version = version;
        this.body = body;
        this.dictionaryVersionNo = dictionaryVersionNo;
        this.edited = edited;
        this.createdBy = createdBy;
    }

    /**
     * 업로드본을 v1으로 발행한다. 대조를 거치지 않았으므로 기준 사전집 버전이 없다.
     */
    public static DocumentVersion publishFirst(Long documentId, String body, Long memberId) {
        return new DocumentVersion(documentId, PublishedVersion.initial(), normalizeBody(body), null, false, memberId);
    }

    public static DocumentVersion publishEdited(
            Long documentId, PublishedVersion version, String body, Integer dictionaryVersionNo, Long memberId) {
        return new DocumentVersion(documentId, version, normalizeBody(body), dictionaryVersionNo, true, memberId);
    }

    /**
     * 활성 사전집이 없거나, 직접 편집되지 않았고 기준 사전집 버전이 활성 버전과 같으면 정렬된 상태다.
     *
     * @param publishedDictionaryVersionNo 그 버전이 통과한 사전집 버전. 대조 전이면 null
     * @param edited 직접 편집본 여부
     * @param activeDictionaryVersionNo 활성 사전집 버전. 사전집이 없으면 null
     */
    public static boolean isAligned(
            Integer publishedDictionaryVersionNo, boolean edited, Integer activeDictionaryVersionNo) {
        return activeDictionaryVersionNo == null
                || (!edited && activeDictionaryVersionNo.equals(publishedDictionaryVersionNo));
    }

    public boolean isAligned(Integer activeDictionaryVersionNo) {
        return isAligned(dictionaryVersionNo, edited, activeDictionaryVersionNo);
    }

    /**
     * 값 객체가 가진 버전 번호를 노출한다.
     */
    public int versionNo() {
        return version.versionNo();
    }

    public OffsetDateTime publishedAt() {
        return version.publishedAt();
    }

    private static String normalizeBody(String body) {
        if (body == null) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_INVALID_CONTENT);
        }

        String normalized = body.strip();
        if (normalized.isEmpty() || normalized.length() > BODY_MAX_LENGTH) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_INVALID_CONTENT);
        }

        return normalized;
    }
}
