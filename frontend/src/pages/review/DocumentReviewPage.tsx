import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Button, Card, Pill } from '../../shared/ui'
import { cx } from '../../shared/lib/cx'
import { useSuggestions } from '../../features/document/hooks/useSuggestions'
import { useResolveSuggestion } from '../../features/document/hooks/useResolveSuggestion'
import { useSuggestionHistory } from '../../features/document/hooks/useSuggestionHistory'
import type { SuggestionTerm } from '../../features/document/model/types'

// ui/main.js renderReviewDocScreen() 이식. 본문은 doc-plan 문서의 고정된 문구를 그대로
// 옮겼다 — 실제 문서 본문에서 정확한 위치(anchor)를 찾아 치환 후보를 표시하는 로직은
// 아직 없다(SuggestionTerm.anchor가 지금은 자리표시자 값). 본문과 위치가 진짜로 연결되려면
// 백엔드의 대조(DictionaryContrast, MVP1 제외) 결과가 필요하다.
const PARAGRAPH: Array<{ text: string } | { suggestionId: string }> = [
  { text: '새로운 신규 획득 정책 2026에 따라 ' },
  { suggestionId: 's1' },
  { text: '를 대상으로 다음과 같은 비즈니스 규칙을 적용합니다. 첫 번째 규칙은 ' },
  { suggestionId: 's2' },
  { text: ' 14일이 충분한 시점을 기준으로 합니다. 이때 고객의 상태는 ' },
  { suggestionId: 's3' },
  { text: '로 자동 전환되어야 합니다. 만약 이 과정에서 ' },
  { suggestionId: 's4' },
  { text: '가 발생할 경우, 시스템은 즉시 ' },
  { suggestionId: 's5' },
  { text: ' 처리를 진행하고 안내 메일을 발송해야 합니다. 두 번째 규칙은 기존 고객의 상향 가입을 유도하기 위한 정책입니다. ' },
  { suggestionId: 's6' },
  { text: '을 진행할 경우, 시스템은 혜택의 일환으로 ' },
  { suggestionId: 's7' },
  { text: ' 5,000원을 지급합니다.' },
]

