package com.ubidict.backend.draftdocument.presentation;

import com.ubidict.backend.draftdocument.presentation.dto.CheckResultCallbackRequest;
import com.ubidict.backend.draftdocument.presentation.dto.JobFailureCallbackRequest;
import com.ubidict.backend.draftdocument.service.DraftDocumentCheckCallbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 워커(FastAPI)가 대조 결과를 돌려주는 자리(D-66).
 *
 * <p><b>인증 주체가 없는 유일한 업무 API다.</b> {@code /api/internal/**}은 필터 체인을 그냥 통과하며(D-69), 호출자 확인은 본문의
 * {@code requestId}를 작업 행에 저장된 값과 대조해서 한다(D-70).
 *
 * <p>응답 코드가 워커의 재시도 여부를 정한다(D-73) — 2xx와 4xx는 메시지를 지우고, 5xx와 타임아웃만 재시도한다. 이미 끝난 작업에 도착한 중복·지각 콜백도
 * <b>204</b>다(D-72).
 */
@RestController
@RequestMapping("/api/internal/llm/checks")
@RequiredArgsConstructor
public class CheckCallbackController {

    private final DraftDocumentCheckCallbackService callbackService;

    @PostMapping("/{checkJobId}/result")
    public ResponseEntity<Void> complete(
            @PathVariable Long checkJobId, @Valid @RequestBody CheckResultCallbackRequest request) {
        callbackService.complete(checkJobId, request.requestId(), request.documentVersionNo(), request.toPorts());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{checkJobId}/failure")
    public ResponseEntity<Void> fail(
            @PathVariable Long checkJobId, @Valid @RequestBody JobFailureCallbackRequest request) {
        callbackService.fail(checkJobId, request.requestId(), request.reason(), request.code());
        return ResponseEntity.noContent().build();
    }
}
