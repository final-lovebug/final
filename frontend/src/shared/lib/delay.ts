// 목업 API 전용 유틸리티. 실제 네트워크 요청처럼 로딩 상태를 화면에서 확인할 수 있게
// 인위적으로 지연시킨다. 실제 백엔드 호출로 교체되면 더 이상 필요 없다.
export function delay(ms = 400): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
