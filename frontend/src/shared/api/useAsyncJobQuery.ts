import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  isJobPollingExhausted,
  jobRefetchInterval,
  type AsyncJobStatus,
} from './jobPolling'

// 비동기 작업(용어 추출 DI-5 / 문서 대조 DD-5) 폴링 훅. 두 도메인이 상태 축과 폴링
// 규격을 공유하므로(shared/api/jobPolling.ts) 훅도 한 벌만 둔다 — 도메인 지식은
// queryFn(각 feature의 api 함수)에만 있고 여기에는 없다.

interface AsyncJob {
  status: AsyncJobStatus
}

/**
 * 캐시에 담기는 형태. 작업 상태에 **조회 횟수를 함께 싣는다** — 폴링 상한과 비교하려면
 * 세야 하는데 `useQuery` 결과에는 `dataUpdateCount`가 없고(refetchInterval 콜백의
 * `query.state`에만 있다), 렌더 중에 `Date.now()`나 ref를 읽는 것은 순수하지 않다.
 * 캐시 키에 jobId가 들어 있으므로 새 작업이면 횟수도 0부터 다시 센다.
 */
interface PolledJob<T extends AsyncJob> {
  job: T
  attempts: number
}

interface UseAsyncJobQueryOptions<T extends AsyncJob> {
  /** 작업 식별자. null이면 아직 접수 전이라 폴링하지 않는다. */
  jobId: string | null
  queryKey: readonly unknown[]
  queryFn: (jobId: string) => Promise<T>
}

export interface AsyncJobQueryResult<T extends AsyncJob> {
  job: T | undefined
  isLoading: boolean
  error: unknown
  /** 상한까지 지켜봤지만 끝나지 않았다 — 실패가 아니라 "그만 지켜봄"이다. */
  isPollingExhausted: boolean
}

export function useAsyncJobQuery<T extends AsyncJob>({
  jobId,
  queryKey,
  queryFn,
}: UseAsyncJobQueryOptions<T>): AsyncJobQueryResult<T> {
  const queryClient = useQueryClient()

  const query = useQuery({
    queryKey,
    queryFn: async (): Promise<PolledJob<T>> => {
      const previous = queryClient.getQueryData<PolledJob<T>>(queryKey)
      const job = await queryFn(jobId!)
      return { job, attempts: (previous?.attempts ?? 0) + 1 }
    },
    enabled: jobId !== null,
    // 작업 상태는 계속 변하므로 전역 staleTime(60초)을 따르지 않는다.
    staleTime: 0,
    refetchInterval: (q) =>
      jobRefetchInterval(q.state.data?.job.status, q.state.data?.attempts ?? 0),
  })

  return {
    job: query.data?.job,
    isLoading: query.isLoading,
    error: query.error,
    isPollingExhausted: isJobPollingExhausted(
      query.data?.job.status,
      query.data?.attempts ?? 0,
    ),
  }
}
