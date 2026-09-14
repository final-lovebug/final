package com.ubidict.backend.draftdictionary.infra.event;

import com.ubidict.backend.common.infra.ai.LlmJobRequest;
import com.ubidict.backend.common.infra.ai.LlmJobRequestSender;
import com.ubidict.backend.common.infra.ai.LlmProperties;
import com.ubidict.backend.draftdictionary.domain.event.DraftDictionaryExtractionRequestedEvent;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryExtractionExecutionService;
import com.ubidict.backend.draftdictionary.service.model.ExtractionJobResult;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 접수된 추출 작업을 AI 워커에게 넘긴다(D-66).
 *
 * <p>전환 이전에는 이 자리에서 <b>작업을 직접 실행</b>했다 — 스텁 포트를 부르고 결과까지 굳혔다. 이제 실행은 외부 FastAPI 워커의 몫이고, 여기가 하는 일은
 * 상관 식별자를 만들어 작업에 새기고 요청을 발행하는 것까지다. 완료는 워커의 HTTP 콜백이 알려 준다.
 *
 * <p><b>커밋 이후에 발행하는 이유</b> — 워커는 메시지를 받자마자 DB를 읽고 콜백을 친다. 커밋 전에 보내면 아직 보이지 않는 작업 행에 콜백이 닿는다.
 *
 * <p>발행에 실패하면 작업을 실패로 끝낸다. 그대로 두면 워크스페이스 단위 중복 방지 정책 때문에 다음 추출 요청이 계속 막힌다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DraftDictionaryExtractionDispatchListener {

    private final DraftDictionaryExtractionExecutionService executionService;
    private final LlmJobRequestSender llmJobRequestSender;
    private final LlmProperties llmProperties;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequested(DraftDictionaryExtractionRequestedEvent event) {
        String requestId = LlmJobRequest.newRequestId();
        Optional<ExtractionJobResult> dispatching =
                executionService.markDispatching(event.extractionJobId(), requestId);
        if (dispatching.isEmpty()) {
            log.info(
                    "[DraftDictionaryExtractionDispatchListener.onRequested] Job is not pending. Skipped. extractionJobId={}",
                    event.extractionJobId());
            return;
        }

        ExtractionJobResult job = dispatching.get();
        try {
            llmJobRequestSender.send(LlmJobRequest.termExtraction(
                    requestId,
                    job.extractionJobId(),
                    job.workspaceId(),
                    job.dictionaryId(),
                    job.sourceDocumentIds(),
                    llmProperties.mode()));
        } catch (Exception exception) {
            log.error(
                    "[DraftDictionaryExtractionDispatchListener.onRequested] Failed to dispatch extraction job. extractionJobId={}",
                    event.extractionJobId(),
                    exception);
            executionService.expire(event.extractionJobId(), "AI 작업 요청을 발행하지 못했습니다.");
        }
    }
}
