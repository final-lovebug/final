package com.ubidict.backend.draftdictionary.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.domain.ExtractionJobStatus;
import com.ubidict.backend.support.RepositoryTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ExtractionJobRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private ExtractionJobRepository extractionJobRepository;

    @DisplayName("대기 중인 용어 추출 작업과 대상 문서를 저장하고 조회한다.")
    @Test
    void save() {
        ExtractionJob saved = extractionJobRepository.save(ExtractionJob.create(1L, null, List.of(10L, 20L), 2L));
        em.flush();
        em.clear();

        assertThat(extractionJobRepository.findByIdAndDeletedAtIsNull(saved.getId()))
                .get()
                .satisfies(job -> {
                    assertThat(job.getStatus()).isEqualTo(ExtractionJobStatus.PENDING);
                    assertThat(job.getSourceDocumentIds()).containsExactlyInAnyOrder(10L, 20L);
                });
    }

    @DisplayName("워크스페이스에 진행 중인 용어 추출 작업이 있는지 확인한다.")
    @Test
    void existsInProgress() {
        extractionJobRepository.save(ExtractionJob.create(1L, null, List.of(10L), 2L));

        assertThat(extractionJobRepository.existsByWorkspaceIdAndStatusInAndDeletedAtIsNull(
                        1L, List.of(ExtractionJobStatus.PENDING, ExtractionJobStatus.RUNNING)))
                .isTrue();
    }
}
