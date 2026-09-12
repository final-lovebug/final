package com.ubidict.backend.reviewrequest.presentation;

import com.ubidict.backend.reviewrequest.presentation.dto.AddCommentRequest;
import com.ubidict.backend.reviewrequest.presentation.dto.CommentResponse;
import com.ubidict.backend.reviewrequest.presentation.dto.ResolveCommentRequest;
import com.ubidict.backend.reviewrequest.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/api/reviews/{reviewId}/comments")
    public ResponseEntity<CommentResponse> add(
            @PathVariable Long reviewId, @RequestParam Long memberId, @Valid @RequestBody AddCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommentResponse.from(commentService.add(request.toCommand(reviewId, memberId))));
    }

    @GetMapping("/api/review-requests/{reviewRequestId}/comments")
    public ResponseEntity<List<CommentResponse>> list(
            @PathVariable Long reviewRequestId,
            @RequestParam Long memberId,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(required = false) Long targetItemId) {
        return ResponseEntity.ok(commentService.list(reviewRequestId, memberId, resolved, targetItemId).stream()
                .map(CommentResponse::from)
                .toList());
    }

    @PatchMapping("/api/comments/{commentId}/resolution")
    public ResponseEntity<CommentResponse> resolve(
            @PathVariable Long commentId,
            @RequestParam Long memberId,
            @Valid @RequestBody ResolveCommentRequest request) {
        return ResponseEntity.ok(CommentResponse.from(commentService.resolve(request.toCommand(commentId, memberId))));
    }
}
