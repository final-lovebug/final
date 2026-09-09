package com.ubidict.backend.workspace.presentation;

import com.ubidict.backend.workspace.service.CreateWorkspaceCommand;
import com.ubidict.backend.workspace.service.RenameWorkspaceCommand;
import com.ubidict.backend.workspace.service.WorkspaceIdResult;
import com.ubidict.backend.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 요청자 memberId를 요청 파라미터로 받는다. 인증 계층이 아직 없어 생긴 임시 방식이며 인증 도입 전까지 운영 배포 대상이 아니다.
 *
 * <p>TODO(NFR-USR-001): 인증이 들어오면 memberId 파라미터를 걷어내고 인증 주체에서 해석한다. 바꿀 지점은 이 클래스의 파라미터 5곳뿐이고 service 이하는
 * 손대지 않는다.
 */
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public WorkspaceIdResponse create(@RequestParam Long memberId, @Valid @RequestBody CreateWorkspaceRequest request) {
        WorkspaceIdResult result = workspaceService.create(new CreateWorkspaceCommand(request.name(), memberId));

        return WorkspaceIdResponse.from(result);
    }

    @GetMapping
    public List<WorkspaceSummaryResponse> readMine(@RequestParam Long memberId) {
        return workspaceService.readMine(memberId).stream()
                .map(WorkspaceSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceResponse read(@PathVariable Long workspaceId, @RequestParam Long memberId) {
        return WorkspaceResponse.from(workspaceService.read(workspaceId, memberId));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{workspaceId}")
    public void rename(
            @PathVariable Long workspaceId,
            @RequestParam Long memberId,
            @Valid @RequestBody UpdateWorkspaceRequest request) {
        workspaceService.rename(new RenameWorkspaceCommand(workspaceId, request.name(), memberId));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{workspaceId}")
    public void delete(@PathVariable Long workspaceId, @RequestParam Long memberId) {
        workspaceService.delete(workspaceId, memberId);
    }
}
