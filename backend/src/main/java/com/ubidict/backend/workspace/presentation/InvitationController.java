package com.ubidict.backend.workspace.presentation;

import com.ubidict.backend.workspace.domain.InvitationStatus;
import com.ubidict.backend.workspace.presentation.dto.InvitationResponse;
import com.ubidict.backend.workspace.presentation.dto.IssueInvitationRequest;
import com.ubidict.backend.workspace.presentation.dto.WorkspaceResponse;
import com.ubidict.backend.workspace.service.InvitationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TODO(NFR-USR-001): 인증이 들어오면 memberId 파라미터를 걷어내고 인증 주체에서 해석한다.
 *
 * <p>인증 도입 전까지 운영 배포 대상이 아니다.
 */
@RestController
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/api/workspaces/{workspaceId}/invitations")
    public ResponseEntity<InvitationResponse> issue(
            @PathVariable Long workspaceId,
            @RequestParam Long memberId,
            @Valid @RequestBody IssueInvitationRequest request) {
        InvitationResponse response =
                InvitationResponse.issued(invitationService.issue(request.toCommand(workspaceId, memberId)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/workspaces/{workspaceId}/invitations")
    public ResponseEntity<List<InvitationResponse>> readAll(
            @PathVariable Long workspaceId,
            @RequestParam Long memberId,
            @RequestParam(required = false) InvitationStatus status) {
        List<InvitationResponse> responses = invitationService.readAll(workspaceId, memberId, status).stream()
                .map(InvitationResponse::listed)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/api/workspaces/{workspaceId}/invitations/{invitationId}")
    public ResponseEntity<Void> cancel(
            @PathVariable Long workspaceId, @PathVariable Long invitationId, @RequestParam Long memberId) {
        invitationService.cancel(workspaceId, invitationId, memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/invitations/{token}/accept")
    public ResponseEntity<WorkspaceResponse> accept(@PathVariable String token, @RequestParam Long memberId) {
        WorkspaceResponse response = WorkspaceResponse.from(invitationService.accept(token, memberId));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
