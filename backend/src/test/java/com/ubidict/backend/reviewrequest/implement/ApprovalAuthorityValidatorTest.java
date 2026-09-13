package com.ubidict.backend.reviewrequest.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ApprovalAuthorityValidatorTest {

    @Mock
    private WorkspaceAccessValidator workspaceAccessValidator;

    @DisplayName("REGULAR이 반영을 시도하면 감사 로그를 남기고 예외가 발생한다.")
    @Test
    void validate_regularPermission() {
        // given
        ReviewRequest request = request();
        given(workspaceAccessValidator.validateParticipant(10L, 2L)).willReturn(Permission.REGULAR);
        ApprovalAuthorityValidator validator = new ApprovalAuthorityValidator(workspaceAccessValidator);
        Logger logger = (Logger) LoggerFactory.getLogger(ApprovalAuthorityValidator.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            // when & then
            assertThatThrownBy(() -> validator.validateRevise(request, 2L))
                    .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                            .isEqualTo(ReviewRequestErrorCode.REVIEW_REQUEST_ACCESS_DENIED));
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).startsWith("[ApprovalAuthorityValidator.validateRevise]");
            });
        } finally {
            logger.detachAppender(appender);
        }
    }

    @DisplayName("ADMIN은 요청자가 아니어도 재교정할 수 있다.")
    @Test
    void validateReexamine_adminPermission() {
        // given
        ReviewRequest request = request();
        given(workspaceAccessValidator.validateParticipant(10L, 2L)).willReturn(Permission.ADMIN);
        ApprovalAuthorityValidator validator = new ApprovalAuthorityValidator(workspaceAccessValidator);

        // when & then
        assertThatCode(() -> validator.validateReexamine(request, 2L)).doesNotThrowAnyException();
    }

    private ReviewRequest request() {
        ReviewRequest request = ReviewRequest.create(10L, ReviewRequestType.DOCUMENT, "리뷰", null, 1L, 1L);
        ReflectionTestUtils.setField(request, "id", 3L);
        return request;
    }
}
