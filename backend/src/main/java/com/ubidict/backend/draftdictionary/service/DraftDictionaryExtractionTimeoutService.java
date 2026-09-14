package com.ubidict.backend.draftdictionary.service;

import com.ubidict.backend.common.infra.ai.LlmProperties;
import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.implement.StaleExtractionJobReader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 콜백이 끝내 오지 않은 작업을 회수한다(D-73).
 *
 * <p>이것이 없으면 고아 작업 하나가 <b>워크스페이스 전체의 추출을 영구히 막는다</b> — 생성 정책이 진행 중인 작업의 존재만 보고 거절하기 때문이다. 외부 워커로
 * 나가면서 「콜백이 영영 오지 않는다」가 실제 가능한 경우가 됐으므로 회수 경로가 반드시 필요하다.
 *
 * <p><b>락을 걸지 않는다.</b> 실패 전이는 멱등이고 이미 끝난 작업은 조용히 넘어가므로, 인스턴스 여럿이 동시에 쓸어도 결과가 같다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DraftDictionaryExtractionTimeoutService {

    private static final String FAILURE_REASON = "AI 작업이 제한 시간 안에 완료되지 않았습니다.";

    private final StaleExtractionJobReader staleExtractionJobReader;
    private final DraftDictionaryExtractionExecutionService executionService;
    private final LlmProperties llmProperties;

    @Transactional
    public int expireStale() {
        List<ExtractionJob> stale =
                staleExtractionJobReader.readStale(llmProperties.timeout().job());
        stale.forEach(job -> executionService.expire(job.getId(), FAILURE_REASON));
        if (!stale.isEmpty()) {
            log.warn(
                    "[DraftDictionaryExtractionTimeoutService.expireStale] Expired stale extraction jobs. count={}",
                    stale.size());
        }
        return stale.size();
    }
}
