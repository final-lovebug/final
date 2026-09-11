package com.ubidict.backend.document.domain;

import com.ubidict.backend.common.domain.BaseEntity;
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
 * <p>공통 감사·삭제 시각 규약을 따르기 위해 {@link BaseEntity}를 상속한다. 현재 버전 삭제 유스케이스는 제공하지 않는다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentVersion extends BaseEntity {

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
    private Long createdBy;

    private DocumentVersion(
            Long documentId, PublishedVersion version, String body, Integer dictionaryVersionNo, Long createdBy) {
        this.documentId = documentId;
        this.version = version;
        this.body = body;
        this.dictionaryVersionNo = dictionaryVersionNo;
        this.createdBy = createdBy;
    }

    /**
     * 업로드본을 v1으로 발행한다. 대조를 거치지 않았으므로 기준 사전집 버전이 없다.
     */
    public static DocumentVersion publishFirst(Long documentId, String body, Long memberId) {
        return new DocumentVersion(documentId, PublishedVersion.initial(), normalizeBody(body), null, memberId);
    }

    /**
     * 최신 사전집에 맞춰지지 않은 버전인지 판정한다.
     *
     * <p>기준은 「마지막으로 대조한 시점」이 아니라 「마지막으로 반영된 버전」이다. 대조만 실행하고 교정을 끝내지 않은 문서는 본문이 아직 사전집에 맞춰지지 않았으므로 여전히
     * outdated다.
     *
     * <p>목록 조회는 본문을 빼고 읽으므로 엔티티가 아니라 조회 결과 모델이 같은 판정을 해야 한다. 규칙이 두 벌이 되지 않도록 static으로 두고 양쪽이 이것을 부른다.
     *
     * @param publishedDictionaryVersionNo 그 버전이 통과한 사전집 버전. 대조 전이면 null
     * @param activeDictionaryVersionNo 활성 사전집의 버전 번호. 사전집이 없으면 null
     */
    public static boolean isOutdated(Integer publishedDictionaryVersionNo, Integer activeDictionaryVersionNo) {
        if (activeDictionaryVersionNo == null) {
            return false;
        }

        return !activeDictionaryVersionNo.equals(publishedDictionaryVersionNo);
    }

    public boolean isOutdated(Integer activeDictionaryVersionNo) {
        return isOutdated(dictionaryVersionNo, activeDictionaryVersionNo);
    }

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
