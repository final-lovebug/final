package com.ubidict.backend.reviewrequest.presentation;

import com.ubidict.backend.reviewrequest.presentation.dto.AssignReviewerRequest;
import com.ubidict.backend.reviewrequest.presentation.dto.ReviewerResponse;
import com.ubidict.backend.reviewrequest.service.ReviewerService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review-requests/{reviewRequestId}/reviewers")
public class ReviewerController {
    private final ReviewerService service;

    @PostMapping
    public ResponseEntity<ReviewerResponse> assign(
            @PathVariable Long reviewRequestId,
            @RequestParam Long memberId,
            @Valid @RequestBody AssignReviewerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReviewerResponse.from(service.assign(request.toCommand(reviewRequestId, memberId))));
    }

    @GetMapping
    public ResponseEntity<List<ReviewerResponse>> list(@PathVariable Long reviewRequestId) {
        return ResponseEntity.ok(service.list(reviewRequestId).stream()
                .map(ReviewerResponse::from)
                .toList());
    }

    @DeleteMapping("/{reviewerId}")
    public ResponseEntity<Void> remove(@PathVariable Long reviewerId) {
        service.remove(reviewerId);
        return ResponseEntity.noContent().build();
    }
}
