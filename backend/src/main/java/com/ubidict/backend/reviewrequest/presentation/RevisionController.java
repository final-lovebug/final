package com.ubidict.backend.reviewrequest.presentation;

import com.ubidict.backend.reviewrequest.presentation.dto.RevisionResponse;
import com.ubidict.backend.reviewrequest.presentation.dto.SubmitRevisionDocumentRequest;
import com.ubidict.backend.reviewrequest.service.RevisionService;
import jakarta.validation.Valid;
import java.util.List;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review-requests/{reviewRequestId}")
public class RevisionController {
    private final RevisionService service;

    @PostMapping("/revision-documents")
    public ResponseEntity<RevisionResponse> document(
            @PathVariable Long reviewRequestId,
            @RequestParam Long memberId,
            @Valid @RequestBody SubmitRevisionDocumentRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RevisionResponse.from(service.submitDocument(r.toCommand(reviewRequestId, memberId))));
    }

    @GetMapping("/revision-documents")
    public ResponseEntity<List<RevisionResponse>> documents(
            @PathVariable Long reviewRequestId, @RequestParam(required = false) Integer round) {
        return ResponseEntity.ok(service.documents(reviewRequestId, round).stream()
                .map(RevisionResponse::from)
                .toList());
    }
}
