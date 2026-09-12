package com.ubidict.backend.reviewrequest.presentation;

import com.ubidict.backend.common.presentation.PageResponse;
import com.ubidict.backend.reviewrequest.domain.*;
import com.ubidict.backend.reviewrequest.presentation.dto.CreateReviewRequestRequest;
import com.ubidict.backend.reviewrequest.presentation.dto.ReviewRequestResponse;
import com.ubidict.backend.reviewrequest.presentation.dto.UpdateReviewRequestRequest;
import com.ubidict.backend.reviewrequest.service.ReviewRequestService;
import com.ubidict.backend.reviewrequest.service.model.CancelReviewRequestCommand;
import com.ubidict.backend.reviewrequest.service.model.ReviewRequestSearchQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 요청자 memberId를 요청 파라미터로 받는다. 인증 계층이 아직 없어 생긴 임시 방식이며 인증 도입 전까지 운영 배포 대상이 아니다.
 *
 * <p>TODO(NFR-USR-001): 인증이 들어오면 memberId 파라미터를 걷어내고 인증 주체에서 해석한다.
 */
@RestController
@RequestMapping("/api/review-requests")
@RequiredArgsConstructor
public class ReviewRequestController {

    private final ReviewRequestService reviewRequestService;

    @GetMapping
    public ResponseEntity<PageResponse<ReviewRequestResponse>> search(
            @RequestParam Long workspaceId,
            @RequestParam(required = false) ReviewRequestType type,
            @RequestParam(required = false) ReviewRequestStatus status,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) Long reviewerMemberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        var result = reviewRequestService.search(new ReviewRequestSearchQuery(
                workspaceId, type, status, requesterId, reviewerMemberId, page, size, sort));
        return ResponseEntity.ok(PageResponse.from(result.map(ReviewRequestResponse::from)));
    }

    @PostMapping
    public ResponseEntity<ReviewRequestResponse> create(
            @RequestParam Long memberId, @Valid @RequestBody CreateReviewRequestRequest request) {
        ReviewRequestResponse response =
                ReviewRequestResponse.from(reviewRequestService.create(request.toCommand(memberId)));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{reviewRequestId}")
    public ResponseEntity<ReviewRequestResponse> read(@PathVariable Long reviewRequestId, @RequestParam Long memberId) {
        return ResponseEntity.ok(ReviewRequestResponse.from(reviewRequestService.read(reviewRequestId, memberId)));
    }

    @PatchMapping("/{reviewRequestId}")
    public ResponseEntity<ReviewRequestResponse> update(
            @PathVariable Long reviewRequestId,
            @RequestParam Long memberId,
            @Valid @RequestBody UpdateReviewRequestRequest request) {
        return ResponseEntity.ok(
                ReviewRequestResponse.from(reviewRequestService.update(request.toCommand(reviewRequestId, memberId))));
    }

    @PostMapping("/{reviewRequestId}/cancellation")
    public ResponseEntity<ReviewRequestResponse> cancel(
            @PathVariable Long reviewRequestId, @RequestParam Long memberId) {
        return ResponseEntity.ok(ReviewRequestResponse.from(
                reviewRequestService.cancel(new CancelReviewRequestCommand(reviewRequestId, memberId))));
    }
}
