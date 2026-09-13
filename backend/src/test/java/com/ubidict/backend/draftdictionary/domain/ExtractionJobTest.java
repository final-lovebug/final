package com.ubidict.backend.draftdictionary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExtractionJobTest {

    @DisplayName("용어 추출 작업을 생성하면 대기 상태로 시작한다.")
    @Test
    void create() {
        ExtractionJob job = ExtractionJob.create(1L, null, List.of(10L, 20L), 2L);

        assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.PENDING);
        assertThat(job.getSourceDocumentIds()).containsExactly(10L, 20L);
    }

    @DisplayName("용어 추출 작업은 실행 후 생성된 사전 초안과 연결된다.")
    @Test
    void succeed() {
        ExtractionJob job = ExtractionJob.create(1L, null, List.of(10L), 2L);
        job.start();

        job.succeed(30L);

        assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.SUCCEEDED);
        assertThat(job.getDraftDictionaryId()).isEqualTo(30L);
    }

    @DisplayName("완료된 용어 추출 작업은 다시 시작할 수 없다.")
    @Test
    void start_succeeded() {
        ExtractionJob job = ExtractionJob.create(1L, null, List.of(10L), 2L);
        job.start();
        job.succeed(30L);

        assertThatThrownBy(job::start)
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_INVALID_STATUS));
    }
}
