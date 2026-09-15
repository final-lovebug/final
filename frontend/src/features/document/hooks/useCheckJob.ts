import { fetchCheckJob, type CheckJob } from '../api/checkJobApi'
import { useAsyncJobQuery } from '../../../shared/api/useAsyncJobQuery'

/**
 * 문서 대조 작업 폴링(DD-5). `checkJobId`가 null이면 쉰다(아직 접수 전).
 *
 * 종단 상태(SUCCEEDED·FAILED)에 닿거나 상한을 넘기면 멈춘다. 상한에 걸린 것은 실패가
 * 아니다 — 서버에서는 작업이 계속 진행되거나 회수 스위퍼가 끝낸다(D-77).
 */
export function useCheckJob(checkJobId: string | null) {
  return useAsyncJobQuery<CheckJob>({
    jobId: checkJobId,
    queryKey: ['check-job', checkJobId],
    queryFn: fetchCheckJob,
  })
}
