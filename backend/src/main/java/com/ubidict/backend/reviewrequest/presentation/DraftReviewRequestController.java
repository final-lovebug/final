package com.ubidict.backend.reviewrequest.presentation;

import com.ubidict.backend.reviewrequest.presentation.dto.RequestDraftReviewRequest;
import com.ubidict.backend.reviewrequest.presentation.dto.ReviewRequestResponse;
import com.ubidict.backend.reviewrequest.service.DraftReviewRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 초안에서 리뷰 요청을 시작하는 두 진입점.
 *
 * <p><b>경로는 초안 아래인데 컨트롤러는 이 도메인에 있다.</b> 리뷰 요청과 최초 개정안을 한 트랜잭션에서 만들어야 하므로 조립 주체가 이 도메인이어야 하고(D-44),
 * 초안 도메인에 두면 그쪽이 reviewrequest를 참조하게 된다.
 *
 * <p>TODO(NFR-USR-001): 인증이 들어오면 memberId 파라미터를 걷어내고 인증 주체에서 해석한다.
 */
@RestController
@RequiredArgsConstructor
public class DraftReviewRequestController {

    private final DraftReviewRequestService draftReviewRequestService;

    @PostMapping("/api/draft-documents/{draftDocumentId}/review-request")
    public ResponseEntity<ReviewRequestResponse> requestDocumentReview(
            @PathVariable Long draftDocumentId,
            @RequestParam Long memberId,
            @Valid @RequestBody RequestDraftReviewRequest request) {
        ReviewRequestResponse response = ReviewRequestResponse.from(
                draftReviewRequestService.requestDocumentReview(request.toDocumentCommand(draftDocumentId, memberId)));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/draft-dictionaries/{draftDictionaryId}/review-request")
    public ResponseEntity<ReviewRequestResponse> requestDictionaryReview(
            @PathVariable Long draftDictionaryId,
            @RequestParam Long memberId,
            @Valid @RequestBody RequestDraftReviewRequest request) {
        ReviewRequestResponse response = ReviewRequestResponse.from(draftReviewRequestService.requestDictionaryReview(
                request.toDictionaryCommand(draftDictionaryId, memberId)));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
