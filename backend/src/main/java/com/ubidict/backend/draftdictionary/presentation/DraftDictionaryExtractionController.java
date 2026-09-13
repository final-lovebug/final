package com.ubidict.backend.draftdictionary.presentation;

import com.ubidict.backend.draftdictionary.presentation.dto.CreateExtractionJobRequest;
import com.ubidict.backend.draftdictionary.presentation.dto.ExtractionJobResponse;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryExtractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** TODO(NFR-USR-001): 인증이 들어오면 memberId 파라미터를 걷어내고 인증 주체에서 해석한다. */
@RestController
@RequestMapping("/api/draft-dictionaries/extractions")
@RequiredArgsConstructor
public class DraftDictionaryExtractionController {

    private final DraftDictionaryExtractionService extractionService;

    @PostMapping
    public ResponseEntity<ExtractionJobResponse> request(
            @RequestParam Long memberId, @Valid @RequestBody CreateExtractionJobRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ExtractionJobResponse.from(extractionService.request(request.toCommand(memberId))));
    }

    @GetMapping("/{extractionJobId}")
    public ResponseEntity<ExtractionJobResponse> read(@PathVariable Long extractionJobId, @RequestParam Long memberId) {
        return ResponseEntity.ok(ExtractionJobResponse.from(extractionService.read(extractionJobId, memberId)));
    }
}
