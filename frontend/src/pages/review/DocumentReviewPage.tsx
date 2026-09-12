import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Card, Pill } from '../../shared/ui'
import { cx } from '../../shared/lib/cx'
import { routes } from '../../shared/config/routes'
import { useAuthStore } from '../../shared/stores/authStore'
import { useSuggestions } from '../../features/document/hooks/useSuggestions'
import { useResolveSuggestion } from '../../features/document/hooks/useResolveSuggestion'
import { useSuggestionHistory } from '../../features/document/hooks/useSuggestionHistory'
import { useDocument } from '../../features/document/hooks/useDocument'
import { SUGGESTION_PARAGRAPHS } from '../../features/document/model/suggestionFixtures'
import type { SuggestionTerm } from '../../features/document/model/types'
import { useWorkspaceMembers } from '../../features/member/hooks/useWorkspaceMembers'
import { useRequestDocumentReview } from '../../features/review/hooks/useRequestDocumentReview'

// ui/main.js renderReviewDocScreen() 이식. 본문은 문서별 고정 조각(SUGGESTION_PARAGRAPHS)을
// 그대로 옮겼다 — 실제 문서 본문에서 정확한 위치(anchor)를 찾아 치환 후보를 표시하는 로직은
// 아직 없다(SuggestionTerm.anchor가 지금은 자리표시자 값). 본문과 위치가 진짜로 연결되려면
// 백엔드의 대조(DictionaryContrast, MVP1 제외) 결과가 필요하다.
export function DocumentReviewPage() {
  const { workspaceId = '', documentId = '' } = useParams<{
    workspaceId: string
    documentId: string
  }>()
  const navigate = useNavigate()
  const currentMember = useAuthStore((state) => state.currentMember)
  const { data: document } = useDocument(documentId)
  const { data: suggestions } = useSuggestions(documentId)
  const { data: history } = useSuggestionHistory(documentId)
  const { data: members } = useWorkspaceMembers(workspaceId)
  const resolveSuggestion = useResolveSuggestion(documentId)
  const requestReview = useRequestDocumentReview(workspaceId)
  const [activeId, setActiveId] = useState<string | null>(null)
  const [sideTab, setSideTab] = useState<'suggestions' | 'history'>('suggestions')

  const byId = new Map(suggestions?.map((s) => [s.id, s]) ?? [])
  const resolvedCount = suggestions?.filter((s) => s.status !== 'PENDING').length ?? 0
  const totalCount = suggestions?.length ?? 0
  const allResolved = totalCount > 0 && resolvedCount === totalCount
  const paragraph = SUGGESTION_PARAGRAPHS[documentId] ?? []

  function handleCompleteReview() {
    if (!document || !currentMember) return
    const reviewerMemberIds = (members ?? [])
      .filter((m) => m.id !== currentMember.id)
      .map((m) => m.id)

    requestReview.mutate(
      {
        workspaceId,
        documentId,
        title: `${document.title} 개정 반영`,
        requesterId: currentMember.id,
        reviewerMemberIds,
      },
      {
        onSuccess: (reviewRequest) => {
          navigate(routes.documentReviewThread(workspaceId, documentId, reviewRequest.id))
        },
      },
    )
  }

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
        {document && <Pill tone="outline">r{document.currentVersionNo}</Pill>}
        {document?.badge && (
          <Pill tone={document.badge === 'danger' ? 'danger' : 'neutral'}>
            {document.badge === 'danger' ? '재검사 필요' : '뒤처짐'}
          </Pill>
        )}
        <div className="flex-1" />
        {!allResolved && (
          <span className="text-[11.5px] text-text-quaternary">
            제안 {resolvedCount}/{totalCount}건 처리 — 전부 처리해야 리뷰를 요청할 수 있습니다
          </span>
        )}
        <Button
          variant="primary"
          disabled={!allResolved || requestReview.isPending}
          onClick={handleCompleteReview}
        >
          {requestReview.isPending ? '리뷰 요청 중…' : '검토 완료 · 리뷰 요청'}
        </Button>
      </div>

      <div className="flex items-start gap-5">
        <Card className="flex-1 p-[26px] text-sm leading-[2.1] text-[#2A2D33]">
          {paragraph.length === 0 && (
            <p className="text-text-tertiary">이 문서에는 아직 검토할 제안이 없습니다.</p>
          )}
          {paragraph.map((part, idx) => (
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
                처리 {resolvedCount}/{totalCount}
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
