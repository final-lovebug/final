package com.ubidict.backend.draftdocument.infra.event;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.infra.ai.LlmJobRequest;
import com.ubidict.backend.common.infra.ai.LlmJobRequestSender;
import com.ubidict.backend.common.infra.ai.LlmProperties;
import com.ubidict.backend.draftdocument.domain.event.DraftDocumentCheckRequestedEvent;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.port.DocumentQueryPort;
import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import com.ubidict.backend.draftdocument.service.DraftDocumentCheckExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 접수된 대조 작업을 AI 워커에게 넘긴다(D-66).
 *
 * <p>전환 이전에는 이 자리에서 <b>작업을 직접 실행</b>했다. 이제 실행은 외부 FastAPI 워커의 몫이고, 여기가 하는 일은 상관 식별자를 만들어 작업에 새기고 요청을
 * 발행하는 것까지다.
 *
 * <p>본문은 싣지 않는다(D-69). 대신 <b>워커가 읽어야 할 문서 버전</b>을 실어 보낸다 — 결과 앵커가 어느 본문 기준인지 대조하려면 그 번호가 있어야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DraftDocumentCheckDispatchListener {

    private final DraftDocumentCheckExecutionService executionService;
    private final DocumentQueryPort documentQueryPort;
    private final LlmJobRequestSender llmJobRequestSender;
    private final LlmProperties llmProperties;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequested(DraftDocumentCheckRequestedEvent event) {
        String requestId = LlmJobRequest.newRequestId();
        if (executionService.markDispatching(event.checkJobId(), requestId).isEmpty()) {
            log.info(
                    "[DraftDocumentCheckDispatchListener.onRequested] Job is not pending. Skipped. checkJobId={}",
                    event.checkJobId());
            return;
        }

        try {
            DocumentSnapshot document = documentQueryPort
                    .read(event.documentId())
                    .orElseThrow(() -> new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_DOCUMENT_NOT_FOUND));
            llmJobRequestSender.send(LlmJobRequest.documentCheck(
                    requestId,
                    event.checkJobId(),
                    document.workspaceId(),
                    document.documentId(),
                    document.currentVersionNo(),
                    llmProperties.mode()));
        } catch (Exception exception) {
            log.error(
                    "[DraftDocumentCheckDispatchListener.onRequested] Failed to dispatch check job. checkJobId={}",
                    event.checkJobId(),
                    exception);
            executionService.expire(event.checkJobId(), failureReason(exception));
        }
    }

    private String failureReason(Exception exception) {
        return exception instanceof BusinessException businessException
                ? businessException.errorCode().message()
                : "AI 작업 요청을 발행하지 못했습니다.";
    }
}
