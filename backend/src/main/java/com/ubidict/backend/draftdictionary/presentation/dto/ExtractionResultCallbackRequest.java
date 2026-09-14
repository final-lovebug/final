package com.ubidict.backend.draftdictionary.presentation.dto;

import com.ubidict.backend.draftdictionary.infra.port.ExtractedTerm;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 워커가 보내는 추출 성공 콜백.
 *
 * @param requestId 발행 때 실어 보낸 UUIDv4. 작업 행에 저장된 값과 대조해 호출자를 확인한다(D-66)
 * @param sourceDocumentIds 워커가 실제로 처리한 문서 집합. 작업이 지시한 집합과 다르면 결과를 받지 않는다 — 발신자가 외부 프로세스가 되면서
 *     비로소 실질적인 방어가 됐다
 */
public record ExtractionResultCallbackRequest(
        @NotBlank String requestId,
        @NotEmpty List<Long> sourceDocumentIds,
        @NotNull @Valid List<ExtractedTermRequest> terms) {

    public List<ExtractedTerm> toPorts() {
        return terms.stream().map(ExtractedTermRequest::toPort).toList();
    }
}
