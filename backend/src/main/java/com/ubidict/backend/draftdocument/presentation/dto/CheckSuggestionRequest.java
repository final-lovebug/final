package com.ubidict.backend.draftdocument.presentation.dto;

import com.ubidict.backend.draftdocument.infra.port.CheckSuggestion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 워커가 돌려준 제안어 하나.
 *
 * <p>{@code anchor}의 오프셋 단위는 <b>Java 문자열과 같은 UTF-16 code unit</b>이다({@code docs/AI_CONTRACT.md}).
 * {@code CheckSuggestionValidator}가 본문에서 그 구간을 잘라 {@code originTerm}과 같은지 대조하므로, 워커가 코드포인트 기준으로 세면
 * 이모지·일부 한자 뒤의 제안어가 전부 거절된다.
 */
public record CheckSuggestionRequest(
        @NotNull @Valid TextRangeRequest anchor,
        @NotBlank String originTerm,
        @NotBlank String suggestionTerm) {

    public CheckSuggestion toPort() {
        return new CheckSuggestion(anchor.to(), originTerm, suggestionTerm);
    }
}
