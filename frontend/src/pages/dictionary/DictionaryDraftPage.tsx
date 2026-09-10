import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Button, Card, Pill } from '../../shared/ui'
import { useCandidates } from '../../features/dictionary/hooks/useCandidates'
import { cx } from '../../shared/lib/cx'

// ui/main.js renderDraftScreen() 이식. 표준어 선택(라디오)·정의 수정 같은 실제 편집은
// 백엔드가 없어 아직 반영되지 않는다 — 지금은 후보 목록/상세를 보여주는 데 집중했다.
export function DictionaryDraftPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const { data: candidates, isLoading } = useCandidates(workspaceId)
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const selected =
    candidates?.find((c) => c.id === selectedId) ?? candidates?.[0] ?? null

  if (isLoading) return <p className="text-sm text-text-tertiary">불러오는 중…</p>

  return (
    <div>
      <div className="mb-1 flex items-center justify-between">
        <h1 className="font-display text-[19px] font-bold text-text">
          사전집 초안 — 후보 작성
        </h1>
      </div>
      <p className="mb-5 text-xs text-text-quaternary">
        담당자를 지정해 표준어와 정의를 채우면, 개정안 제출 후 다른 멤버가 사전집 개정안에서
        승인합니다.
      </p>

      <div className="flex items-start gap-5">
        <Card className="flex-1 overflow-hidden">
          <table className="w-full border-collapse text-[12.5px]">
            <thead>
              <tr>
                {['후보 단어', '유형', '출현/문서', '작성자'].map((heading) => (
                  <th
                    key={heading}
                    className="whitespace-nowrap border-b border-border-soft px-4 py-[11px] text-left text-[11px] font-semibold text-text-quaternary"
                  >
                    {heading}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {candidates?.map((candidate) => (
                <tr
                  key={candidate.id}
                  onClick={() => setSelectedId(candidate.id)}
                  className={cx(
                    'cursor-pointer',
                    candidate.id === selected?.id
                      ? 'bg-accent-bg-strong shadow-[inset_3px_0_0_var(--color-accent)]'
                      : 'hover:bg-surface-muted',
                  )}
                >
                  <td className="border-b border-border-faint px-4 py-3 font-semibold text-text">
                    {candidate.words.join(', ')}
                  </td>
                  <td className="border-b border-border-faint px-4 py-3 text-text-secondary">
                    {candidate.type}
                  </td>
                  <td className="border-b border-border-faint px-4 py-3 text-text-secondary">
                    {candidate.occurrenceCount} / {candidate.occurredDocumentIds.length}
                  </td>
                  <td className="border-b border-border-faint px-4 py-3 text-text-secondary">
                    {candidate.ownerName}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>

        {selected && (
          <Card className="flex w-[420px] shrink-0 flex-col gap-4 p-5">
            <div className="flex items-baseline justify-between">
              <div className="flex items-center gap-2">
                <span className="font-display text-base">{selected.form}</span>
                <Pill tone="neutral">{selected.type}</Pill>
              </div>
              <span className="text-[11px] text-text-quaternary">
                출현 {selected.occurrenceCount}회 · 문서 {selected.occurredDocumentIds.length}건
              </span>
            </div>

            <div>
              <p className="mb-2 text-xs font-bold text-text">표준어 후보</p>
              <div className="flex flex-col gap-[7px]">
                {selected.words.map((word) => (
                  <div
                    key={word}
                    className="flex items-center gap-2 text-[13px] text-text-secondary"
                  >
                    <span className="inline-block h-[15px] w-[15px] shrink-0 rounded-full border-[1.5px] border-text-disabled" />
                    {word}
                  </div>
                ))}
              </div>
            </div>

            <div>
              <p className="mb-2 text-xs font-bold text-text">정의</p>
              <div className="min-h-16 rounded-sm border border-border-strong bg-surface-muted px-3 py-[10px] text-[12.5px] leading-[1.6] text-text-secondary">
                {selected.proposedDefinition}
              </div>
              <p className="mt-[5px] text-[10.5px] text-text-quaternary">
                AI 초안 · 수정할 수 있습니다
              </p>
            </div>

            <div>
              <p className="mb-2 text-xs font-bold text-text">근거 문장</p>
              <div className="flex flex-col gap-2">
                {selected.quotes.map((quote, idx) => (
                  <div
                    key={idx}
                    className={cx(
                      'rounded-r-xs border-l-2 bg-surface-muted px-3 py-[9px]',
                      quote.split ? 'border-accent' : 'border-border-strong',
                    )}
                  >
                    <p className="text-xs italic">"{quote.text}"</p>
                    <p className="mt-1 text-[10.5px] text-text-quaternary">{quote.source}</p>
                  </div>
                ))}
                {selected.splitNote && (
                  <p className="text-[11px] text-text-tertiary">{selected.splitNote}</p>
                )}
              </div>
            </div>

            <div className="flex items-center gap-[10px] border-t border-border-soft pt-[14px]">
              <span className="text-xs font-bold text-text">작성 담당자</span>
              <span className="rounded-sm border border-border-strong bg-surface px-[10px] py-[6px] text-xs">
                {selected.ownerName} ▾
              </span>
            </div>
            <div className="flex gap-2">
              <Button size="sm" variant="primary">
                작성 완료로 표시
              </Button>
              <Button size="sm" variant="outline">
                보류
              </Button>
            </div>
          </Card>
        )}
      </div>
    </div>
  )
}
