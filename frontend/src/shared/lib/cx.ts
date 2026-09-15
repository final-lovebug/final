// 아주 작은 className 결합 유틸리티. clsx 등 신규 라이브러리를 추가하기 전에는
// 이 정도 로직만으로 충분해서 직접 작성했다 (CLAUDE.md: 신규 의존성은 사전 승인 필요).
export function cx(
  ...classes: Array<string | false | null | undefined>
): string {
  return classes.filter(Boolean).join(' ')
}
