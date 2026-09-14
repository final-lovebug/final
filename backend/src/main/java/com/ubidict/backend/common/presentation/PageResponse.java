package com.ubidict.backend.common.presentation;

import com.ubidict.backend.common.service.PageResult;
import java.util.List;

/**
 * 페이징 응답. 키 이름 다섯 개는 {@code docs/API.md} «페이징·정렬 규격»에 고정돼 있으므로 record 컴포넌트 이름을 바꾸면 API 계약이 깨진다.
 *
 * <p>Spring Data {@code Page}를 그대로 직렬화하지 않는 이유 — {@code pageable}·{@code first}·{@code last}·
 * {@code numberOfElements} 같은 필드가 버전에 따라 바뀌고, 클라이언트가 그것에 의존하게 된다.
 *
 * <p>불변식은 {@link PageResult}가 갖는다. 이 타입은 그것을 직렬화 형태로 옮기는 DTO다.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(PageResult<T> result) {
        return new PageResponse<>(
                result.content(), result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
