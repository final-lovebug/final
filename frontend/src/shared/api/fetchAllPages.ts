import type { Page } from '../types/common'

// docs/API.md "페이징·정렬 규격" — `size`는 최대 100이고 초과하면 400
// (`COMMON_INVALID_REQUEST`)이다. 화면에 페이징 UI가 없는 목록들이 "한 번에 전부"를
// 원해 각 호출부가 제각기 size를 골라 왔고(20·50·100·200·500), 100을 넘긴 것들은 그대로
// 400을 맞고 있었다. 넘지 않은 것들도 상한에 닿으면 조용히 잘렸다.
//
// 그래서 size를 키우는 대신 **상한 크기로 끝까지 페이징**한다. 계약(100)은 그대로 두고
// "전부 가져온다"는 의도를 호출부가 정직하게 표현하게 하는 쪽이다.
export const MAX_PAGE_SIZE = 100

// 폭주 방지 상한. 100 × 50 = 5,000건까지 모은다. 이걸 넘는 목록은 화면에 페이징·검색
// UI가 필요한 것이라 조용히 더 부르지 않고 경고를 남긴다.
const MAX_PAGES = 50

/**
 * 페이지 응답이 다른 객체 안에 들어 있는 엔드포인트용(예: `DictionaryResponse.terms`).
 *
 * 첫 페이지 응답을 그대로 함께 돌려준다 — 감싼 객체의 메타데이터(버전 번호, 발행자 등)가
 * 거기에만 있기 때문이다.
 *
 * 첫 페이지의 `totalPages`로 남은 페이지 수를 알 수 있으므로 2페이지부터는 병렬로 부른다.
 */
export async function fetchAllPagesWith<R, T>(
  loadPage: (page: number, size: number) => Promise<R>,
  selectPage: (response: R) => Page<T>,
): Promise<{ first: R; content: T[] }> {
  const first = await loadPage(0, MAX_PAGE_SIZE)
  const firstPage = selectPage(first)

  if (firstPage.totalPages > MAX_PAGES) {
    console.warn(
      `[fetchAllPages] 페이지가 ${firstPage.totalPages}개라 앞 ${MAX_PAGES}개만 읽는다. ` +
        `전체 ${firstPage.totalElements}건 — 이 화면은 페이징 UI가 필요하다.`,
    )
  }

  const totalPages = Math.min(firstPage.totalPages, MAX_PAGES)
  const restResponses = await Promise.all(
    Array.from({ length: Math.max(0, totalPages - 1) }, (_, index) =>
      loadPage(index + 1, MAX_PAGE_SIZE),
    ),
  )

  return {
    first,
    content: [firstPage.content, ...restResponses.map((response) => selectPage(response).content)].flat(),
  }
}

/** 응답이 곧 페이지인 엔드포인트용. 모든 페이지의 `content`를 이어 붙여 돌려준다. */
export async function fetchAllPages<T>(
  loadPage: (page: number, size: number) => Promise<Page<T>>,
): Promise<T[]> {
  const { content } = await fetchAllPagesWith(loadPage, (page) => page)
  return content
}
