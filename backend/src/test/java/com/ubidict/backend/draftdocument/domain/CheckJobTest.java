package com.ubidict.backend.draftdocument.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CheckJobTest {

    private static final String REQUEST_ID = "0d5c6f6e-0000-4000-8000-000000000001";

    @DisplayName("문서 대조 작업을 만들면 대기 상태로 시작한다.")
    @Test
    void create() {
        CheckJob checkJob = CheckJob.create(10L, 20L);

        assertThat(checkJob.getStatus()).isEqualTo(CheckJobStatus.PENDING);
        assertThat(checkJob.getDocumentId()).isEqualTo(10L);
        assertThat(checkJob.getRequestedBy()).isEqualTo(20L);
        assertThat(checkJob.getRequestId()).isNull();
    }

    @DisplayName("워커에게 넘기면 상관 식별자를 새기고 실행 중 상태가 된다.")
    @Test
    void markDispatching() {
        CheckJob checkJob = CheckJob.create(10L, 20L);

        checkJob.markDispatching(REQUEST_ID);

        assertThat(checkJob.getStatus()).isEqualTo(CheckJobStatus.RUNNING);
        assertThat(checkJob.getRequestId()).isEqualTo(REQUEST_ID);
    }

    @DisplayName("같은 상관 식별자로 다시 넘겨도 상태가 달라지지 않는다.")
    @Test
    void markDispatching_idempotent() {
        CheckJob checkJob = CheckJob.create(10L, 20L);
        checkJob.markDispatching(REQUEST_ID);

        checkJob.markDispatching(REQUEST_ID);

        assertThat(checkJob.getStatus()).isEqualTo(CheckJobStatus.RUNNING);
    }

    @DisplayName("콜백이 들고 온 상관 식별자가 다르면 이 작업의 것으로 보지 않는다.")
    @Test
    void matchesRequestId() {
        CheckJob checkJob = CheckJob.create(10L, 20L);
        assertThat(checkJob.matchesRequestId(REQUEST_ID)).isFalse();

        checkJob.markDispatching(REQUEST_ID);

        assertThat(checkJob.matchesRequestId(REQUEST_ID)).isTrue();
        assertThat(checkJob.matchesRequestId("0d5c6f6e-0000-4000-8000-000000000002"))
                .isFalse();
        assertThat(checkJob.matchesRequestId(null)).isFalse();
    }

    @DisplayName("실행 중인 대조 작업을 완료하면 결과 초안 식별자를 기록한다.")
    @Test
    void succeed() {
        CheckJob checkJob = CheckJob.create(10L, 20L);
        checkJob.markDispatching(REQUEST_ID);

        checkJob.succeed(30L);

        assertThat(checkJob.getStatus()).isEqualTo(CheckJobStatus.SUCCEEDED);
        assertThat(checkJob.getDraftDocumentId()).isEqualTo(30L);
    }

    @DisplayName("완료된 대조 작업은 실패 상태로 바꿀 수 없다.")
    @Test
    void fail_afterSucceeded() {
        CheckJob checkJob = CheckJob.create(10L, 20L);
        checkJob.markDispatching(REQUEST_ID);
        checkJob.succeed(30L);

        assertThatThrownBy(() -> checkJob.fail("실패"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_STATUS));
    }
}
