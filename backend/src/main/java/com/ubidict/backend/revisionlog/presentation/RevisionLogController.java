package com.ubidict.backend.revisionlog.presentation;

import com.ubidict.backend.common.presentation.PageResponse;
import com.ubidict.backend.revisionlog.domain.RevisionLogTargetType;
import com.ubidict.backend.revisionlog.presentation.dto.RevisionLogDetailResponse;
import com.ubidict.backend.revisionlog.presentation.dto.RevisionLogResponse;
import com.ubidict.backend.revisionlog.service.RevisionLogService;
import com.ubidict.backend.revisionlog.service.model.RevisionLogSearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 확정 후에는 바뀌지 않는 사전집·문서 개정 이력을 조회한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workspaces/{workspaceId}/revision-logs")
public class RevisionLogController {

    private final RevisionLogService revisionLogService;

    @GetMapping
    public ResponseEntity<PageResponse<RevisionLogResponse>> search(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal Long memberId,
            @RequestParam RevisionLogTargetType targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        RevisionLogSearchQuery query =
                RevisionLogSearchQuery.of(workspaceId, memberId, targetType, targetId, page, size, sort);
        return ResponseEntity.ok(
                PageResponse.from(revisionLogService.search(query).map(RevisionLogResponse::from)));
    }

    @GetMapping("/{revisionLogId}")
    public ResponseEntity<RevisionLogDetailResponse> read(
            @PathVariable Long workspaceId, @PathVariable Long revisionLogId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(
                RevisionLogDetailResponse.from(revisionLogService.read(workspaceId, revisionLogId, memberId)));
    }
}
