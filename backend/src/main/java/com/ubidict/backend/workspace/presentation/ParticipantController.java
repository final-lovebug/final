package com.ubidict.backend.workspace.presentation;

import com.ubidict.backend.workspace.presentation.dto.ChangePermissionRequest;
import com.ubidict.backend.workspace.presentation.dto.ParticipantResponse;
import com.ubidict.backend.workspace.service.ParticipantService;
import com.ubidict.backend.workspace.service.model.RemoveParticipantCommand;
import com.ubidict.backend.workspace.service.model.TransferOwnershipCommand;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/participants")
@RequiredArgsConstructor
public class ParticipantController {
    private final ParticipantService participantService;

    @GetMapping
    public ResponseEntity<List<ParticipantResponse>> readAll(
            @PathVariable Long workspaceId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(participantService.readAll(workspaceId, memberId).stream()
                .map(ParticipantResponse::from)
                .toList());
    }

    @PatchMapping("/{participantId}/permission")
    public ResponseEntity<Void> changePermission(
            @PathVariable Long workspaceId,
            @PathVariable Long participantId,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ChangePermissionRequest request) {
        participantService.changePermission(request.toCommand(workspaceId, participantId, memberId));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{participantId}/ownership")
    public ResponseEntity<Void> transferOwnership(
            @PathVariable Long workspaceId, @PathVariable Long participantId, @AuthenticationPrincipal Long memberId) {
        participantService.transferOwnership(new TransferOwnershipCommand(workspaceId, participantId, memberId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{participantId}")
    public ResponseEntity<Void> remove(
            @PathVariable Long workspaceId, @PathVariable Long participantId, @AuthenticationPrincipal Long memberId) {
        participantService.remove(new RemoveParticipantCommand(workspaceId, participantId, memberId));
        return ResponseEntity.noContent().build();
    }
}
