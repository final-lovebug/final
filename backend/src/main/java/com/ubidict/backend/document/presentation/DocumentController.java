package com.ubidict.backend.document.presentation;

import com.ubidict.backend.document.presentation.dto.CreateDocumentRequest;
import com.ubidict.backend.document.presentation.dto.DocumentResponse;
import com.ubidict.backend.document.presentation.dto.DocumentSummaryResponse;
import com.ubidict.backend.document.presentation.dto.DocumentVersionResponse;
import com.ubidict.backend.document.presentation.dto.DocumentVersionSummaryResponse;
import com.ubidict.backend.document.presentation.dto.UpdateDocumentRequest;
import com.ubidict.backend.document.service.DocumentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 모든 경로가 워크스페이스 하위에 중첩된다. workspaceId가 URL에 강제되면 데이터 격리(NFR-WS-001) 검증이 모든 엔드포인트에서 같은 모양이 된다.
 *
 * <p>본문을 바꾸는 엔드포인트가 없다. 문서 편집은 대조를 실행해 초안을 만드는 별개의 유스케이스이며, draftdocument 도메인이
 * {@code POST /documents/{documentId}/drafts}로 연다(REQ-CHK-007). 아래 PATCH는 제목·라벨 전용이다.
 *
 * <p>요청자 memberId를 요청 파라미터로 받는다. 인증 계층이 아직 없어 생긴 임시 방식이며 인증 도입 전까지 운영 배포 대상이 아니다.
 *
 * <p>TODO(NFR-USR-001): 인증이 들어오면 memberId 파라미터를 걷어내고 인증 주체에서 해석한다. 바꿀 지점은 이 클래스의 파라미터뿐이고 service 이하는
 * 손대지 않는다.
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public ResponseEntity<DocumentResponse> create(
            @PathVariable Long workspaceId,
            @RequestParam Long memberId,
            @Valid @RequestBody CreateDocumentRequest request) {
        DocumentResponse response =
                DocumentResponse.from(documentService.create(request.toCommand(workspaceId, memberId)));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentSummaryResponse>> readAll(
            @PathVariable Long workspaceId, @RequestParam Long memberId, @RequestParam(required = false) String label) {
        List<DocumentSummaryResponse> responses = documentService.readAll(workspaceId, memberId, label).stream()
                .map(DocumentSummaryResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentResponse> read(
            @PathVariable Long workspaceId, @PathVariable Long documentId, @RequestParam Long memberId) {
        return ResponseEntity.ok(DocumentResponse.from(documentService.read(workspaceId, documentId, memberId)));
    }

    @PatchMapping("/{documentId}")
    public ResponseEntity<Void> update(
            @PathVariable Long workspaceId,
            @PathVariable Long documentId,
            @RequestParam Long memberId,
            @Valid @RequestBody UpdateDocumentRequest request) {
        documentService.update(request.toCommand(workspaceId, documentId, memberId));

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long workspaceId, @PathVariable Long documentId, @RequestParam Long memberId) {
        documentService.delete(workspaceId, documentId, memberId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{documentId}/versions")
    public ResponseEntity<List<DocumentVersionSummaryResponse>> readVersions(
            @PathVariable Long workspaceId, @PathVariable Long documentId, @RequestParam Long memberId) {
        List<DocumentVersionSummaryResponse> responses =
                documentService.readVersions(workspaceId, documentId, memberId).stream()
                        .map(DocumentVersionSummaryResponse::from)
                        .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{documentId}/versions/{versionNo}")
    public ResponseEntity<DocumentVersionResponse> readVersion(
            @PathVariable Long workspaceId,
            @PathVariable Long documentId,
            @PathVariable int versionNo,
            @RequestParam Long memberId) {
        return ResponseEntity.ok(DocumentVersionResponse.from(
                documentService.readVersion(workspaceId, documentId, versionNo, memberId)));
    }
}
