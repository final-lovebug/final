package com.ubidict.backend.draftdictionary.infra.ai;

import com.ubidict.backend.common.infra.ai.LlmJobRequest;
import com.ubidict.backend.common.infra.ai.LlmJobType;
import com.ubidict.backend.draftdictionary.infra.port.ExtractedTerm;
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
 * <p>로컬 화면·흐름 검증을 위해 계약 검증을 통과하는 고정 후보어를 돌려준다. 유래 문서 id만 요청에서 가져오므로,
 * 실제 워커 콜백과 같은 소스 문서 범위 검증도 함께 탄다. 진짜 추출 결과가 필요하면 워커를 띄우고
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
        callbackService.complete(
                request.jobId(),
                request.requestId(),
                request.sourceDocumentIds(),
                mockTerms(request.sourceDocumentIds()));
    }

    private static List<ExtractedTerm> mockTerms(List<Long> sourceDocumentIds) {
        Long sourceDocumentId = sourceDocumentIds.getFirst();
        return List.of(
                new ExtractedTerm(
                        "결제",
                        "재화나 용역의 대가를 지급하는 행위",
                        "Payment",
                        List.of(sourceDocumentId),
                        3,
                        List.of("회원은 결제할 수 있다."),
                        List.of("결제", "페이먼트")),
                new ExtractedTerm(
                        "주문",
                        "고객이 상품 또는 서비스를 구매하기 위해 요청한 건",
                        "Order",
                        List.of(sourceDocumentId),
                        2,
                        List.of("주문이 완료되면 결제 상태를 갱신한다."),
                        List.of("주문", "오더")));
    }
}