export function DocumentReviewPage() {
  const { documentId = '' } = useParams<{ documentId: string }>()
  const { data: suggestions } = useSuggestions(documentId)
  const { data: history } = useSuggestionHistory(documentId)
  const resolveSuggestion = useResolveSuggestion(documentId)
  const [activeId, setActiveId] = useState<string | null>(null)
  const [sideTab, setSideTab] = useState<'suggestions' | 'history'>('suggestions')

  const byId = new Map(suggestions?.map((s) => [s.id, s]) ?? [])
  const resolvedCount =
    suggestions?.filter((s) => s.status !== 'PENDING').length ?? 0

  function renderSpan(suggestion: SuggestionTerm) {
    if (suggestion.status !== 'PENDING') {
      return (
        <span
          key={suggestion.id}
          title={`원래: ${suggestion.originTerm} · 근거: ${suggestion.suggestionTerm}`}
          className="cursor-default border-b-[1.5px] border-dashed border-text-faint"
        >
          {suggestion.originTerm}
        </span>
      )
    }

    const isOpen = activeId === suggestion.id
    return (
      <span key={suggestion.id} className="relative">
        <span
          onClick={() => setActiveId(isOpen ? null : suggestion.id)}
          className={cx(
            'cursor-pointer font-semibold text-accent-strong underline',
            isOpen && 'bg-accent-bg',
          )}
        >
          {suggestion.originTerm}
        </span>
        {isOpen && (
          <Card className="absolute left-0 top-full z-10 mt-2 w-[280px] p-4 text-xs leading-[1.6] shadow-pop">
            <p className="mb-3 font-bold">
              {suggestion.originTerm} → {suggestion.suggestionTerm}
            </p>
            <div className="flex gap-2">
              <Button
                size="sm"
                variant="primary"
                onClick={() => {
                  resolveSuggestion.mutate({
                    suggestionId: suggestion.id,
                    status: 'APPLY_SUGGESTION',
                  })
                  setActiveId(null)
                }}
              >
                적용
              </Button>
              <Button
                size="sm"
                variant="outline"
                onClick={() => {
                  resolveSuggestion.mutate({
                    suggestionId: suggestion.id,
                    status: 'KEEP_ORIGINAL',
                  })
                  setActiveId(null)
                }}
              >
                무시
              </Button>
            </div>
          </Card>
        )}
      </span>
    )
  }

  return (
    <div>
      <div className="mb-4 flex items-center gap-3">
        <Pill tone="warn">AI 검토</Pill>
        <Pill tone="outline">r4</Pill>
        <Pill tone="danger">재검사 필요</Pill>
        <span className="text-[11.5px] text-text-tertiary">리뷰어 승인 1/2</span>
        <div className="flex-1" />
        <Button variant="primary">검토 완료</Button>
      </div>

      <div className="flex items-start gap-5">
        <Card className="flex-1 p-[26px] text-sm leading-[2.1] text-[#2A2D33]">
          {PARAGRAPH.map((part, idx) => (
            <span key={idx}>
              {'text' in part
                ? part.text
                : byId.has(part.suggestionId)
                  ? renderSpan(byId.get(part.suggestionId)!)
                  : null}
            </span>
          ))}
          <p className="mt-6 text-[10.5px] text-text-faint">
            수락하면 새 버전으로 커밋됩니다 · 검토본은 따로 만들지 않습니다 · 리뷰어는
            [처리 내역] 탭에서 이 기록을 봅니다
          </p>
        </Card>

        <Card className="w-[320px] shrink-0 p-[18px]">
          <div className="flex gap-4 border-b border-border-soft">
            <button
              type="button"
              onClick={() => setSideTab('suggestions')}
              className={cx(
                'border-b-2 border-transparent pb-[9px] text-[12.5px] text-text-quaternary',
                sideTab === 'suggestions' && 'border-accent font-bold text-text',
              )}
            >
              제안 {suggestions?.length ?? 0}건
            </button>
            <button
              type="button"
              onClick={() => setSideTab('history')}
              className={cx(
                'border-b-2 border-transparent pb-[9px] text-[12.5px] text-text-quaternary',
                sideTab === 'history' && 'border-accent font-bold text-text',
              )}
            >
              처리 내역
            </button>
          </div>

          {sideTab === 'suggestions' ? (
            <>
              <p className="my-3 text-xs font-semibold text-text-tertiary">
                처리 {resolvedCount}/{suggestions?.length ?? 0}
              </p>
              <div className="flex flex-col gap-[7px] text-xs">
                {suggestions?.map((s) => (
                  <div
                    key={s.id}
                    onClick={() => s.status === 'PENDING' && setActiveId(s.id)}
                    className={cx(
                      'cursor-pointer rounded-md border-[1.5px] px-[10px] py-2',
                      s.id === activeId
                        ? 'border-accent bg-accent-bg-strong'
                        : 'border-border-strong',
                      s.status !== 'PENDING' && 'text-text-quaternary',
                    )}
                  >
                    {s.originTerm} → {s.suggestionTerm}
                    {s.status !== 'PENDING' && (
                      <span className="ml-1 font-semibold text-success">✓</span>
                    )}
                  </div>
                ))}
              </div>
            </>
          ) : (
            <div className="mt-3 overflow-x-auto">
              <table className="w-full border-collapse text-xs">
                <thead>
                  <tr>
                    {['원래', '결과', '처리'].map((h) => (
                      <th
                        key={h}
                        className="border-b border-border-soft px-2 py-2 text-left text-[10.5px] text-text-quaternary"
                      >
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {history?.map((h) => (
                    <tr key={h.original}>
                      <td className="border-b border-border-faint px-2 py-2">
                        {h.action === 'ignored' && (
                          <span className="mr-1 inline-block h-[5px] w-[5px] rounded-full bg-danger" />
                        )}
                        {h.original}
                      </td>
                      <td className="border-b border-border-faint px-2 py-2">
                        {h.result}
                      </td>
                      <td
                        className={cx(
                          'border-b border-border-faint px-2 py-2',
                          h.action === 'applied' && 'font-semibold text-success',
                          h.action === 'manual' && 'font-semibold text-accent-strong',
                          h.action === 'ignored' && 'text-text-tertiary',
                        )}
                      >
                        {h.action === 'applied'
                          ? '적용'
                          : h.action === 'manual'
                            ? `직접 입력 → "${h.manualValue}"`
                            : `무시 — "${h.reason}"`}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </div>
    </div>
  )
}
