package com.ubidict.backend.draftdocument.presentation;

import com.ubidict.backend.common.presentation.PageResponse;
import com.ubidict.backend.draftdocument.domain.SuggestionTermStatus;
import com.ubidict.backend.draftdocument.presentation.dto.*;
import com.ubidict.backend.draftdocument.service.SuggestionTermService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SuggestionTermController {
    private final SuggestionTermService service;

    @PostMapping("/api/draft-documents/{id}/suggestion-terms")
    public ResponseEntity<SuggestionTermResponse> add(
            @PathVariable Long id, @RequestParam Long memberId, @Valid @RequestBody AddSuggestionTermRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuggestionTermResponse.from(service.add(r.toCommand(id, memberId))));
    }

    @GetMapping("/api/draft-documents/{id}/suggestion-terms")
    public ResponseEntity<PageResponse<SuggestionTermResponse>> search(
            @PathVariable Long id,
            @RequestParam(required = false) SuggestionTermStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(PageResponse.from(
                service.search(new com.ubidict.backend.draftdocument.service.model.SuggestionTermSearchQuery(
                                id, status, page, size, sort))
                        .map(SuggestionTermResponse::from)));
    }

    @PatchMapping("/api/suggestion-terms/{id}")
    public SuggestionTermResponse edit(
            @PathVariable Long id, @RequestParam Long memberId, @RequestBody EditSuggestionTermRequest r) {
        return SuggestionTermResponse.from(service.edit(r.toCommand(id, memberId)));
    }

    @DeleteMapping("/api/suggestion-terms/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
