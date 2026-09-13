package com.ubidict.backend.reviewrequest.presentation;

import com.ubidict.backend.reviewrequest.presentation.dto.ReviseResponse;
import com.ubidict.backend.reviewrequest.service.ReviseService;
import com.ubidict.backend.reviewrequest.service.model.PerformReviseCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review-requests/{reviewRequestId}/revision")
@RequiredArgsConstructor
public class ReviseController {

    private final ReviseService reviseService;

    @PostMapping
    public ResponseEntity<ReviseResponse> perform(
            @PathVariable Long reviewRequestId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(
                ReviseResponse.from(reviseService.perform(new PerformReviseCommand(reviewRequestId, memberId))));
    }
}
