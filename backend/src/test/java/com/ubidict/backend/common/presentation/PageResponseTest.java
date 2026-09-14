package com.ubidict.backend.common.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.common.service.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class PageResponseTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @DisplayName("조회 결과의 페이지 정보를 그대로 옮긴다.")
    @Test
    void from() {
        // given
        PageResult<String> result = new PageResult<>(List.of("가", "나"), 1, 2, 5);

        // when
        PageResponse<String> response = PageResponse.from(result);

        // then
        assertThat(response.content()).containsExactly("가", "나");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @DisplayName("응답 본문의 키는 규격에 고정된 다섯 개뿐이다.")
    @Test
    void serialize() throws Exception {
        // given
        PageResponse<String> response = PageResponse.from(new PageResult<>(List.of("가"), 0, 20, 1));

        // when
        String json = jsonMapper.writeValueAsString(response);

        // then
        assertThat(json).isEqualTo("{\"content\":[\"가\"],\"page\":0,\"size\":20,\"totalElements\":1,\"totalPages\":1}");
    }
}
