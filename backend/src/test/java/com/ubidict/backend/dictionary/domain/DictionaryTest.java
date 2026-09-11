package com.ubidict.backend.dictionary.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DictionaryTest {

    private static final Long WORKSPACE_ID = 1L;
    private static final Long CREATED_BY = 10L;
    private static final Long OTHER_MEMBER_ID = 20L;

    @DisplayName("첫 사전집을 만들면 버전 1의 활성 사전집이 된다.")
    @Test
    void createFirst() {
        // when
        Dictionary dictionary = Dictionary.createFirst(WORKSPACE_ID, CREATED_BY);

        // then
        assertThat(dictionary.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(dictionary.getCreatedBy()).isEqualTo(CREATED_BY);
        assertThat(dictionary.versionNo()).isEqualTo(1);
        assertThat(dictionary.getStatus()).isEqualTo(DictionaryStatus.ACTIVE);
        assertThat(dictionary.getVersion().publishedAt()).isNotNull();
    }

    @DisplayName("다음 버전을 만들면 버전 번호가 1 오른다.")
    @Test
    void nextVersion() {
        // given
        Dictionary previous = Dictionary.createFirst(WORKSPACE_ID, CREATED_BY);

        // when
        Dictionary next = Dictionary.nextVersion(previous, OTHER_MEMBER_ID);

        // then
        assertThat(next.versionNo()).isEqualTo(2);
        assertThat(next.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(next.getCreatedBy()).isEqualTo(OTHER_MEMBER_ID);
        assertThat(next.getStatus()).isEqualTo(DictionaryStatus.ACTIVE);
    }

    /**
     * 확정된 버전이 나중에 바뀌면 이력이 거짓말이 된다. 보관 처리는 호출자가 archive()로 따로 한다.
     */
    @DisplayName("다음 버전을 만들어도 이전 버전은 그대로다.")
    @Test
    void nextVersion_doesNotTouchPrevious() {
        // given
        Dictionary previous = Dictionary.createFirst(WORKSPACE_ID, CREATED_BY);

        // when
        Dictionary.nextVersion(previous, OTHER_MEMBER_ID);

        // then
        assertThat(previous.versionNo()).isEqualTo(1);
        assertThat(previous.getStatus()).isEqualTo(DictionaryStatus.ACTIVE);
    }

    @DisplayName("보관하면 지나간 버전이 된다.")
    @Test
    void archive() {
        // given
        Dictionary dictionary = Dictionary.createFirst(WORKSPACE_ID, CREATED_BY);

        // when
        dictionary.archive();

        // then
        assertThat(dictionary.getStatus()).isEqualTo(DictionaryStatus.ARCHIVED);
        assertThat(dictionary.isActive()).isFalse();
    }

    @DisplayName("이미 보관된 사전집을 다시 보관해도 상태가 바뀌지 않는다.")
    @Test
    void archive_isIdempotent() {
        // given
        Dictionary dictionary = Dictionary.createFirst(WORKSPACE_ID, CREATED_BY);
        dictionary.archive();

        // when
        dictionary.archive();

        // then
        assertThat(dictionary.getStatus()).isEqualTo(DictionaryStatus.ARCHIVED);
    }

    /**
     * 사전집은 삭제하지 않는다. 모든 행이 보존해야 할 버전 이력이다.
     */
    @DisplayName("확정일시는 버전 값 객체에 위임한다.")
    @Test
    void publishedAt_delegatesToVersion() {
        // when
        Dictionary dictionary = Dictionary.createFirst(WORKSPACE_ID, CREATED_BY);

        // then
        assertThat(dictionary.publishedAt()).isEqualTo(dictionary.getVersion().publishedAt());
    }
}
