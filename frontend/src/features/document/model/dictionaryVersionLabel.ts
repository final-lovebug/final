// 「적용 사전집」 칸에 찍는 표기. 문서 버전(`currentVersionNo`)과 혼동하기 쉬워 한곳에 모은다 —
// 목록 화면이 실제로 문서 버전을 이 자리에 찍어 업로드 직후 문서가 `r1`로 보이는 버그가 있었다.
//
// 업로드본은 대조를 거치지 않아 기준 사전집 버전이 없고(`D-93`), 워크스페이스에 활성 사전집이
// 있든 없든 `dictionaryVersionNo`는 `null`이다. 그 자리는 번호 대신 "—"다.

export function dictionaryVersionLabel(dictionaryVersionNo?: number | null): string {
  return dictionaryVersionNo == null ? '—' : `r${dictionaryVersionNo}`
}
