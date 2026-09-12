package com.ubidict.backend.draftdictionary.presentation;

import com.ubidict.backend.draftdictionary.presentation.dto.CreateDraftDictionaryRequest;
import com.ubidict.backend.draftdictionary.presentation.dto.DraftDictionaryResponse;
import com.ubidict.backend.draftdictionary.presentation.dto.UpdateSourceDocumentsRequest;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사전 초안 API입니다.
 *
 * <p>요청의 memberId는 인증 체계가 도입되기 전까지 사용하는 임시 식별자이며 운영 배포 대상이 아닙니다.
 *
 * <p>TODO(NFR-USR-001): 인증 도입 후 memberId 요청 파라미터를 제거하고 인증 주체에서 식별합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/draft-dictionaries")
public class DraftDictionaryController {

    private final DraftDictionaryService draftDictionaryService;

    @PostMapping
    public ResponseEntity<DraftDictionaryResponse> create(
            @RequestParam Long memberId, @Valid @RequestBody CreateDraftDictionaryRequest request) {
        DraftDictionaryResponse response =
                DraftDictionaryResponse.from(draftDictionaryService.create(request.toCommand(memberId)));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{draftDictionaryId}")
    public ResponseEntity<DraftDictionaryResponse> read(
            @PathVariable Long draftDictionaryId, @RequestParam Long memberId) {
        return ResponseEntity.ok(
                DraftDictionaryResponse.from(draftDictionaryService.read(draftDictionaryId, memberId)));
    }

    @PutMapping("/{draftDictionaryId}/source-documents")
    public ResponseEntity<DraftDictionaryResponse> updateSourceDocuments(
            @PathVariable Long draftDictionaryId,
            @RequestParam Long memberId,
            @Valid @RequestBody UpdateSourceDocumentsRequest request) {
        return ResponseEntity.ok(DraftDictionaryResponse.from(
                draftDictionaryService.updateSourceDocuments(request.toCommand(draftDictionaryId, memberId))));
    }

    @DeleteMapping("/{draftDictionaryId}")
    public ResponseEntity<Void> delete(@PathVariable Long draftDictionaryId, @RequestParam Long memberId) {
        draftDictionaryService.delete(draftDictionaryId, memberId);

        return ResponseEntity.noContent().build();
    }
}
