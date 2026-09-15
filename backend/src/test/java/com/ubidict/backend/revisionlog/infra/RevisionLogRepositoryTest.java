package com.ubidict.backend.revisionlog.infra;

import static com.ubidict.backend.revisionlog.fixture.RevisionLogFixture.dictionaryLog;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.revisionlog.domain.RevisionLog;
import com.ubidict.backend.revisionlog.domain.RevisionLogTargetType;
import com.ubidict.backend.support.RepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class RevisionLogRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private RevisionLogRepository revisionLogRepository;

    @DisplayName("같은 대상 버전의 개정 이력을 두 번 저장하면 거부된다.")
    @Test
    void targetVersionIsUnique() {
        // given
        revisionLogRepository.saveAndFlush(dictionaryLog().build());

        // when & then
        assertThatThrownBy(
                        () -> revisionLogRepository.saveAndFlush(dictionaryLog().build()))
                .isInstanceOf(Exception.class);
    }

    @DisplayName("대상 축과 대상별로 최신 확정 시각 순으로 개정 이력을 조회한다.")
    @Test
    void findTimeline() {
        // given
        RevisionLog first = RevisionLog.forDictionary(
                1L,
                10L,
                1,
                null,
                com.ubidict.backend.revisionlog.domain.RevisionLogGrade.INITIAL,
                0,
                0,
                0,
                0,
                "최초 발행",
                3L,
                java.time.OffsetDateTime.parse("2026-09-13T10:00:00Z"));
        revisionLogRepository.save(first);
        revisionLogRepository.save(dictionaryLog().build());
        revisionLogRepository.save(dictionaryLog().dictionaryId(11L).build());
        revisionLogRepository.flush();

        // when
        var page = revisionLogRepository.findAllByWorkspaceIdAndTargetTypeAndTargetId(
                1L,
                RevisionLogTargetType.DICTIONARY,
                10L,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "publishedAt")));

        // then
        assertThat(page.getContent()).extracting(RevisionLog::getVersionNo).containsExactly(2, 1);
    }
}
