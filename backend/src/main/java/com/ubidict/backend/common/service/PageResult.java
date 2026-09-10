package com.ubidict.backend.common.service;

import java.util.List;
import java.util.function.Function;

/**
 * 페이징 조회 결과. 규격은 {@code docs/API.md} «페이징·정렬 규격»을 따른다.
 *
 * <p>Spring Data {@code Page}를 상위 레이어로 올리지 않기 위한 타입이다. infra가 돌려준 페이지 정보를 저장소를 다루는 implement에서 이 타입으로
 * 옮기고, service는 이것만 반환한다. 쿼리 방식이나 페이징 전략이 바뀌어도 service·presentation이 함께 바뀌지 않는다.
 *
 * <p>{@code totalPages}를 필드로 받지 않고 계산한다 — 호출자가 넘기게 하면 나머지 값과 어긋난 페이지 수가 응답으로 나갈 수 있다.
 *
 * @param content 이 페이지의 항목. 불변 복사본으로 보관한다
 * @param page 0-base 페이지 번호
 * @param size 페이지 크기
 * @param totalElements 전체 항목 수
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

    public PageResult {
        if (content == null) {
            throw new IllegalArgumentException("content는 null일 수 없습니다.");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page는 0 이상이어야 합니다.");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size는 1 이상이어야 합니다.");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements는 0 이상이어야 합니다.");
        }
        content = List.copyOf(content);
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }

    /**
     * 페이지 정보는 그대로 두고 항목만 다른 타입으로 옮긴다. 도메인 모델 → result, result → 응답 DTO 변환에 쓴다.
     */
    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResult<>(content.stream().<R>map(mapper).toList(), page, size, totalElements);
    }
}
