package com.ubidict.backend.reviewrequest.presentation;

import com.ubidict.backend.reviewrequest.presentation.dto.PerformReexamineRequest;
import com.ubidict.backend.reviewrequest.presentation.dto.ReexamineResponse;
import com.ubidict.backend.reviewrequest.service.ReexamineService;
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

/**
 * 요청자 memberId를 요청 파라미터로 받는다. 인증 계층이 아직 없어 생긴 임시 방식이며 인증 도입 전까지 운영 배포 대상이 아니다.
 *
 * <p>TODO(NFR-USR-001): 인증이 들어오면 memberId 파라미터를 걷어내고 인증 주체에서 해석한다.
 */
@RestController
@RequestMapping("/api/review-requests/{reviewRequestId}/reexaminations")
@RequiredArgsConstructor
public class ReexamineController {

    private final ReexamineService reexamineService;

    @PostMapping
    public ResponseEntity<ReexamineResponse> perform(
            @PathVariable Long reviewRequestId,
            @RequestParam Long memberId,
            @RequestBody PerformReexamineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReexamineResponse.from(reexamineService.perform(request.to(reviewRequestId, memberId))));
    }

    @GetMapping
    public ResponseEntity<List<ReexamineResponse>> list(
            @PathVariable Long reviewRequestId, @RequestParam Long memberId) {
        return ResponseEntity.ok(reexamineService.list(reviewRequestId, memberId).stream()
                .map(ReexamineResponse::from)
                .toList());
    }
}
