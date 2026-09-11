package com.ubidict.backend.dictionary.presentation;

import com.ubidict.backend.dictionary.presentation.dto.DictionaryResponse;
import com.ubidict.backend.dictionary.presentation.dto.DictionaryVersionResponse;
import com.ubidict.backend.dictionary.presentation.dto.ReviseDictionaryRequest;
import com.ubidict.backend.dictionary.service.DictionaryService;
import jakarta.validation.Valid;
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

    /**
     * 새 사전집 버전을 반영한다.
     *
     * <p>TODO(REQ-REV-005, DIC-1): 사전집 버전은 원래 리뷰 승인의 산출물이다. 리뷰 도메인이 완성되면 승인 처리가 DictionaryService.revise를 직접
     * 호출하도록 옮기고 이 엔드포인트를 제거한다.
     */
    @PostMapping("/versions")
    public ResponseEntity<DictionaryResponse> revise(
            @PathVariable Long workspaceId,
            @RequestParam Long memberId,
            @Valid @RequestBody ReviseDictionaryRequest request) {
        DictionaryResponse response =
                DictionaryResponse.from(dictionaryService.revise(request.toCommand(workspaceId, memberId)));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<DictionaryResponse> readActive(@PathVariable Long workspaceId, @RequestParam Long memberId) {
        return ResponseEntity.ok(DictionaryResponse.from(dictionaryService.readActive(workspaceId, memberId)));
    }

    @GetMapping("/versions")
    public ResponseEntity<List<DictionaryVersionResponse>> readVersions(
            @PathVariable Long workspaceId, @RequestParam Long memberId) {
        List<DictionaryVersionResponse> responses = dictionaryService.readVersions(workspaceId, memberId).stream()
                .map(DictionaryVersionResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/versions/{versionNo}")
    public ResponseEntity<DictionaryResponse> readVersion(
            @PathVariable Long workspaceId, @PathVariable int versionNo, @RequestParam Long memberId) {
        return ResponseEntity.ok(
                DictionaryResponse.from(dictionaryService.readVersion(workspaceId, versionNo, memberId)));
    }
}
