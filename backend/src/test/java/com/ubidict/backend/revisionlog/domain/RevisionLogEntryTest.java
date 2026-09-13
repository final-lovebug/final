package com.ubidict.backend.revisionlog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RevisionLogEntryTest {

    @DisplayName("용어 변경 항목은 영문명과 상세를 담는다.")
    @Test
    void term() {
        // when
        RevisionLogEntry entry =
                RevisionLogEntry.term(1L, RevisionLogChangeType.CHANGED, "구독", "Subscription", "정의 수정");

        // then
        assertThat(entry.getSubjectEnglishName()).isEqualTo("Subscription");
        assertThat(entry.getReplacement()).isNull();
        assertThat(entry.getDetail()).isEqualTo("정의 수정");
    }

    @DisplayName("문서 치환 항목은 변경 전후 표기를 담는다.")
    @Test
    void replacement() {
        // when
        RevisionLogEntry entry = RevisionLogEntry.replacement(1L, "유저", "사용자");

        // then
        assertThat(entry.getChangeType()).isEqualTo(RevisionLogChangeType.CHANGED);
        assertThat(entry.getSubject()).isEqualTo("유저");
        assertThat(entry.getReplacement()).isEqualTo("사용자");
        assertThat(entry.getSubjectEnglishName()).isNull();
        assertThat(entry.getDetail()).isNull();
    }
}
