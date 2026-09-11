package com.ubidict.backend.workspace.presentation;

import com.ubidict.backend.workspace.service.*;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/participants")
@RequiredArgsConstructor
public class ParticipantController {
    private final ParticipantService participantService;

    @GetMapping
    public ResponseEntity<List<ParticipantResponse>> readAll(@PathVariable Long workspaceId, @RequestParam Long memberId) {
        return ResponseEntity.ok(participantService.readAll(workspaceId, memberId).stream().map(ParticipantResponse::from).toList());
    }

    @PatchMapping("/{targetMemberId}/permission")
    public ResponseEntity<ParticipantResponse> changePermission(@PathVariable Long workspaceId, @PathVariable Long targetMemberId,
            @RequestParam Long memberId, @Valid @RequestBody ChangePermissionRequest request) {
        return ResponseEntity.ok(ParticipantResponse.from(participantService.changePermission(
                new ChangePermissionCommand(workspaceId, targetMemberId, request.permission(), memberId))));
    }

    @PostMapping("/{targetMemberId}/ownership")
    public ResponseEntity<Void> transferOwnership(@PathVariable Long workspaceId, @PathVariable Long targetMemberId, @RequestParam Long memberId) {
        participantService.transferOwnership(new TransferOwnershipCommand(workspaceId, targetMemberId, memberId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{targetMemberId}")
    public ResponseEntity<Void> remove(@PathVariable Long workspaceId, @PathVariable Long targetMemberId, @RequestParam Long memberId) {
        participantService.remove(new RemoveParticipantCommand(workspaceId, targetMemberId, memberId));
        return ResponseEntity.noContent().build();
    }
}
