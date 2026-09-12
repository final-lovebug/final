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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 요청자 memberId를 요청 파라미터로 받는다. 인증 계층이 아직 없어 생긴 임시 방식이며 인증 도입 전까지 운영 배포 대상이 아니다.
 *
 * <p>TODO(NFR-USR-001): 인증이 들어오면 memberId 파라미터를 걷어내고 인증 주체에서 해석한다.
 */
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/review-requests/{reviewRequestId}/reviews")
    public ResponseEntity<ReviewResponse> submit(
            @PathVariable Long reviewRequestId,
            @RequestParam Long memberId,
            @Valid @RequestBody SubmitReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReviewResponse.from(reviewService.submit(request.toCommand(reviewRequestId, memberId))));
    }

    @GetMapping("/api/review-requests/{reviewRequestId}/reviews")
    public ResponseEntity<List<ReviewResponse>> list(
            @PathVariable Long reviewRequestId,
            @RequestParam Long memberId,
            @RequestParam(required = false) Integer targetRound) {
        return ResponseEntity.ok(reviewService.list(reviewRequestId, memberId, targetRound).stream()
                .map(ReviewResponse::from)
                .toList());
    }

    @GetMapping("/api/review-requests/{reviewRequestId}/review-progress")
    public ResponseEntity<ReviewProgressResponse> progress(
            @PathVariable Long reviewRequestId, @RequestParam Long memberId) {
        return ResponseEntity.ok(ReviewProgressResponse.from(reviewService.progress(reviewRequestId, memberId)));
    }
}
