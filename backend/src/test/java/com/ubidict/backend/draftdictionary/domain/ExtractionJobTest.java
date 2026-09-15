package com.ubidict.backend.draftdictionary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExtractionJobTest {

    private static final String REQUEST_ID = "0d5c6f6e-0000-4000-8000-000000000001";

    @DisplayName("용어 추출 작업을 생성하면 대기 상태로 시작한다.")
    @Test
    void create() {
        ExtractionJob job = ExtractionJob.create(1L, null, List.of(10L, 20L), 2L);

        assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.PENDING);
        assertThat(job.getSourceDocumentIds()).containsExactly(10L, 20L);
        assertThat(job.getRequestId()).isNull();
    }

    @DisplayName("워커에게 넘기면 상관 식별자를 새기고 실행 중 상태가 된다.")
    @Test
    void markDispatching() {
        ExtractionJob job = ExtractionJob.create(1L, null, List.of(10L), 2L);

        job.markDispatching(REQUEST_ID);

        assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.RUNNING);
        assertThat(job.getRequestId()).isEqualTo(REQUEST_ID);
    }

    @DisplayName("같은 상관 식별자로 다시 넘겨도 상태가 달라지지 않는다.")
    @Test
    void markDispatching_idempotent() {
        ExtractionJob job = ExtractionJob.create(1L, null, List.of(10L), 2L);
        job.markDispatching(REQUEST_ID);

        job.markDispatching(REQUEST_ID);

        assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.RUNNING);
        assertThat(job.getRequestId()).isEqualTo(REQUEST_ID);
    }

    @DisplayName("실행 중인 작업을 다른 상관 식별자로 넘길 수 없다.")
    @Test
    void markDispatching_otherRequestId() {
        ExtractionJob job = ExtractionJob.create(1L, null, List.of(10L), 2L);
        job.markDispatching(REQUEST_ID);

        assertThatThrownBy(() -> job.markDispatching("0d5c6f6e-0000-4000-8000-000000000002"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_INVALID_STATUS));
    }

    @DisplayName("콜백이 들고 온 상관 식별자가 다르면 이 작업의 것으로 보지 않는다.")
    @Test
    void matchesRequestId() {
        ExtractionJob job = ExtractionJob.create(1L, null, List.of(10L), 2L);
        assertThat(job.matchesRequestId(REQUEST_ID)).isFalse();

        job.markDispatching(REQUEST_ID);

        assertThat(job.matchesRequestId(REQUEST_ID)).isTrue();
        assertThat(job.matchesRequestId("0d5c6f6e-0000-4000-8000-000000000002")).isFalse();
        assertThat(job.matchesRequestId(null)).isFalse();
    }

    @DisplayName("용어 추출 작업은 실행 후 생성된 사전 초안과 연결된다.")
    @Test
    void succeed() {
        ExtractionJob job = ExtractionJob.create(1L, null, List.of(10L), 2L);
        job.markDispatching(REQUEST_ID);

        job.succeed(30L);

        assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.SUCCEEDED);
        assertThat(job.getDraftDictionaryId()).isEqualTo(30L);
    }

    @DisplayName("완료된 용어 추출 작업은 다시 워커에게 넘길 수 없다.")
    @Test
    void markDispatching_succeeded() {
        ExtractionJob job = ExtractionJob.create(1L, null, List.of(10L), 2L);
        job.markDispatching(REQUEST_ID);
        job.succeed(30L);

        assertThatThrownBy(() -> job.markDispatching("0d5c6f6e-0000-4000-8000-000000000002"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_INVALID_STATUS));
    }
}
