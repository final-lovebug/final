package com.ubidict.backend.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PageResultTest {

    @DisplayName("전체 항목 수가 페이지 크기로 나누어떨어지면 전체 페이지 수는 그 몫이다.")
    @Test
    void totalPages_divisible() {
        // given
        PageResult<String> result = new PageResult<>(List.of("가", "나"), 0, 2, 4);

        // when & then
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @DisplayName("나누어떨어지지 않으면 남은 항목을 담는 페이지 하나를 더한다.")
    @Test
    void totalPages_notDivisible() {
        // given
        PageResult<String> result = new PageResult<>(List.of("가"), 2, 2, 5);

        // when & then
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @DisplayName("전체 항목이 없으면 전체 페이지 수는 0이다.")
    @Test
    void totalPages_empty() {
        // given
        PageResult<String> result = new PageResult<>(List.of(), 0, 20, 0);

        // when & then
        assertThat(result.totalPages()).isZero();
    }

    @DisplayName("항목을 다른 타입으로 옮겨도 페이지 정보는 그대로다.")
    @Test
    void map() {
        // given
        PageResult<String> result = new PageResult<>(List.of("가", "나"), 1, 2, 5);

        // when
        PageResult<Integer> mapped = result.map(String::length);

        // then
        assertThat(mapped.content()).containsExactly(1, 1);
        assertThat(mapped.page()).isEqualTo(1);
        assertThat(mapped.size()).isEqualTo(2);
        assertThat(mapped.totalElements()).isEqualTo(5);
        assertThat(mapped.totalPages()).isEqualTo(3);
    }

    @DisplayName("생성 뒤 원본 리스트를 바꿔도 결과의 항목은 바뀌지 않는다.")
    @Test
    void content_isDefensiveCopy() {
        // given
        List<String> source = new ArrayList<>(List.of("가"));
        PageResult<String> result = new PageResult<>(source, 0, 20, 1);

        // when
        source.add("나");

        // then
        assertThat(result.content()).containsExactly("가");
    }

    @DisplayName("항목이 null이면 예외가 발생한다.")
    @Test
    void create_contentIsNull() {
        // when & then
        assertThatThrownBy(() -> new PageResult<>(null, 0, 20, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("content는 null일 수 없습니다.");
    }

    @DisplayName("페이지 번호가 음수면 예외가 발생한다.")
    @Test
    void create_pageIsNegative() {
        // when & then
        assertThatThrownBy(() -> new PageResult<>(List.of(), -1, 20, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("page는 0 이상이어야 합니다.");
    }

    @DisplayName("페이지 크기가 1보다 작으면 예외가 발생한다.")
    @Test
    void create_sizeIsNotPositive() {
        // when & then
        assertThatThrownBy(() -> new PageResult<>(List.of(), 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("size는 1 이상이어야 합니다.");
    }

    @DisplayName("전체 항목 수가 음수면 예외가 발생한다.")
    @Test
    void create_totalElementsIsNegative() {
        // when & then
        assertThatThrownBy(() -> new PageResult<>(List.of(), 0, 20, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("totalElements는 0 이상이어야 합니다.");
    }
}
