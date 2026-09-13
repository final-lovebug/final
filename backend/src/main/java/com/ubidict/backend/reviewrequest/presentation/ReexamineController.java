package com.ubidict.backend.reviewrequest.presentation;

import com.ubidict.backend.reviewrequest.presentation.dto.PerformReexamineRequest;
import com.ubidict.backend.reviewrequest.presentation.dto.ReexamineResponse;
import com.ubidict.backend.reviewrequest.service.ReexamineService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review-requests/{reviewRequestId}/reexaminations")
@RequiredArgsConstructor
public class ReexamineController {

    private final ReexamineService reexamineService;

    @PostMapping
    public ResponseEntity<ReexamineResponse> perform(
            @PathVariable Long reviewRequestId,
            @AuthenticationPrincipal Long memberId,
            @RequestBody PerformReexamineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReexamineResponse.from(reexamineService.perform(request.to(reviewRequestId, memberId))));
    }

    @GetMapping
    public ResponseEntity<List<ReexamineResponse>> list(
            @PathVariable Long reviewRequestId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(reexamineService.list(reviewRequestId, memberId).stream()
                .map(ReexamineResponse::from)
                .toList());
    }
}
