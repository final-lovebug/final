package com.ubidict.backend.draftdictionary.presentation;

import com.ubidict.backend.common.presentation.PageResponse;
import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import com.ubidict.backend.draftdictionary.presentation.dto.DraftDictionaryResponse;
import com.ubidict.backend.draftdictionary.presentation.dto.UpdateSourceDocumentsRequest;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryService;
import com.ubidict.backend.draftdictionary.service.model.CompleteExamineCommand;
import com.ubidict.backend.draftdictionary.service.model.DraftDictionarySearchQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 사전 초안 API입니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/draft-dictionaries")
public class DraftDictionaryController {

    private final DraftDictionaryService draftDictionaryService;

    /** 워크스페이스 기준 진행 중 사전 초안 조회(T-INT-20, D-64). */
    @GetMapping
    public PageResponse<DraftDictionaryResponse> search(
            @RequestParam Long workspaceId,
            @RequestParam(required = false) DraftDictionaryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal Long memberId) {
        return PageResponse.from(draftDictionaryService
                .search(new DraftDictionarySearchQuery(workspaceId, status, page, size, sort, memberId))
                .map(DraftDictionaryResponse::from));
    }

    @GetMapping("/{draftDictionaryId}")
    public ResponseEntity<DraftDictionaryResponse> read(
            @PathVariable Long draftDictionaryId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(
                DraftDictionaryResponse.from(draftDictionaryService.read(draftDictionaryId, memberId)));
    }

    @PutMapping("/{draftDictionaryId}/source-documents")
    public ResponseEntity<DraftDictionaryResponse> updateSourceDocuments(
            @PathVariable Long draftDictionaryId,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody UpdateSourceDocumentsRequest request) {
        return ResponseEntity.ok(DraftDictionaryResponse.from(
                draftDictionaryService.updateSourceDocuments(request.toCommand(draftDictionaryId, memberId))));
    }

    @DeleteMapping("/{draftDictionaryId}")
    public ResponseEntity<Void> delete(@PathVariable Long draftDictionaryId, @AuthenticationPrincipal Long memberId) {
        draftDictionaryService.delete(draftDictionaryId, memberId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{draftDictionaryId}/examine-completion")
    public ResponseEntity<DraftDictionaryResponse> complete(
            @PathVariable Long draftDictionaryId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(DraftDictionaryResponse.from(
                draftDictionaryService.completeExamine(new CompleteExamineCommand(draftDictionaryId, memberId))));
    }
}
