package com.ubidict.backend.document.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.exception.DocumentErrorCode;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PublishedVersionTest {

    @DisplayName("첫 버전은 번호가 1이고 확정일시가 채워진다.")
    @Test
    void initial() {
        // when
        PublishedVersion version = PublishedVersion.initial();

        // then
        assertThat(version.versionNo()).isEqualTo(1);
        assertThat(version.publishedAt()).isNotNull();
    }

    /**
     * 번호와 확정일시가 함께 움직인다는 것이 이 값 객체를 두는 이유다.
     */
    @DisplayName("다음 버전은 번호를 하나 올리고 확정일시를 새로 찍는다.")
    @Test
    void next_increasesVersionNoAndStampsNewTime() {
        // given
        PublishedVersion first = PublishedVersion.initial();

        // when
        PublishedVersion second = first.next();

        // then
        assertThat(second.versionNo()).isEqualTo(2);
        assertThat(second.publishedAt()).isAfterOrEqualTo(first.publishedAt());
    }

    @DisplayName("다음 버전을 만들어도 이전 버전은 그대로다.")
    @Test
    void next_doesNotMutatePrevious() {
        // given
        PublishedVersion first = PublishedVersion.initial();

        // when
        first.next();

        // then
        assertThat(first.versionNo()).isEqualTo(1);
    }

    @DisplayName("버전 번호가 1보다 작으면 예외가 발생한다.")
    @Test
    void create_versionNoIsBelowFirst() {
        // when & then
        assertThatThrownBy(() -> new PublishedVersion(0, OffsetDateTime.now()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DocumentErrorCode.DOCUMENT_INVALID_VERSION);
    }

    @DisplayName("확정일시가 없으면 예외가 발생한다.")
    @Test
    void create_publishedAtIsNull() {
        // when & then
        assertThatThrownBy(() -> new PublishedVersion(1, null))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DocumentErrorCode.DOCUMENT_INVALID_VERSION);
    }

    /**
     * DB의 시각 정밀도가 마이크로초다. 절삭하지 않으면 저장 전후 값이 달라진다.
     */
    @DisplayName("확정일시는 마이크로초로 잘려 있다.")
    @Test
    void initial_publishedAtIsTruncatedToMicros() {
        // when
        PublishedVersion version = PublishedVersion.initial();

        // then
        assertThat(version.publishedAt().getNano() % 1_000).isZero();
    }
}
