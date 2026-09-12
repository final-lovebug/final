package com.ubidict.backend.draftdocument.presentation;

import com.ubidict.backend.common.presentation.PageResponse;
import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import com.ubidict.backend.draftdocument.presentation.dto.CreateDraftDocumentRequest;
import com.ubidict.backend.draftdocument.presentation.dto.DraftDocumentResponse;
import com.ubidict.backend.draftdocument.presentation.dto.ExamineProgressResponse;
import com.ubidict.backend.draftdocument.presentation.dto.UpdateDraftBodyRequest;
import com.ubidict.backend.draftdocument.service.DraftDocumentService;
import com.ubidict.backend.draftdocument.service.model.CompleteExamineCommand;
import com.ubidict.backend.draftdocument.service.model.DraftDocumentSearchQuery;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 요청자 memberId를 요청 파라미터로 받는다. 인증 계층이 아직 없어 생긴 임시 방식이며 인증 도입 전까지 운영 배포 대상이 아니다.
 *
 * <p>TODO(NFR-USR-001): 인증이 들어오면 memberId 파라미터를 걷어내고 인증 주체에서 해석한다.
 */
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
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return PageResponse.from(draftDocumentService
                .search(new DraftDocumentSearchQuery(documentId, status, page, size, sort))
                .map(DraftDocumentResponse::from));
    }

    @PostMapping
    public ResponseEntity<DraftDocumentResponse> create(
            @RequestParam Long memberId, @Valid @RequestBody CreateDraftDocumentRequest request) {
        DraftDocumentResponse response =
                DraftDocumentResponse.from(draftDocumentService.create(request.toCommand(memberId)));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{draftDocumentId}")
    public ResponseEntity<DraftDocumentResponse> read(@PathVariable Long draftDocumentId, @RequestParam Long memberId) {
        return ResponseEntity.ok(DraftDocumentResponse.from(draftDocumentService.read(draftDocumentId, memberId)));
    }

    @PatchMapping("/{draftDocumentId}")
    public ResponseEntity<DraftDocumentResponse> updateBody(
            @PathVariable Long draftDocumentId,
            @RequestParam Long memberId,
            @Valid @RequestBody UpdateDraftBodyRequest request) {
        return ResponseEntity.ok(DraftDocumentResponse.from(
                draftDocumentService.updateBody(request.toCommand(draftDocumentId, memberId))));
    }

    @DeleteMapping("/{draftDocumentId}")
    public ResponseEntity<Void> delete(@PathVariable Long draftDocumentId, @RequestParam Long memberId) {
        draftDocumentService.delete(draftDocumentId, memberId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{draftDocumentId}/examine-completion")
    public ResponseEntity<DraftDocumentResponse> completeExamine(
            @PathVariable Long draftDocumentId, @RequestParam Long memberId) {
        return ResponseEntity.ok(DraftDocumentResponse.from(
                draftDocumentService.completeExamine(new CompleteExamineCommand(draftDocumentId, memberId))));
    }

    @GetMapping("/{draftDocumentId}/examine-progress")
    public ResponseEntity<ExamineProgressResponse> readExamineProgress(
            @PathVariable Long draftDocumentId, @RequestParam Long memberId) {
        return ResponseEntity.ok(
                ExamineProgressResponse.from(draftDocumentService.readExamineProgress(draftDocumentId, memberId)));
    }
}
