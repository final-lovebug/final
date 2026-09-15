package com.ubidict.backend.revisionlog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.revisionlog.fixture.RevisionLogFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RevisionLogTest {

    @DisplayName("사전집 개정 이력은 문서 전용 칸을 비워 만든다.")
    @Test
    void forDictionary() {
        // when
        RevisionLog revisionLog = RevisionLogFixture.dictionaryLog().build();

        // then
        assertThat(revisionLog.isDictionary()).isTrue();
        assertThat(revisionLog.getOrigin()).isEqualTo(RevisionOrigin.REVIEW_REVISE);
        assertThat(revisionLog.getBaseDictionaryVersionNo()).isNull();
    }

    @DisplayName("문서 개정 이력은 사전집 전용 칸을 비워 만든다.")
    @Test
    void forDocument() {
        // when
        RevisionLog revisionLog = RevisionLogFixture.documentLog().build();

        // then
        assertThat(revisionLog.isDictionary()).isFalse();
        assertThat(revisionLog.getGrade()).isNull();
        assertThat(revisionLog.getAffectedDocumentCount()).isZero();
        assertThat(revisionLog.getAddedCount()).isZero();
        assertThat(revisionLog.getRemovedCount()).isZero();
    }

    @DisplayName("이전 버전 번호가 없으면 첫 버전이다.")
    @Test
    void isFirstVersion() {
        // given
        RevisionLog firstVersion = RevisionLog.forDocument(
                1L,
                20L,
                1,
                null,
                RevisionOrigin.UPLOAD,
                null,
                0,
                "최초 업로드",
                3L,
                java.time.OffsetDateTime.parse("2026-09-13T12:00:00Z"));

        // when & then
        assertThat(firstVersion.isFirstVersion()).isTrue();
    }
}
