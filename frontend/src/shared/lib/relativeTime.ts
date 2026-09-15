// 백엔드는 ISO 시각만 주는데 화면은 "40분 전"처럼 상대시간으로 보여준다
// (T-INT-12 결정 6). Intl.RelativeTimeFormat은 표준 내장이라 의존성이 늘지 않는다.

const RELATIVE = new Intl.RelativeTimeFormat('ko', { numeric: 'auto' })

const UNITS: { limit: number; seconds: number; unit: Intl.RelativeTimeFormatUnit }[] = [
  { limit: 60, seconds: 1, unit: 'second' },
  { limit: 3600, seconds: 60, unit: 'minute' },
  { limit: 86400, seconds: 3600, unit: 'hour' },
  { limit: 604800, seconds: 86400, unit: 'day' },
  { limit: 2629800, seconds: 604800, unit: 'week' },
  { limit: 31557600, seconds: 2629800, unit: 'month' },
]

/**
 * ISO 시각을 "방금 전" · "40분 전" · "3일 전"처럼 바꾼다. 1년이 넘으면 연 단위로 떨어진다.
 *
 * `now`를 주입할 수 있게 둔 건 테스트 때문이다 — 기본값은 호출 시점.
 */
export function toRelativeTime(isoTime: string, now: Date = new Date()): string {
  const elapsedSeconds = (new Date(isoTime).getTime() - now.getTime()) / 1000
  if (Number.isNaN(elapsedSeconds)) return '—'
  if (Math.abs(elapsedSeconds) < 5) return '방금 전'

  for (const { limit, seconds, unit } of UNITS) {
    if (Math.abs(elapsedSeconds) < limit) {
      return RELATIVE.format(Math.round(elapsedSeconds / seconds), unit)
    }
  }
  return RELATIVE.format(Math.round(elapsedSeconds / 31557600), 'year')
}
