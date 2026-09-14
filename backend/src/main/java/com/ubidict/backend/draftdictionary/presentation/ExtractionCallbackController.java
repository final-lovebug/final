package com.ubidict.backend.draftdictionary.presentation;

import com.ubidict.backend.draftdictionary.presentation.dto.ExtractionResultCallbackRequest;
import com.ubidict.backend.draftdictionary.presentation.dto.JobFailureCallbackRequest;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryExtractionCallbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 워커(FastAPI)가 추출 결과를 돌려주는 자리(D-66).
 *
 * <p><b>인증 주체가 없는 유일한 업무 API다.</b> {@code /api/internal/**}은 필터 체인을 그냥 통과하며(D-69), 호출자 확인은 본문의
 * {@code requestId}를 작업 행에 저장된 값과 대조해서 한다(D-70). 그래서 {@code @AuthenticationPrincipal}을 받지 않는다 — 행위의 주체는
 * 작업이 이미 들고 있는 {@code requestedBy}이므로 감사 추적에는 구멍이 없다.
 *
 * <p><b>성공과 실패를 한 엔드포인트로 합치지 않았다.</b> 합치면 {@code terms}와 {@code reason}이 서로 조건부 필수가 돼 Bean Validation으로
 * 표현할 수 없고, 서비스에도 상태 분기가 생긴다.
 *
 * <p>응답 코드가 워커의 재시도 여부를 정한다(D-73) — 2xx와 4xx는 메시지를 지우고, 5xx와 타임아웃만 재시도한다. 이미 끝난 작업에 도착한 중복·지각 콜백도
 * <b>204</b>다(D-72). 4xx로 답하면 워커가 영원히 재시도한다.
 */
@RestController
@RequestMapping("/api/internal/llm/extractions")
@RequiredArgsConstructor
public class ExtractionCallbackController {

    private final DraftDictionaryExtractionCallbackService callbackService;

    @PostMapping("/{extractionJobId}/result")
    public ResponseEntity<Void> complete(
            @PathVariable Long extractionJobId, @Valid @RequestBody ExtractionResultCallbackRequest request) {
        callbackService.complete(extractionJobId, request.requestId(), request.sourceDocumentIds(), request.toPorts());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{extractionJobId}/failure")
    public ResponseEntity<Void> fail(
            @PathVariable Long extractionJobId, @Valid @RequestBody JobFailureCallbackRequest request) {
        callbackService.fail(extractionJobId, request.requestId(), request.reason(), request.code());
        return ResponseEntity.noContent().build();
    }
}
