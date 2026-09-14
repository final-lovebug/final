package com.ubidict.backend.draftdocument.presentation.dto;

import com.ubidict.backend.draftdocument.infra.port.CheckSuggestion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 워커가 보내는 대조 성공 콜백.
 *
 * @param requestId 발행 때 실어 보낸 UUIDv4. 작업 행에 저장된 값과 대조해 호출자를 확인한다(D-70)
 * @param documentVersionNo 워커가 읽은 문서 버전. 그 사이 문서가 새 버전으로 바뀌었다면 앵커 오프셋이 이미 무효이므로 결과를 받지 않는다
 */
public record CheckResultCallbackRequest(
        @NotBlank String requestId,
        int documentVersionNo,
        @NotNull @Valid List<CheckSuggestionRequest> suggestions) {

    public List<CheckSuggestion> toPorts() {
        return suggestions.stream().map(CheckSuggestionRequest::toPort).toList();
    }
}
