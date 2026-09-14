import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router-dom'
import { queryClient } from './app/queryClient'
import { router } from './app/router'
import { useSessionBootstrap } from './app/useSessionBootstrap'

function App() {
  // 새로고침 직후에도 refresh token 쿠키로 세션을 조용히 복구할 때까지 라우터 렌더링을
  // 미룬다(app/useSessionBootstrap.ts 참고) — 안 그러면 RequireAuth가 잠깐 /login으로
  // 리다이렉트했다가 복구되면 다시 돌아오는 깜빡임이 생긴다.
  const isBootstrapping = useSessionBootstrap()

  if (isBootstrapping) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg">
        <p className="text-sm text-text-tertiary">불러오는 중...</p>
      </div>
    )
  }

  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  )
}

export default App
