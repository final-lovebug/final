package com.ubidict.backend.draftdocument.presentation;

import com.ubidict.backend.common.presentation.PageResponse;
import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import com.ubidict.backend.draftdocument.presentation.dto.DraftDocumentResponse;
import com.ubidict.backend.draftdocument.presentation.dto.ExamineProgressResponse;
import com.ubidict.backend.draftdocument.presentation.dto.UpdateDraftBodyRequest;
import com.ubidict.backend.draftdocument.service.DraftDocumentService;
import com.ubidict.backend.draftdocument.service.model.CompleteExamineCommand;
import com.ubidict.backend.draftdocument.service.model.DraftDocumentSearchQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/draft-documents")
@RequiredArgsConstructor
public class DraftDocumentController {

    private final DraftDocumentService draftDocumentService;

    @GetMapping
    public PageResponse<DraftDocumentResponse> search(
            @RequestParam(required = false) Long documentId,
            @RequestParam(required = false) DraftDocumentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal Long memberId) {
        return PageResponse.from(draftDocumentService
                .search(new DraftDocumentSearchQuery(documentId, status, page, size, sort, memberId))
                .map(DraftDocumentResponse::from));
    }

    @GetMapping("/{draftDocumentId}")
    public ResponseEntity<DraftDocumentResponse> read(
            @PathVariable Long draftDocumentId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(DraftDocumentResponse.from(draftDocumentService.read(draftDocumentId, memberId)));
    }

    @PatchMapping("/{draftDocumentId}")
    public ResponseEntity<DraftDocumentResponse> updateBody(
            @PathVariable Long draftDocumentId,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody UpdateDraftBodyRequest request) {
        return ResponseEntity.ok(DraftDocumentResponse.from(
                draftDocumentService.updateBody(request.toCommand(draftDocumentId, memberId))));
    }

    @DeleteMapping("/{draftDocumentId}")
    public ResponseEntity<Void> delete(@PathVariable Long draftDocumentId, @AuthenticationPrincipal Long memberId) {
        draftDocumentService.delete(draftDocumentId, memberId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{draftDocumentId}/examine-completion")
    public ResponseEntity<DraftDocumentResponse> completeExamine(
            @PathVariable Long draftDocumentId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(DraftDocumentResponse.from(
                draftDocumentService.completeExamine(new CompleteExamineCommand(draftDocumentId, memberId))));
    }

    @GetMapping("/{draftDocumentId}/examine-progress")
    public ResponseEntity<ExamineProgressResponse> readExamineProgress(
            @PathVariable Long draftDocumentId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(
                ExamineProgressResponse.from(draftDocumentService.readExamineProgress(draftDocumentId, memberId)));
    }
}
