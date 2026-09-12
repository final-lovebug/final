package com.ubidict.backend.workspace.implement;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.workspace.domain.RuleSet;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RuleSetValidatorTest {
    private final ParticipantRepository repository = Mockito.mock(ParticipantRepository.class);
    private final RuleSetValidator validator = new RuleSetValidator(repository);
    private final Workspace workspace = WorkspaceFixture.workspace().id(1L).build();

    @DisplayName("필수 리뷰어 수가 참여자 수를 넘으면 예외가 발생한다.")
    @Test
    void validate_exceedsParticipantCount() {
        given(repository.countByWorkspaceIdAndDeletedAtIsNull(1L)).willReturn(2L);
        assertThatThrownBy(() -> validator.validate(workspace, new RuleSet(3, 0)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_REVIEWER_COUNT_EXCEEDS_PARTICIPANTS);
    }

    @DisplayName("필수 리뷰어 수가 참여자 수와 같으면 허용한다.")
    @Test
    void validate_equalsParticipantCount() {
        given(repository.countByWorkspaceIdAndDeletedAtIsNull(1L)).willReturn(2L);

        assertThatCode(() -> validator.validate(workspace, new RuleSet(2, 2))).doesNotThrowAnyException();
    }
}
