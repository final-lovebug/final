package com.ubidict.backend.draftdictionary.presentation;

import com.ubidict.backend.draftdictionary.presentation.dto.CreateExtractionJobRequest;
import com.ubidict.backend.draftdictionary.presentation.dto.ExtractionJobResponse;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryExtractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/draft-dictionaries/extractions")
@RequiredArgsConstructor
public class DraftDictionaryExtractionController {

    private final DraftDictionaryExtractionService extractionService;

    @PostMapping
    public ResponseEntity<ExtractionJobResponse> request(
            @AuthenticationPrincipal Long memberId, @Valid @RequestBody CreateExtractionJobRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ExtractionJobResponse.from(extractionService.request(request.toCommand(memberId))));
    }

    @GetMapping("/{extractionJobId}")
    public ResponseEntity<ExtractionJobResponse> read(
            @PathVariable Long extractionJobId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ExtractionJobResponse.from(extractionService.read(extractionJobId, memberId)));
    }
}
