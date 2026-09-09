package com.ubidict.backend.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WorkspaceServiceTest extends IntegrationTestSupport {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private ParticipantRepository participantRepository;

    @DisplayName("워크스페이스를 만들면 생성자가 OWNER 참여자로 함께 등록된다.")
    @Test
    void create() {
        // when
        WorkspaceIdResult result = workspaceService.create(new CreateWorkspaceCommand("개발팀", OWNER_ID));

        // then
        Participant owner = participantRepository
                .findByWorkspaceIdAndMemberIdAndDeletedAtIsNull(result.workspaceId(), OWNER_ID)
                .orElseThrow();
        assertThat(owner.getPermission()).isEqualTo(Permission.OWNER);
    }

    @DisplayName("워크스페이스를 만들면 룰셋이 기본값 0으로 저장된다.")
    @Test
    void create_ruleSetIsInitial() {
        // given
        WorkspaceIdResult created = workspaceService.create(new CreateWorkspaceCommand("개발팀", OWNER_ID));

        // when
        WorkspaceResult result = workspaceService.read(created.workspaceId(), OWNER_ID);

        // then
        assertThat(result.requiredDocumentReviewerCount()).isZero();
        assertThat(result.requiredDictionaryReviewerCount()).isZero();
    }

    @DisplayName("내가 참여한 워크스페이스만 목록에 나온다.")
    @Test
    void readMine() {
        // given
        WorkspaceIdResult mine = workspaceService.create(new CreateWorkspaceCommand("개발팀", OWNER_ID));
        workspaceService.create(new CreateWorkspaceCommand("남의팀", OTHER_MEMBER_ID));

        // when
        List<WorkspaceSummaryResult> results = workspaceService.readMine(OWNER_ID);

        // then
        assertThat(results).extracting(WorkspaceSummaryResult::workspaceId).containsExactly(mine.workspaceId());
        assertThat(results).extracting(WorkspaceSummaryResult::myPermission).containsExactly(Permission.OWNER);
    }

    @DisplayName("참여자가 아니면 상세를 조회할 수 없다.")
    @Test
    void read_memberIsNotParticipant() {
        // given
        WorkspaceIdResult created = workspaceService.create(new CreateWorkspaceCommand("개발팀", OWNER_ID));

        // when & then
        assertThatThrownBy(() -> workspaceService.read(created.workspaceId(), OTHER_MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    @DisplayName("삭제된 워크스페이스는 상세를 조회할 수 없다.")
    @Test
    void read_workspaceIsDeleted() {
        // given
        WorkspaceIdResult created = workspaceService.create(new CreateWorkspaceCommand("개발팀", OWNER_ID));
        workspaceService.delete(created.workspaceId(), OWNER_ID);

        // when & then
        assertThatThrownBy(() -> workspaceService.read(created.workspaceId(), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    @DisplayName("ADMIN은 워크스페이스 이름을 바꿀 수 있다.")
    @Test
    void rename() {
        // given
        WorkspaceIdResult created = workspaceService.create(new CreateWorkspaceCommand("개발팀", OWNER_ID));
        joinAs(created.workspaceId(), OTHER_MEMBER_ID, Permission.ADMIN);

        // when
        workspaceService.rename(new RenameWorkspaceCommand(created.workspaceId(), "플랫폼팀", OTHER_MEMBER_ID));

        // then
        assertThat(workspaceService.read(created.workspaceId(), OWNER_ID).name())
                .isEqualTo("플랫폼팀");
    }

    @DisplayName("REGULAR는 워크스페이스 이름을 바꿀 수 없다.")
    @Test
    void rename_permissionIsBelowAdmin() {
        // given
        WorkspaceIdResult created = workspaceService.create(new CreateWorkspaceCommand("개발팀", OWNER_ID));
        joinAs(created.workspaceId(), OTHER_MEMBER_ID, Permission.REGULAR);
        RenameWorkspaceCommand command = new RenameWorkspaceCommand(created.workspaceId(), "플랫폼팀", OTHER_MEMBER_ID);

        // when & then
        assertThatThrownBy(() -> workspaceService.rename(command))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_ADMIN_REQUIRED);
    }

    @DisplayName("참여자가 아닌 사용자의 이름 변경은 권한 부족이 아니라 조회 실패로 응답한다.")
    @Test
    void rename_memberIsNotParticipant() {
        // given
        WorkspaceIdResult created = workspaceService.create(new CreateWorkspaceCommand("개발팀", OWNER_ID));
        RenameWorkspaceCommand command = new RenameWorkspaceCommand(created.workspaceId(), "플랫폼팀", OTHER_MEMBER_ID);

        // when & then
        assertThatThrownBy(() -> workspaceService.rename(command))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    @DisplayName("OWNER가 아니면 워크스페이스를 삭제할 수 없다.")
    @Test
    void delete_memberIsNotOwner() {
        // given
        WorkspaceIdResult created = workspaceService.create(new CreateWorkspaceCommand("개발팀", OWNER_ID));
        joinAs(created.workspaceId(), OTHER_MEMBER_ID, Permission.ADMIN);

        // when & then
        assertThatThrownBy(() -> workspaceService.delete(created.workspaceId(), OTHER_MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_OWNER_REQUIRED);
    }

    @DisplayName("워크스페이스를 삭제하면 목록에서 사라진다.")
    @Test
    void delete() {
        // given
        WorkspaceIdResult created = workspaceService.create(new CreateWorkspaceCommand("개발팀", OWNER_ID));

        // when
        workspaceService.delete(created.workspaceId(), OWNER_ID);

        // then
        assertThat(workspaceService.readMine(OWNER_ID)).isEmpty();
    }

    private void joinAs(Long workspaceId, Long memberId, Permission permission) {
        participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(memberId)
                .permission(permission)
                .build());
    }
}
