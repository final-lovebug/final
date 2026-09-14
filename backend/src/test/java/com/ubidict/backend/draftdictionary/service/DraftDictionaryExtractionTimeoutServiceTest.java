package com.ubidict.backend.draftdictionary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.common.infra.ai.LlmMode;
import com.ubidict.backend.common.infra.ai.LlmProperties;
import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.implement.StaleExtractionJobReader;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 콜백이 끝내 오지 않은 작업의 회수(D-73).
 *
 * <p>외부 워커로 나가면서 「콜백이 영영 오지 않는다」가 실제 가능한 경우가 됐다. 회수하지 않으면 고아 작업 하나가 워크스페이스 전체의 추출을 막는다.
 */
class DraftDictionaryExtractionTimeoutServiceTest {

    private final StaleExtractionJobReader staleReader = mock(StaleExtractionJobReader.class);
    private final DraftDictionaryExtractionExecutionService executionService =
            mock(DraftDictionaryExtractionExecutionService.class);
    private final LlmProperties llmProperties = new LlmProperties(
            LlmMode.STUB, new LlmProperties.Timeout(true, Duration.ofMinutes(15), Duration.ofMinutes(1)));
    private final DraftDictionaryExtractionTimeoutService service =
            new DraftDictionaryExtractionTimeoutService(staleReader, executionService, llmProperties);

    @DisplayName("제한 시간을 넘긴 작업을 모두 실패로 회수한다.")
    @Test
    void expireStale() {
        given(staleReader.readStale(Duration.ofMinutes(15))).willReturn(List.of(job(30L), job(31L)));

        assertThat(service.expireStale()).isEqualTo(2);

        verify(executionService).expire(30L, "AI 작업이 제한 시간 안에 완료되지 않았습니다.");
        verify(executionService).expire(31L, "AI 작업이 제한 시간 안에 완료되지 않았습니다.");
    }

    @DisplayName("회수할 작업이 없으면 아무것도 건드리지 않는다.")
    @Test
    void expireStale_none() {
        given(staleReader.readStale(any())).willReturn(List.of());

        assertThat(service.expireStale()).isZero();

        verify(executionService, never()).expire(anyLong(), any());
    }

    private ExtractionJob job(Long id) {
        ExtractionJob job = ExtractionJob.create(1L, null, List.of(10L), 2L);
        ReflectionTestUtils.setField(job, "id", id);
        return job;
    }
}
