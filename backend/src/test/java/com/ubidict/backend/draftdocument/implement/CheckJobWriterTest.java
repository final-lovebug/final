package com.ubidict.backend.draftdocument.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.CheckJobRepository;
import com.ubidict.backend.support.RepositoryTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 동시 요청이 사전 검사를 함께 통과했을 때 무엇이 나가는지 본다.
 *
 * <p>제약 위반이 그대로 새어 나가면 {@code DataIntegrityViolationException} → 500이 된다. 사용자가 보는 것은 순차 요청과 같은 409여야
 * 한다 — 실제로 일어난 일이 「이미 진행 중인 작업이 있다」로 같기 때문이다.
 */
class CheckJobWriterTest extends RepositoryTestSupport {

    @Autowired
    private CheckJobRepository checkJobRepository;

    private CheckJobWriter writer;

    @BeforeEach
    void setUpWriter() {
        writer = new CheckJobWriter(checkJobRepository);
    }

    @DisplayName("진행 중인 작업이 이미 있으면 제약 위반을 409로 바꿔 던진다.")
    @Test
    void append_inProgressJobExists() {
        checkJobRepository.saveAndFlush(CheckJob.create(10L, 20L));

        assertThatThrownBy(() -> writer.append(10L, 21L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_ALREADY_RUNNING);
    }

    @DisplayName("진행 중인 작업이 없으면 그대로 저장한다.")
    @Test
    void append() {
        CheckJob appended = writer.append(10L, 20L);

        assertThat(appended.getId()).isNotNull();
        assertThat(appended.getDocumentId()).isEqualTo(10L);
    }
}
