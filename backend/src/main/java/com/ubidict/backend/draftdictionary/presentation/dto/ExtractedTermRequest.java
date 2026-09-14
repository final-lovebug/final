package com.ubidict.backend.draftdictionary.presentation.dto;

import com.ubidict.backend.draftdictionary.infra.port.ExtractedTerm;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 워커가 돌려준 후보어 하나. 형태 검증은 여기서 걸러내고, 업무 규칙(작업 대상 밖 문서·표기 중복 등)은
 * {@code ExtractionResultValidator}가 본다.
 *
 * <p>{@code variantForms}는 추출기가 한 개념으로 묶은 여러 표기다(`D-65`). 빠져 있으면 빈 목록으로 다룬다 — 워커가 그룹핑을 하지 않아도
 * 결과를 받을 수 있어야 한다.
 */
public record ExtractedTermRequest(
        @NotBlank String form,
        String proposedDefinition,
        String proposedEnglishName,
        @NotNull List<Long> occurredDocumentIds,
        @Min(1) int occurrenceCount,
        @NotNull List<String> contextSnippets,
        List<String> variantForms) {

    public ExtractedTerm toPort() {
        return new ExtractedTerm(
                form,
                proposedDefinition,
                proposedEnglishName,
                List.copyOf(occurredDocumentIds),
                occurrenceCount,
                List.copyOf(contextSnippets),
                variantForms == null ? List.of() : List.copyOf(variantForms));
    }
}
