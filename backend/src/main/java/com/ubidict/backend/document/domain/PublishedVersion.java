package com.ubidict.backend.document.domain;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.exception.DocumentErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 문서 버전 번호와 확정일시. 식별자도 생명주기도 따로 두지 않는 값 객체이며 document_version 테이블의 컬럼 2개로 저장된다.
 *
 * <p>사전집의 {@code DictionaryVersion}과 같은 역할이다. 이름만 다른 이유는 그 자리의 엔티티가 이미 {@link DocumentVersion}이라
 * {@code DocumentVersion}을 값 객체 이름으로 쓸 수 없기 때문이다.
 *
 * <p>번호와 확정일시를 묶는 이유는 둘이 함께 파생되기 때문이다. 다음 버전은 번호를 하나 올리면서 확정 시각을 새로 찍는다 — 한쪽만 바뀌는 경우가 없다.
 */
@Embeddable
public record PublishedVersion(
        @Column(name = "version_no", nullable = false, updatable = false)
        int versionNo,

        @Column(name = "published_at", nullable = false, updatable = false)
        OffsetDateTime publishedAt) {

    private static final int FIRST_VERSION_NO = 1;

    public PublishedVersion {
        if (versionNo < FIRST_VERSION_NO || publishedAt == null) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_INVALID_VERSION);
        }
    }

    /**
     * 업로드본의 버전. 문서의 첫 확정본이므로 1이다.
     */
    public static PublishedVersion initial() {
        return new PublishedVersion(FIRST_VERSION_NO, now());
    }

    /**
     * 다음 버전. 반영(Revise)이 붙기 전까지는 부르는 곳이 없지만, 번호와 확정일시가 함께 움직인다는 규칙을 여기에 둔다.
     */
    public PublishedVersion next() {
        return new PublishedVersion(versionNo + 1, now());
    }

    /**
     * DB의 시각 정밀도가 마이크로초이므로 같은 값으로 잘라 저장 전후 값이 달라지지 않게 한다.
     */
    private static OffsetDateTime now() {
        return OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}
