package com.ubidict.backend.workspace.presentation;

import com.ubidict.backend.workspace.service.CreateWorkspaceCommand;
import com.ubidict.backend.workspace.service.RenameWorkspaceCommand;
import com.ubidict.backend.workspace.service.WorkspaceIdResult;
import com.ubidict.backend.workspace.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Workspace", description = "워크스페이스 생성·조회·수정·삭제")
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @Operation(summary = "워크스페이스 생성", description = "생성자를 OWNER 참여자로 함께 등록한다.")
    @ApiResponse(responseCode = "201", description = "생성 성공")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public WorkspaceIdResponse create(
            @Parameter(description = "요청자 회원 식별자(인증 도입 전 임시)") @RequestParam Long memberId,
            @Valid @RequestBody CreateWorkspaceRequest request) {
        WorkspaceIdResult result = workspaceService.create(new CreateWorkspaceCommand(request.name(), memberId));

        return WorkspaceIdResponse.from(result);
    }

    @Operation(summary = "참여 중인 워크스페이스 목록 조회")
    @GetMapping
    public List<WorkspaceSummaryResponse> readMine(
            @Parameter(description = "요청자 회원 식별자(인증 도입 전 임시)") @RequestParam Long memberId) {
        return workspaceService.readMine(memberId).stream()
                .map(WorkspaceSummaryResponse::from)
                .toList();
    }

    @Operation(summary = "워크스페이스 상세 조회", description = "참여자만 조회할 수 있다.")
    @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", description = "없거나 참여자가 아님")})
    @GetMapping("/{workspaceId}")
    public WorkspaceResponse read(
            @PathVariable Long workspaceId,
            @Parameter(description = "요청자 회원 식별자(인증 도입 전 임시)") @RequestParam Long memberId) {
        return WorkspaceResponse.from(workspaceService.read(workspaceId, memberId));
    }

    @Operation(summary = "워크스페이스 이름 수정", description = "ADMIN 이상만 수정할 수 있다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "수정 성공"),
        @ApiResponse(responseCode = "403", description = "ADMIN 미만"),
        @ApiResponse(responseCode = "404", description = "없거나 참여자가 아님")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{workspaceId}")
    public void rename(
            @PathVariable Long workspaceId,
            @Parameter(description = "요청자 회원 식별자(인증 도입 전 임시)") @RequestParam Long memberId,
            @Valid @RequestBody UpdateWorkspaceRequest request) {
        workspaceService.rename(new RenameWorkspaceCommand(workspaceId, request.name(), memberId));
    }

    @Operation(summary = "워크스페이스 삭제", description = "OWNER만 삭제할 수 있다. 소프트 삭제다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "403", description = "OWNER 아님"),
        @ApiResponse(responseCode = "404", description = "없거나 참여자가 아님")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{workspaceId}")
    public void delete(
            @PathVariable Long workspaceId,
            @Parameter(description = "요청자 회원 식별자(인증 도입 전 임시)") @RequestParam Long memberId) {
        workspaceService.delete(workspaceId, memberId);
    }
}
