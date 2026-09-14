package com.ubidict.backend.draftdocument.infra.ai;

import com.ubidict.backend.common.infra.ai.LlmJobRequest;
import com.ubidict.backend.common.infra.ai.LlmJobType;
import com.ubidict.backend.draftdocument.service.DraftDocumentCheckCallbackService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * FastAPI 워커가 없는 환경에서 그 자리를 대신한다(D-70).
 *
 * <p>제안어를 하나도 찾지 못한 것으로 처리한다 — 전환 이전의 스텁 어댑터와 같은 결과다. 앵커 오프셋을 지어내면 본문과 어긋나 검증에 걸리므로, 로컬에서 의미 있는
 * 제안어가 필요하면 워커를 띄우고 {@code app.ai.dispatch.mode=sqs}로 돌린다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.dispatch.mode", havingValue = "in-process", matchIfMissing = true)
public class InProcessCheckWorker {

    private final DraftDocumentCheckCallbackService callbackService;

    @EventListener
    public void on(LlmJobRequest request) {
        if (request.jobType() != LlmJobType.DOCUMENT_CHECK) {
            return;
        }
        log.info(
                "[InProcessCheckWorker.on] Handling check in process. checkJobId={}, mode={}",
                request.jobId(),
                request.mode());
        callbackService.complete(request.jobId(), request.requestId(), request.documentVersionNo(), List.of());
    }
}
