package com.ubidict.backend.draftdictionary.presentation.dto;

import com.ubidict.backend.draftdictionary.infra.port.ExtractedTerm;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 워커가 돌려준 후보어 하나. 형태 검증은 여기서 걸러내고, 업무 규칙(작업 대상 밖 문서·표기 중복 등)은
 * {@code ExtractionResultValidator}가 본다.
 */
public record ExtractedTermRequest(
        @NotBlank String form,
        String proposedDefinition,
        String proposedEnglishName,
        @NotNull List<Long> occurredDocumentIds,
        @Min(1) int occurrenceCount,
        @NotNull List<String> contextSnippets) {

    public ExtractedTerm toPort() {
        return new ExtractedTerm(
                form,
                proposedDefinition,
                proposedEnglishName,
                List.copyOf(occurredDocumentIds),
                occurrenceCount,
                List.copyOf(contextSnippets));
    }
}
