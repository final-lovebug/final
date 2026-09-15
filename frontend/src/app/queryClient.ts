import { QueryClient } from '@tanstack/react-query'

// 지금은 전부 목데이터라 서버 상태가 바뀌지 않으므로 불필요한 재요청을 막아둔다.
// 실제 API로 교체되면(frontend/docs/SPEC.md "백엔드 연동 범위" 참고) 화면 특성에 맞게 다시 조정한다.
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60_000,
      retry: false,
    },
  },
})
