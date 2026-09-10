package com.ubidict.backend.dictionary.domain;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.dictionary.exception.DictionaryErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 사전집 버전 번호와 확정일시. 식별자도 생명주기도 따로 두지 않는 값 객체이며 dictionary 테이블의 컬럼 2개로 저장된다.
 *
 * <p>사전집 행 하나가 곧 확정된 버전 하나이므로 미확정 상태가 존재하지 않는다. 행이 만들어지는 순간이 확정 순간이라 publishedAt은 항상 채워진다.
 */
@Embeddable
public record DictionaryVersion(
        @Column(name = "version_no", nullable = false, updatable = false)
        int versionNo,

        @Column(name = "published_at", nullable = false, updatable = false)
        OffsetDateTime publishedAt) {

    private static final int FIRST_VERSION_NO = 1;

    public DictionaryVersion {
        if (versionNo < FIRST_VERSION_NO || publishedAt == null) {
            throw new BusinessException(DictionaryErrorCode.DICTIONARY_INVALID_VERSION);
        }
    }

    public static DictionaryVersion initial() {
        return new DictionaryVersion(FIRST_VERSION_NO, now());
    }

    public DictionaryVersion next() {
        return new DictionaryVersion(versionNo + 1, now());
    }

    /**
     * DB의 시각 정밀도가 마이크로초이므로 같은 값으로 잘라 저장 전후 값이 달라지지 않게 한다.
     */
    private static OffsetDateTime now() {
        return OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}
