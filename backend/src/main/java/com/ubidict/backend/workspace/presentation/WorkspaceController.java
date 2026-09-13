package com.ubidict.backend.workspace.presentation;

import com.ubidict.backend.workspace.presentation.dto.CreateWorkspaceRequest;
import com.ubidict.backend.workspace.presentation.dto.UpdateRuleSetRequest;
import com.ubidict.backend.workspace.presentation.dto.UpdateWorkspaceRequest;
import com.ubidict.backend.workspace.presentation.dto.WorkspaceResponse;
import com.ubidict.backend.workspace.service.WorkspaceService;
import com.ubidict.backend.workspace.service.model.WorkspaceResult;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<WorkspaceResponse> create(
            @AuthenticationPrincipal Long memberId, @Valid @RequestBody CreateWorkspaceRequest request) {
        WorkspaceResult result = workspaceService.create(request.toCommand(memberId));

        return ResponseEntity.status(HttpStatus.CREATED).body(WorkspaceResponse.from(result));
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> readMine(@AuthenticationPrincipal Long memberId) {
        List<WorkspaceResponse> responses = workspaceService.readMine(memberId).stream()
                .map(WorkspaceResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{workspaceId}")
    public ResponseEntity<WorkspaceResponse> read(
            @PathVariable Long workspaceId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(WorkspaceResponse.from(workspaceService.read(workspaceId, memberId)));
    }

    @PatchMapping("/{workspaceId}")
    public ResponseEntity<Void> rename(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody UpdateWorkspaceRequest request) {
        workspaceService.rename(request.toCommand(workspaceId, memberId));

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<Void> delete(@PathVariable Long workspaceId, @AuthenticationPrincipal Long memberId) {
        workspaceService.delete(workspaceId, memberId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{workspaceId}/rule-set")
    public ResponseEntity<WorkspaceResponse> changeRuleSet(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody UpdateRuleSetRequest request) {
        return ResponseEntity.ok(
                WorkspaceResponse.from(workspaceService.changeRuleSet(request.toCommand(workspaceId, memberId))));
    }
}
