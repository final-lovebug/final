package com.ubidict.backend.document.presentation;

import com.ubidict.backend.document.presentation.dto.LabelResponse;
import com.ubidict.backend.document.service.DocumentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 목록 조회만 연다. 라벨은 문서에 붙일 때 없으면 만들어지므로 생성·수정 엔드포인트를 두지 않는다.
 *
 * <p>TODO(NFR-USR-001): 인증이 들어오면 memberId 파라미터를 걷어낸다.
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/labels")
@RequiredArgsConstructor
public class LabelController {

    private final DocumentService documentService;

    @GetMapping
    public ResponseEntity<List<LabelResponse>> readAll(@PathVariable Long workspaceId, @RequestParam Long memberId) {
        List<LabelResponse> responses = documentService.readLabels(workspaceId, memberId).stream()
                .map(LabelResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }
}
