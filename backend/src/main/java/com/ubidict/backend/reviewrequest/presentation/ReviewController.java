package com.ubidict.backend.reviewrequest.presentation;

import com.ubidict.backend.reviewrequest.presentation.dto.ReviewProgressResponse;
import com.ubidict.backend.reviewrequest.presentation.dto.ReviewResponse;
import com.ubidict.backend.reviewrequest.presentation.dto.SubmitReviewRequest;
import com.ubidict.backend.reviewrequest.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/review-requests/{reviewRequestId}/reviews")
    public ResponseEntity<ReviewResponse> submit(
            @PathVariable Long reviewRequestId,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody SubmitReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReviewResponse.from(reviewService.submit(request.toCommand(reviewRequestId, memberId))));
    }

    @GetMapping("/api/review-requests/{reviewRequestId}/reviews")
    public ResponseEntity<List<ReviewResponse>> list(
            @PathVariable Long reviewRequestId,
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) Integer targetRound) {
        return ResponseEntity.ok(reviewService.list(reviewRequestId, memberId, targetRound).stream()
                .map(ReviewResponse::from)
                .toList());
    }

    @GetMapping("/api/review-requests/{reviewRequestId}/review-progress")
    public ResponseEntity<ReviewProgressResponse> progress(
            @PathVariable Long reviewRequestId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ReviewProgressResponse.from(reviewService.progress(reviewRequestId, memberId)));
    }
}
