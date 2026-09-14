package com.ubidict.backend.draftdictionary.infra.ai;

import com.ubidict.backend.common.infra.ai.LlmJobRequest;
import com.ubidict.backend.common.infra.ai.LlmJobType;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryExtractionCallbackService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * FastAPI 워커가 없는 환경에서 그 자리를 대신한다(D-74).
 *
 * <p>후보어를 하나도 찾지 못한 것으로 처리한다 — 전환 이전의 스텁 어댑터와 같은 결과다. 여기서 그럴듯한 목 데이터를 지어내지 않는 이유는, 로컬에서 통과한 결과가
 * 실제 워커에서는 검증에 걸리는 상황을 만들지 않기 위해서다. 진짜 결과가 필요하면 워커를 띄우고
 * {@code app.ai.dispatch.mode=sqs}로 돌린다.
 *
 * <p>콜백 서비스를 직접 부른다 — HTTP를 타지 않으므로 {@code /api/internal/**}의 인증·직렬화는 여기서 검증되지 않는다. 그 경계는 컨트롤러 테스트와
 * 왕복 테스트가 본다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.dispatch.mode", havingValue = "in-process", matchIfMissing = true)
public class InProcessExtractionWorker {

    private final DraftDictionaryExtractionCallbackService callbackService;

    @EventListener
    public void on(LlmJobRequest request) {
        if (request.jobType() != LlmJobType.TERM_EXTRACTION) {
            return;
        }
        log.info(
                "[InProcessExtractionWorker.on] Handling extraction in process. extractionJobId={}, mode={}",
                request.jobId(),
                request.mode());
        callbackService.complete(request.jobId(), request.requestId(), request.sourceDocumentIds(), List.of());
    }
}
