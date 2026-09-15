// 비동기 작업(용어 추출·문서 대조)의 폴링 정책. 두 도메인이 같은 상태 축
// (PENDING/RUNNING/SUCCEEDED/FAILED)과 같은 폴링 규격을 쓰므로 여기 한 곳에 둔다
// (docs/API.md "용어 추출 작업 접수와 조회"·"비동기 문서 대조").
//
// 상태 조회가 폴링인 이유는 백엔드 결정이다(D-34) — 작업 상태는 DB 작업 테이블로
// 관리하고 클라이언트가 주기적으로 읽는다. 실행 주체는 외부 FastAPI 워커이고
// 완료는 워커가 백엔드로 치는 HTTP 콜백으로 반영된다(D-66·D-68).

/** 작업 상태 4종(docs/API.md). 추출·대조가 같은 축을 쓴다. */
export type AsyncJobStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'

/**
 * 폴링 간격. 2초는 "예상 소요 2~3분"(추출 화면 안내 문구) 대비 충분히 촘촘하면서
 * 5분 동안 150회를 넘지 않는 값이다.
 */
export const JOB_POLL_INTERVAL_MS = 2_000

/**
 * 폴링 상한. 백엔드의 회수 스위퍼(`app.ai.timeout.job`, 기본 15분)보다 짧다 — 화면이
 * 먼저 포기해도 작업 자체는 서버에서 계속 진행되거나 스위퍼가 FAILED로 회수한다(D-77).
 * 그래서 상한에 걸린 것은 "실패"가 아니라 "그만 지켜봄"으로 표시한다.
 */
export const JOB_POLL_TIMEOUT_MS = 5 * 60 * 1_000

export const JOB_POLL_MAX_ATTEMPTS = Math.ceil(JOB_POLL_TIMEOUT_MS / JOB_POLL_INTERVAL_MS)

export function isTerminalJobStatus(status: AsyncJobStatus | undefined): boolean {
  return status === 'SUCCEEDED' || status === 'FAILED'
}

/**
 * TanStack Query `refetchInterval`에 그대로 넘길 값을 계산한다.
 *
 * `false`를 돌려주면 폴링이 멈춘다 — 종단 상태에 도달했거나 상한을 넘겼을 때다.
 * `attempts`는 이 작업을 지금까지 몇 번 조회했는지다.
 */
export function jobRefetchInterval(
  status: AsyncJobStatus | undefined,
  attempts: number,
): number | false {
  if (isTerminalJobStatus(status)) return false
  if (attempts >= JOB_POLL_MAX_ATTEMPTS) return false
  return JOB_POLL_INTERVAL_MS
}

/** 상한에 걸려 폴링을 멈춘 상태인지. 화면이 "확인을 중단했다"고 알리는 데 쓴다. */
export function isJobPollingExhausted(
  status: AsyncJobStatus | undefined,
  attempts: number,
): boolean {
  return (
    status !== undefined && !isTerminalJobStatus(status) && attempts >= JOB_POLL_MAX_ATTEMPTS
  )
}
