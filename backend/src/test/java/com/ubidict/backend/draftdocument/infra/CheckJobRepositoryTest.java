package com.ubidict.backend.draftdocument.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import com.ubidict.backend.support.RepositoryTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CheckJobRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private CheckJobRepository checkJobRepository;

    @DisplayName("대기 중인 문서 대조 작업을 저장하고 조회한다.")
    @Test
    void save() {
        CheckJob saved = checkJobRepository.save(CheckJob.create(10L, 20L));
        em.flush();
        em.clear();

        assertThat(checkJobRepository.findByIdAndDeletedAtIsNull(saved.getId()))
                .get()
                .extracting(CheckJob::getStatus, CheckJob::getDocumentId)
                .containsExactly(CheckJobStatus.PENDING, 10L);
    }

    @DisplayName("문서에 대기 또는 실행 중인 대조 작업이 있는지 확인한다.")
    @Test
    void existsInProgress() {
        checkJobRepository.save(CheckJob.create(10L, 20L));

        assertThat(checkJobRepository.existsByDocumentIdAndStatusInAndDeletedAtIsNull(
                        10L, List.of(CheckJobStatus.PENDING, CheckJobStatus.RUNNING)))
                .isTrue();
    }
}
