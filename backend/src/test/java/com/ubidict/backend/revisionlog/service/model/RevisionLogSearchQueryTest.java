package com.ubidict.backend.revisionlog.service.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.CommonErrorCode;
import com.ubidict.backend.revisionlog.domain.RevisionLogTargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RevisionLogSearchQueryTest {

    @DisplayName("정렬을 생략하면 확정 시각 최신순으로 만든다.")
    @Test
    void of_defaultsToPublishedAtDescending() {
        RevisionLogSearchQuery query =
                RevisionLogSearchQuery.of(1L, 2L, RevisionLogTargetType.DICTIONARY, null, null, null, null);

        assertThat(query.sort()).isEqualTo("publishedAt,desc");
        assertThat(query.toPageable().getSort().getOrderFor("publishedAt").getDirection())
                .isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
    }

    @DisplayName("targetType이 없으면 공통 요청 오류를 던진다.")
    @Test
    void of_requiresTargetType() {
        assertThatThrownBy(() -> RevisionLogSearchQuery.of(1L, 2L, null, null, null, null, null))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(CommonErrorCode.COMMON_INVALID_REQUEST));
    }

    @DisplayName("화이트리스트 밖의 정렬 필드는 공통 요청 오류를 던진다.")
    @Test
    void of_rejectsUnsupportedSort() {
        assertThatThrownBy(() -> RevisionLogSearchQuery.of(
                        1L, 2L, RevisionLogTargetType.DOCUMENT, null, 0, 20, "createdAt,desc"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(CommonErrorCode.COMMON_INVALID_REQUEST));
    }
}
