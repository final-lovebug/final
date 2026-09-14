import { Link, useParams } from 'react-router-dom'
import { Button, Card, Pill } from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { useDictionaryRevisionTimeline } from '../../features/dictionary/hooks/useDictionaryRevisionTimeline'
import type { DictionaryRevisionTone } from '../../features/dictionary/model/versionFixtures'
import type { PillTone } from '../../shared/ui'

const TONE_MAP: Record<DictionaryRevisionTone, PillTone> = {
  success: 'success',
  neutral: 'neutral',
  danger: 'danger',
  accent: 'accent',
}

export function DictionaryHistoryPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const { data, isLoading } = useDictionaryRevisionTimeline(workspaceId)

  if (isLoading) return <p className="text-sm text-text-tertiary">불러오는 중…</p>
  if (!data) return null

  return (
    <div>
      <div className="mb-[18px] flex items-center gap-3">
        <h1 className="font-display text-[19px] font-bold text-text">
          사전집 리비전 이력 · 비교
        </h1>
        <div className="flex-1" />
        <Link to={routes.dictionary(workspaceId)}>
          <Button variant="outline">돌아가기</Button>
        </Link>
      </div>

      <div className="flex items-start gap-5">
        <div className="flex w-[380px] shrink-0 flex-col gap-[9px]">
          {data.timeline.map((item) => (
            <div
              key={item.id}
              className={`rounded-md bg-surface p-[15px] ${
                item.emphasize
                  ? 'border-2 border-danger'
                  : item.current
                    ? 'border-2 border-accent'
                    : 'border border-border'
              }`}
            >
              <div className="flex items-center gap-2">
                <span className="font-bold">{item.id}</span>
                <Pill tone={TONE_MAP[item.tone]}>{item.grade}</Pill>
                {item.current && (
                  <span className="text-[10px] font-bold text-accent-strong">현재</span>
                )}
              </div>
              <p className="my-1 text-[11px] text-text-quaternary">
                {item.date} · {item.author}
              </p>
              <p className="text-xs text-text-secondary">{item.summary}</p>
              {item.emphasize && (
                <p className="mt-1 text-[10.5px] text-danger">문서 2건에 재검사 필요</p>
              )}
            </div>
          ))}
        </div>

        <Card className="flex-1 p-[22px]">
          <p className="mb-4 font-display text-[15px]">r6 → r7 변경 내역</p>
          <div className="flex flex-col gap-4">
            <div>
              <p className="mb-2 text-[12.5px] font-bold text-success">
                추가 {data.latestDiff.added.length}
              </p>
              <p className="text-[13px] leading-[1.8] text-text-secondary">
                {data.latestDiff.added.join(' · ')}
              </p>
            </div>
            <div>
              <p className="mb-2 text-[12.5px] font-bold text-warn">
                변경 {data.latestDiff.changed.length}
              </p>
              <p className="text-[13px] leading-[1.8] text-text-secondary">
                {data.latestDiff.changed.join(' · ')}
              </p>
            </div>
            <p className="text-[12.5px] font-bold text-text-quaternary">
              삭제 {data.latestDiff.removed.length}
            </p>
          </div>
          <div className="mt-[18px] rounded-sm border border-accent-border bg-accent-bg-strong p-4 text-[12.5px] leading-[1.7] text-text">
            대표어 변경과 용어 삭제가 없어 '새 지적'입니다. 기존 문서를 다시 볼 필요는 없고,
            새 용어에 대한 제안만 늘어납니다.
            <br />
            <strong>r6 이후 재검사 등급 없음</strong> → r6 태그를 단 문서는 '뒤처짐'으로만
            표시됩니다.
          </div>
        </Card>
      </div>
    </div>
  )
}
