package com.ubidict.backend.draftdocument.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CheckJobTest {

    @DisplayName("문서 대조 작업을 만들면 대기 상태로 시작한다.")
    @Test
    void create() {
        CheckJob checkJob = CheckJob.create(10L, 20L);

        assertThat(checkJob.getStatus()).isEqualTo(CheckJobStatus.PENDING);
        assertThat(checkJob.getDocumentId()).isEqualTo(10L);
        assertThat(checkJob.getRequestedBy()).isEqualTo(20L);
    }

    @DisplayName("실행 중인 대조 작업을 완료하면 결과 초안 식별자를 기록한다.")
    @Test
    void succeed() {
        CheckJob checkJob = CheckJob.create(10L, 20L);
        checkJob.start();

        checkJob.succeed(30L);

        assertThat(checkJob.getStatus()).isEqualTo(CheckJobStatus.SUCCEEDED);
        assertThat(checkJob.getDraftDocumentId()).isEqualTo(30L);
    }

    @DisplayName("완료된 대조 작업은 실패 상태로 바꿀 수 없다.")
    @Test
    void fail_afterSucceeded() {
        CheckJob checkJob = CheckJob.create(10L, 20L);
        checkJob.start();
        checkJob.succeed(30L);

        assertThatThrownBy(() -> checkJob.fail("실패"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_STATUS));
    }
}
