package com.ubidict.backend.reviewrequest.presentation;

import com.ubidict.backend.reviewrequest.presentation.dto.RevisionResponse;
import com.ubidict.backend.reviewrequest.service.RevisionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review-requests/{reviewRequestId}")
public class RevisionController {
    private final RevisionService service;

    @GetMapping("/revision-documents")
    public ResponseEntity<List<RevisionResponse>> documents(
            @PathVariable Long reviewRequestId,
            @RequestParam(required = false) Integer round,
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(service.documents(reviewRequestId, round, memberId).stream()
                .map(RevisionResponse::from)
                .toList());
    }

    @GetMapping("/revision-dictionaries")
    public ResponseEntity<List<RevisionResponse>> dictionaries(
            @PathVariable Long reviewRequestId,
            @RequestParam(required = false) Integer round,
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(service.dictionaries(reviewRequestId, round, memberId).stream()
                .map(RevisionResponse::from)
                .toList());
    }
}
