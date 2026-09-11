package com.ubidict.backend.workspace.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * 워크스페이스를 읽지 않는 도메인(사전집·문서)이 이 검증에만 올라타므로, 검증기 자체를 대상으로 확인한다.
 */
@ExtendWith(OutputCaptureExtension.class)
class WorkspaceAccessValidatorTest extends IntegrationTestSupport {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;

    @Autowired
    private WorkspaceAccessValidator workspaceAccessValidator;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @DisplayName("참여자는 자신의 권한을 돌려받는다.")
    @Test
    void validateParticipant() {
        // given
        Long workspaceId = createWorkspaceWith(MEMBER_ID, Permission.ADMIN);

        // when
        Permission permission = workspaceAccessValidator.validateParticipant(workspaceId, MEMBER_ID);

        // then
        assertThat(permission).isEqualTo(Permission.ADMIN);
    }

    @DisplayName("참여자가 아니면 권한 부족이 아니라 조회 실패로 응답한다.")
    @Test
    void validateParticipant_memberIsNotParticipant() {
        // given
        Long workspaceId = createWorkspaceWith(MEMBER_ID, Permission.OWNER);

        // when & then
        assertThatThrownBy(() -> workspaceAccessValidator.validateParticipant(workspaceId, OTHER_MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    @DisplayName("워크스페이스가 없으면 조회 실패로 응답한다.")
    @Test
    void validateParticipant_workspaceDoesNotExist() {
        // when & then
        assertThatThrownBy(() -> workspaceAccessValidator.validateParticipant(999L, MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    /**
     * 워크스페이스를 소프트 삭제해도 참여자 행은 남는다. 참여 여부만 보면 삭제된 워크스페이스에 딸린 자원이 새어 나가므로 검증기가 생존까지 확인해야 한다.
     */
    @DisplayName("워크스페이스가 삭제되면 남아 있는 참여자도 접근할 수 없다.")
    @Test
    void validateParticipant_workspaceIsDeleted() {
        // given
        Long workspaceId = createWorkspaceWith(MEMBER_ID, Permission.OWNER);
        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow();
        workspace.delete();
        workspaceRepository.save(workspace);

        // when & then
        assertThat(participantRepository.findByWorkspaceIdAndMemberIdAndDeletedAtIsNull(workspaceId, MEMBER_ID))
                .isPresent();
        assertThatThrownBy(() -> workspaceAccessValidator.validateParticipant(workspaceId, MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    @DisplayName("요구 서열을 만족하면 예외가 발생하지 않는다.")
    @Test
    void validateAtLeast() {
        // given
        Long workspaceId = createWorkspaceWith(MEMBER_ID, Permission.OWNER);

        // when & then
        assertThatCode(() -> workspaceAccessValidator.validateAtLeast(workspaceId, MEMBER_ID, Permission.ADMIN))
                .doesNotThrowAnyException();
    }

    @DisplayName("참여자지만 서열이 모자라면 권한 부족으로 응답한다.")
    @Test
    void validateAtLeast_permissionIsBelowRequired() {
        // given
        Long workspaceId = createWorkspaceWith(MEMBER_ID, Permission.REGULAR);

        // when & then
        assertThatThrownBy(() -> workspaceAccessValidator.validateAtLeast(workspaceId, MEMBER_ID, Permission.ADMIN))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_ADMIN_REQUIRED);
    }

    @DisplayName("서열이 모자라면 감사 로그를 남긴다.")
    @Test
    void validateAtLeast_logsWarnOnInsufficientPermission(CapturedOutput output) {
        Long workspaceId = createWorkspaceWith(MEMBER_ID, Permission.REGULAR);

        assertThatThrownBy(() -> workspaceAccessValidator.validateAtLeast(workspaceId, MEMBER_ID, Permission.ADMIN))
                .isInstanceOf(BusinessException.class);

        assertThat(output)
                .contains("[WorkspaceAccessValidator.validateAtLeast] Insufficient permission")
                .contains("workspaceId=" + workspaceId)
                .contains("memberId=" + MEMBER_ID)
                .contains("required=ADMIN");
    }

    /**
     * 서열 검증보다 참여 여부 검증이 먼저다. 뒤집으면 비참여자에게 403이 나가 워크스페이스의 존재가 드러난다.
     */
    @DisplayName("참여자가 아닌 사용자의 서열 검증은 권한 부족이 아니라 조회 실패로 응답한다.")
    @Test
    void validateAtLeast_memberIsNotParticipant() {
        // given
        Long workspaceId = createWorkspaceWith(MEMBER_ID, Permission.OWNER);

        // when & then
        assertThatThrownBy(
                        () -> workspaceAccessValidator.validateAtLeast(workspaceId, OTHER_MEMBER_ID, Permission.ADMIN))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    private Long createWorkspaceWith(Long memberId, Permission permission) {
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().build());
        participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspace.getId())
                .memberId(memberId)
                .permission(permission)
                .build());

        return workspace.getId();
    }
}
