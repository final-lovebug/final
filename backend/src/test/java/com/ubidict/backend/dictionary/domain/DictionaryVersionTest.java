package com.ubidict.backend.dictionary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.dictionary.exception.DictionaryErrorCode;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DictionaryVersionTest {

    @DisplayName("첫 버전은 번호 1과 확정일시를 갖는다.")
    @Test
    void initial() {
        // when
        DictionaryVersion version = DictionaryVersion.initial();

        // then
        assertThat(version.versionNo()).isEqualTo(1);
        assertThat(version.publishedAt()).isNotNull();
    }

    @DisplayName("다음 버전은 번호가 1 오르고 확정일시를 다시 찍는다.")
    @Test
    void next() {
        // given
        DictionaryVersion version = DictionaryVersion.initial();

        // when
        DictionaryVersion next = version.next();

        // then
        assertThat(next.versionNo()).isEqualTo(2);
        assertThat(next.publishedAt()).isAfterOrEqualTo(version.publishedAt());
    }

    @DisplayName("버전 번호가 1보다 작으면 버전을 만들 수 없다.")
    @Test
    void create_versionNoIsBelowFirst() {
        // given
        OffsetDateTime publishedAt = OffsetDateTime.now();

        // when & then
        assertThatThrownBy(() -> new DictionaryVersion(0, publishedAt))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DictionaryErrorCode.DICTIONARY_INVALID_VERSION);
    }

    /**
     * 사전집 행 하나가 곧 확정된 버전 하나이므로 미확정 상태가 존재하지 않는다.
     */
    @DisplayName("확정일시가 없으면 버전을 만들 수 없다.")
    @Test
    void create_publishedAtIsNull() {
        // when & then
        assertThatThrownBy(() -> new DictionaryVersion(1, null))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DictionaryErrorCode.DICTIONARY_INVALID_VERSION);
    }
}
