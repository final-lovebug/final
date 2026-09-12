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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 요청자 memberId를 요청 파라미터로 받는다. 인증 계층이 아직 없어 생긴 임시 방식이며 인증 도입 전까지 운영 배포 대상이 아니다.
 *
 * <p>TODO(NFR-USR-001): 인증이 들어오면 memberId 파라미터를 걷어내고 인증 주체에서 해석한다.
 */
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
        return ResponseEntity.ok(
                PageResponse.from(service.search(new SuggestionTermSearchQuery(id, status, page, size, sort))
                        .map(SuggestionTermResponse::from)));
    }

    @PatchMapping("/api/suggestion-terms/{id}")
    public ResponseEntity<SuggestionTermResponse> edit(
            @PathVariable Long id, @RequestParam Long memberId, @Valid @RequestBody EditSuggestionTermRequest r) {
        return ResponseEntity.ok(SuggestionTermResponse.from(service.edit(r.toCommand(id, memberId))));
    }

    @DeleteMapping("/api/suggestion-terms/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/suggestion-terms/{id}/acceptance")
    public ResponseEntity<SuggestionTermResponse> accept(@PathVariable Long id, @RequestParam Long memberId) {
        return ResponseEntity.ok(
                SuggestionTermResponse.from(service.accept(new AcceptSuggestionTermCommand(id, memberId))));
    }

    @PostMapping("/api/suggestion-terms/{id}/rejection")
    public ResponseEntity<SuggestionTermResponse> reject(
            @PathVariable Long id,
            @RequestParam Long memberId,
            @Valid @RequestBody RejectSuggestionTermRequest request) {
        return ResponseEntity.ok(SuggestionTermResponse.from(service.reject(request.toCommand(id, memberId))));
    }
}
