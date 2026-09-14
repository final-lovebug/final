package com.ubidict.backend.draftdocument.service;

import com.ubidict.backend.common.infra.ai.LlmProperties;
import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.implement.StaleCheckJobReader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 콜백이 끝내 오지 않은 작업을 회수한다(D-77).
 *
 * <p>이것이 없으면 고아 작업 하나가 <b>그 문서의 대조를 영구히 막는다</b> — 생성 정책이 진행 중인 작업의 존재만 보고 거절하기 때문이다.
 *
 * <p><b>락을 걸지 않는다.</b> 실패 전이는 멱등이고 이미 끝난 작업은 조용히 넘어가므로, 인스턴스 여럿이 동시에 쓸어도 결과가 같다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DraftDocumentCheckTimeoutService {

    private static final String FAILURE_REASON = "AI 작업이 제한 시간 안에 완료되지 않았습니다.";

    private final StaleCheckJobReader staleCheckJobReader;
    private final DraftDocumentCheckExecutionService executionService;
    private final LlmProperties llmProperties;

    @Transactional
    public int expireStale() {
        List<CheckJob> stale =
                staleCheckJobReader.readStale(llmProperties.timeout().job());
        stale.forEach(job -> executionService.expire(job.getId(), FAILURE_REASON));
        if (!stale.isEmpty()) {
            log.warn("[DraftDocumentCheckTimeoutService.expireStale] Expired stale check jobs. count={}", stale.size());
        }
        return stale.size();
    }
}
