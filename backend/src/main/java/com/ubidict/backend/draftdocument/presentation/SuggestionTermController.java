package com.ubidict.backend.draftdocument.presentation;

import com.ubidict.backend.common.presentation.PageResponse;
import com.ubidict.backend.draftdocument.domain.SuggestionTermStatus;
import com.ubidict.backend.draftdocument.presentation.dto.AddSuggestionTermRequest;
import com.ubidict.backend.draftdocument.presentation.dto.EditSuggestionTermRequest;
import com.ubidict.backend.draftdocument.presentation.dto.RejectSuggestionTermRequest;
import com.ubidict.backend.draftdocument.presentation.dto.SuggestionTermResponse;
import com.ubidict.backend.draftdocument.service.SuggestionTermService;
import com.ubidict.backend.draftdocument.service.model.AcceptSuggestionTermCommand;
import com.ubidict.backend.draftdocument.service.model.SuggestionTermSearchQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SuggestionTermController {
    private final SuggestionTermService service;

    @PostMapping("/api/draft-documents/{id}/suggestion-terms")
    public ResponseEntity<SuggestionTermResponse> add(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody AddSuggestionTermRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuggestionTermResponse.from(service.add(r.toCommand(id, memberId))));
    }

    @GetMapping("/api/draft-documents/{id}/suggestion-terms")
    public ResponseEntity<PageResponse<SuggestionTermResponse>> search(
            @PathVariable Long id,
            @RequestParam(required = false) SuggestionTermStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(
                PageResponse.from(service.search(new SuggestionTermSearchQuery(id, status, page, size, sort, memberId))
                        .map(SuggestionTermResponse::from)));
    }

    @PatchMapping("/api/suggestion-terms/{id}")
    public ResponseEntity<SuggestionTermResponse> edit(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody EditSuggestionTermRequest r) {
        return ResponseEntity.ok(SuggestionTermResponse.from(service.edit(r.toCommand(id, memberId))));
    }

    @DeleteMapping("/api/suggestion-terms/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Long memberId) {
        service.delete(id, memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/suggestion-terms/{id}/acceptance")
    public ResponseEntity<SuggestionTermResponse> accept(
            @PathVariable Long id, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(
                SuggestionTermResponse.from(service.accept(new AcceptSuggestionTermCommand(id, memberId))));
    }

    @PostMapping("/api/suggestion-terms/{id}/rejection")
    public ResponseEntity<SuggestionTermResponse> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody RejectSuggestionTermRequest request) {
        return ResponseEntity.ok(SuggestionTermResponse.from(service.reject(request.toCommand(id, memberId))));
    }
}
