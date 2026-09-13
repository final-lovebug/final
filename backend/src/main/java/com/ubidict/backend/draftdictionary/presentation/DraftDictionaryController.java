package com.ubidict.backend.draftdictionary.presentation;

import com.ubidict.backend.draftdictionary.presentation.dto.DraftDictionaryResponse;
import com.ubidict.backend.draftdictionary.presentation.dto.ExamineProgressResponse;
import com.ubidict.backend.draftdictionary.presentation.dto.UpdateSourceDocumentsRequest;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryService;
import com.ubidict.backend.draftdictionary.service.model.CompleteExamineCommand;
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
import org.springframework.web.bind.annotation.RestController;

/** 사전 초안 API입니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/draft-dictionaries")
public class DraftDictionaryController {

    private final DraftDictionaryService draftDictionaryService;

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

    @GetMapping("/{draftDictionaryId}/examine-progress")
    public ResponseEntity<ExamineProgressResponse> examineProgress(
            @PathVariable Long draftDictionaryId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(
                ExamineProgressResponse.from(draftDictionaryService.readExamineProgress(draftDictionaryId, memberId)));
    }

    @PostMapping("/{draftDictionaryId}/examine-completion")
    public ResponseEntity<DraftDictionaryResponse> complete(
            @PathVariable Long draftDictionaryId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(DraftDictionaryResponse.from(
                draftDictionaryService.completeExamine(new CompleteExamineCommand(draftDictionaryId, memberId))));
    }
}
