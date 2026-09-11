package com.ubidict.backend.workspace.presentation;

import com.ubidict.backend.workspace.service.ChangePermissionCommand;
import com.ubidict.backend.workspace.service.ParticipantService;
import com.ubidict.backend.workspace.service.RemoveParticipantCommand;
import com.ubidict.backend.workspace.service.TransferOwnershipCommand;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping("/api/workspaces/{workspaceId}/participants")
@RequiredArgsConstructor
public class ParticipantController {
    private final ParticipantService participantService;

    @GetMapping
    public ResponseEntity<List<ParticipantResponse>> readAll(
            @PathVariable Long workspaceId, @RequestParam Long memberId) {
        return ResponseEntity.ok(participantService.readAll(workspaceId, memberId).stream()
                .map(ParticipantResponse::from)
                .toList());
    }

    @PatchMapping("/{participantId}/permission")
    public ResponseEntity<Void> changePermission(
            @PathVariable Long workspaceId,
            @PathVariable Long participantId,
            @RequestParam Long memberId,
            @Valid @RequestBody ChangePermissionRequest request) {
        participantService.changePermission(
                new ChangePermissionCommand(workspaceId, participantId, request.permission(), memberId));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{participantId}/ownership")
    public ResponseEntity<Void> transferOwnership(
            @PathVariable Long workspaceId, @PathVariable Long participantId, @RequestParam Long memberId) {
        participantService.transferOwnership(new TransferOwnershipCommand(workspaceId, participantId, memberId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{participantId}")
    public ResponseEntity<Void> remove(
            @PathVariable Long workspaceId, @PathVariable Long participantId, @RequestParam Long memberId) {
        participantService.remove(new RemoveParticipantCommand(workspaceId, participantId, memberId));
        return ResponseEntity.noContent().build();
    }
}
