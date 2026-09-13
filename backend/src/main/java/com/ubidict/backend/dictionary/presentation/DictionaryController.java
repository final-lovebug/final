package com.ubidict.backend.dictionary.presentation;

import com.ubidict.backend.common.presentation.PageResponse;
import com.ubidict.backend.dictionary.presentation.dto.DictionaryResponse;
import com.ubidict.backend.dictionary.presentation.dto.DictionaryVersionResponse;
import com.ubidict.backend.dictionary.service.DictionaryService;
import com.ubidict.backend.dictionary.service.model.DictionarySearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 워크스페이스에 사전집은 활성 1개 + 보관 N개로 존재하므로 경로를 단수로 둔다.
 *
 * <p>요청자 memberId를 요청 파라미터로 받는다. 인증 계층이 아직 없어 생긴 임시 방식이며 인증 도입 전까지 운영 배포 대상이 아니다.
 *
 * <p>TODO(NFR-USR-001): 인증이 들어오면 memberId 파라미터를 걷어내고 인증 주체에서 해석한다.
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/dictionary")
@RequiredArgsConstructor
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @GetMapping
    public ResponseEntity<DictionaryResponse> readActive(
            @PathVariable Long workspaceId,
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "preferredForm,asc") String sort,
            @RequestParam(required = false) String keyword) {
        var result = dictionaryService.readActive(
                workspaceId, memberId, new DictionarySearchQuery(page, size, sort, keyword));
        return ResponseEntity.ok(DictionaryResponse.from(result));
    }

    @GetMapping("/versions")
    public ResponseEntity<PageResponse<DictionaryVersionResponse>> readVersions(
            @PathVariable Long workspaceId,
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = dictionaryService.readVersions(workspaceId, memberId, page, size);
        return ResponseEntity.ok(PageResponse.from(result.map(DictionaryVersionResponse::from)));
    }

    @GetMapping("/versions/{versionNo}")
    public ResponseEntity<DictionaryResponse> readVersion(
            @PathVariable Long workspaceId,
            @PathVariable int versionNo,
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "preferredForm,asc") String sort,
            @RequestParam(required = false) String keyword) {
        var result = dictionaryService.readVersion(
                workspaceId, versionNo, memberId, new DictionarySearchQuery(page, size, sort, keyword));
        return ResponseEntity.ok(DictionaryResponse.from(result));
    }
}
