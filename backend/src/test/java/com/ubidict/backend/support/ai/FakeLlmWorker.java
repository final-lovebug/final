package com.ubidict.backend.support.ai;

import com.ubidict.backend.common.infra.ai.LlmJobRequest;
import com.ubidict.backend.common.infra.ai.LlmJobType;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryExtractionCallbackService;
import com.ubidict.backend.draftdocument.service.DraftDocumentCheckCallbackService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * 테스트에서 FastAPI 워커의 자리를 대신한다(D-71).
 *
 * <p>실제 큐를 읽는다 — 이 전환의 위험이 정확히 거기 있기 때문이다. 직렬화, 큐 이름, 계약 필드, at-least-once 재수신은 인메모리 대역으로는 검증되지
 * 않는다.
 *
 * <p>콜백은 HTTP가 아니라 서비스 직접 호출이다. {@code docs/TEST.md}의 분업대로 <b>콜백의 HTTP 계약은 컨트롤러 테스트가, permitAll 범위는
 * {@code SecurityConfigTest}가</b> 따로 본다.
 */
@Slf4j
@RequiredArgsConstructor
public class FakeLlmWorker {

    /** 워커가 어떻게 응답할지. 테스트가 바꾼다. */
    public enum Scenario {
        /** 결과 없음으로 성공 콜백을 보낸다. */
        SUCCESS,
        /** 같은 성공 콜백을 두 번 보낸다 — at-least-once 재수신을 흉내 낸다. */
        SUCCESS_TWICE,
        /** 실패 콜백을 보낸다. */
        FAILURE,
        /** 엉뚱한 상관 식별자로 성공 콜백을 보낸다. */
        WRONG_REQUEST_ID,
        /** 아무 응답도 보내지 않는다 — 콜백이 영영 오지 않는 경우. */
        SILENT
    }

    private static final String WRONG_REQUEST_ID = "0d5c6f6e-ffff-4000-8000-ffffffffffff";

    private final ObjectMapper objectMapper;
    private final DraftDictionaryExtractionCallbackService extractionCallbackService;
    private final DraftDocumentCheckCallbackService checkCallbackService;

    private final BlockingQueue<LlmJobRequest> received = new LinkedBlockingQueue<>();
    private volatile Scenario scenario = Scenario.SUCCESS;

    public void scenario(Scenario scenario) {
        this.scenario = scenario;
    }

    public void reset() {
        received.clear();
        scenario = Scenario.SUCCESS;
    }

    /** 워커가 실제로 받은 요청들. 메시지 계약을 단언할 때 쓴다. */
    public List<LlmJobRequest> received() {
        return List.copyOf(received);
    }

    @SqsListener("${app.messaging.sqs.llm-request-queue}")
    public void on(String message) {
        LlmJobRequest request = objectMapper.readValue(message, LlmJobRequest.class);
        received.add(request);
        log.info(
                "[FakeLlmWorker.on] Received. jobType={}, jobId={}, scenario={}",
                request.jobType(),
                request.jobId(),
                scenario);

        if (scenario == Scenario.SILENT) {
            return;
        }
        String requestId = scenario == Scenario.WRONG_REQUEST_ID ? WRONG_REQUEST_ID : request.requestId();

        try {
            respond(request, requestId);
            if (scenario == Scenario.SUCCESS_TWICE) {
                respond(request, requestId);
            }
        } catch (RuntimeException exception) {
            // 워커는 4xx를 재시도하지 않는다(D-69). 여기서도 메시지를 되돌리지 않고 기록만 남긴다.
            log.info(
                    "[FakeLlmWorker.on] Callback rejected. jobId={}, reason={}", request.jobId(), exception.toString());
        }
    }

    private void respond(LlmJobRequest request, String requestId) {
        if (request.jobType() == LlmJobType.TERM_EXTRACTION) {
            if (scenario == Scenario.FAILURE) {
                extractionCallbackService.fail(
                        request.jobId(), requestId, "모델 응답이 스키마를 만족하지 않습니다.", "LLM_SCHEMA_VIOLATION");
                return;
            }
            extractionCallbackService.complete(request.jobId(), requestId, request.sourceDocumentIds(), List.of());
            return;
        }
        if (scenario == Scenario.FAILURE) {
            checkCallbackService.fail(request.jobId(), requestId, "모델 응답이 스키마를 만족하지 않습니다.", "LLM_SCHEMA_VIOLATION");
            return;
        }
        checkCallbackService.complete(request.jobId(), requestId, request.documentVersionNo(), List.of());
    }
}
