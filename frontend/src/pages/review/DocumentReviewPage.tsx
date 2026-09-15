import { useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  Banner,
  Button,
  Card,
  ColFlex,
  DataTable,
  Pill,
  PrThread,
  TabRow,
  Td,
  TextInput,
  Th,
  Toolbar,
  ToolbarSpacer,
  Tr,
  TwoCol,
} from '../../shared/ui'
import { cx } from '../../shared/lib/cx'
import { routes } from '../../shared/config/routes'
import { ApiError } from '../../shared/api/httpClient'
import { useAuthStore } from '../../shared/stores/authStore'
import { useSuggestions } from '../../features/document/hooks/useSuggestions'
import { useResolveSuggestion } from '../../features/document/hooks/useResolveSuggestion'
import { useSuggestionHistory } from '../../features/document/hooks/useSuggestionHistory'
import { useDocument } from '../../features/document/hooks/useDocument'
import { useCheckJob } from '../../features/document/hooks/useCheckJob'
import { useCreateCheckJob } from '../../features/document/hooks/useCreateCheckJob'
import { buildSuggestionSegments } from '../../features/document/model/suggestionSegments'
import type { SuggestionTerm } from '../../features/document/model/types'
import { useWorkspaceMembers } from '../../features/member/hooks/useWorkspaceMembers'
import { useRequestDocumentReview } from '../../features/review/hooks/useRequestDocumentReview'

type SideTab = 'suggestions' | 'history'

// ui/main.js renderReviewDocScreen() 이식 — 본문 하이라이트 + 우측 제안 목록 2단
// (`REQ-CHK-004`).
//
// **본문은 초안 본문(`draftBody`)이고 하이라이트는 제안어의 anchor로 그린다**(T-INT-17).
// 프로토타입은 문서별로 하드코딩한 문단 조각을 썼기 때문에 그 2개 문서 바깥에서는 아무것도
// 보이지 않았다 — 대조(DictionaryContrast)가 실제로 붙으면서 데이터 기반으로 바꿨다.
//
// 대조를 한 번도 돌리지 않은 문서에는 초안이 없다. 그 경우 「최신 사전집으로 갱신」으로
// 대조 작업(DD-5)을 접수하고 완료까지 폴링한다 — 문서 상세 화면에서 접수해 들어오면
// 작업 id가 쿼리로 넘어온다.
export function DocumentReviewPage() {
  const { workspaceId = '', documentId = '' } = useParams<{
    workspaceId: string
    documentId: string
  }>()
  const navigate = useNavigate()
  const currentMember = useAuthStore((state) => state.currentMember)
  const { data: document } = useDocument(workspaceId, documentId)
  const { data: contrast, isLoading: isLoadingContrast } = useSuggestions(documentId)
  const { data: history } = useSuggestionHistory(documentId)
  const { data: members } = useWorkspaceMembers(workspaceId)
  const resolveSuggestion = useResolveSuggestion(documentId)
  const requestReview = useRequestDocumentReview(workspaceId)

  const [searchParams] = useSearchParams()
  const [checkJobId, setCheckJobId] = useState<string | null>(() => searchParams.get('checkJob'))
  const createCheckJob = useCreateCheckJob(workspaceId, documentId)
  const { job: checkJob, isPollingExhausted } = useCheckJob(checkJobId)

  const [activeId, setActiveId] = useState<string | null>(null)
  const [sideTab, setSideTab] = useState<SideTab>('suggestions')
  const [rejectReason, setRejectReason] = useState('')

  const draft = contrast?.draft ?? null
  const suggestions = contrast?.suggestions ?? []
  const resolvedCount = suggestions.filter((s) => s.status !== 'PENDING').length
  const totalCount = suggestions.length
  const allResolved = totalCount > 0 && resolvedCount === totalCount
  const segments = draft ? buildSuggestionSegments(draft.draftBody, suggestions) : []
  const isChecking = checkJob?.status === 'PENDING' || checkJob?.status === 'RUNNING'

  function handleRunCheck() {
    createCheckJob.mutate(undefined, { onSuccess: (created) => setCheckJobId(created.id) })
  }

  function handleCompleteReview() {
    if (!document || !currentMember) return
    const reviewerMemberIds = (members ?? [])
      .filter((member) => member.id !== currentMember.id)
      .map((member) => member.id)

    // 요청자는 인증 주체에서 해석되므로 보내지 않는다. 대상 초안(draftDocumentId)은
    // api가 documentId로 찾아준다.
    requestReview.mutate(
      { documentId, title: `${document.title} 개정 반영`, reviewerMemberIds },
      {
        onSuccess: (reviewRequest) =>
          navigate(routes.documentReviewThread(workspaceId, documentId, reviewRequest.id)),
      },
    )
  }

  function openSuggestion(suggestion: SuggestionTerm) {
    setActiveId(suggestion.id)
    setRejectReason(suggestion.rejectReason ?? '')
  }

  function renderSuggestionSpan(suggestion: SuggestionTerm, text: string) {
    if (suggestion.status !== 'PENDING') {
      const applied = suggestion.status === 'APPLY_SUGGESTION'
      return (
        <span
          title={
            applied
              ? `적용됨 — "${suggestion.originTerm}" → "${suggestion.suggestionTerm}"`
              : `유지됨 — ${suggestion.rejectReason ?? '사유 없음'}`
          }
          className={cx(
            'cursor-default border-b-[1.5px] border-dashed',
            applied ? 'border-success text-success' : 'border-text-faint',
          )}
        >
          {text}
        </span>
      )
    }

    const isOpen = activeId === suggestion.id
    return (
      <span className="relative">
        <span
          onClick={() => (isOpen ? setActiveId(null) : openSuggestion(suggestion))}
          className={cx(
            'cursor-pointer font-semibold text-accent-strong underline',
            isOpen && 'bg-accent-bg',
          )}
        >
          {text}
        </span>
        {isOpen && (
          <span className="absolute left-0 top-full z-10 mt-2 block w-[300px] rounded-md border border-border bg-surface p-4 text-[12.5px] leading-[1.6] shadow-pop">
            <span className="mb-3 block font-bold">
              {suggestion.originTerm} → {suggestion.suggestionTerm}
            </span>

            <span className="mb-3 flex gap-2">
              <Button
                size="sm"
                variant="primary"
                disabled={resolveSuggestion.isPending}
                onClick={() =>
                  resolveSuggestion.mutate(
                    { suggestionId: suggestion.id, decision: 'APPLY_SUGGESTION' },
                    { onSuccess: () => setActiveId(null) },
                  )
                }
              >
                적용
              </Button>
              <Button
                size="sm"
                variant="outline"
                // 실 API가 사유를 필수로 받는다(@NotBlank) — 비어 있으면 400이라 미리 막는다.
                disabled={resolveSuggestion.isPending || rejectReason.trim() === ''}
                onClick={() =>
                  resolveSuggestion.mutate(
                    {
                      suggestionId: suggestion.id,
                      decision: 'KEEP_ORIGINAL',
                      rejectReason: rejectReason.trim(),
                    },
                    { onSuccess: () => setActiveId(null) },
                  )
                }
              >
                무시
              </Button>
            </span>

            <span className="mb-1 block text-[10.5px] text-text-quaternary">
              무시 사유 (무시할 때만 필요)
            </span>
            <TextInput
              value={rejectReason}
              onChange={(event) => setRejectReason(event.target.value)}
              placeholder="예: 제품 고유명사라 원문을 유지합니다"
              className="py-[6px] text-xs"
            />

            {resolveSuggestion.isError && (
              <span className="mt-2 block text-[11px] text-danger">
                {resolveSuggestion.error instanceof ApiError
                  ? resolveSuggestion.error.message
                  : '판정에 실패했습니다.'}
              </span>
            )}
          </span>
        )}
      </span>
    )
  }

  return (
    <div>
      <Toolbar>
        <Pill tone="warn" size="lg">
          AI 검토
        </Pill>
        {document && <Pill tone="outline">r{document.currentVersionNo}</Pill>}
        {document?.badge && (
          <Pill tone={document.badge === 'danger' ? 'danger' : 'neutral'}>
            {document.badge === 'danger' ? '재검사 필요' : '뒤처짐'}
          </Pill>
        )}
        {draft && (
          <span className="text-[11.5px] text-text-tertiary">
            제안 {resolvedCount}/{totalCount}건 처리
          </span>
        )}

        <ToolbarSpacer />

        {!draft && !isLoadingContrast && (
          <Button
            variant="outline"
            disabled={createCheckJob.isPending || isChecking}
            onClick={handleRunCheck}
          >
            {createCheckJob.isPending ? '접수 중…' : isChecking ? '대조 중…' : '최신 사전집으로 갱신'}
          </Button>
        )}
        {draft && (
          <Button
            variant="primary"
            disabled={!allResolved || requestReview.isPending}
            title={allResolved ? undefined : '제안을 전부 처리해야 리뷰를 요청할 수 있습니다'}
            onClick={handleCompleteReview}
          >
            {requestReview.isPending ? '리뷰 요청 중…' : '검토 완료 · 리뷰 요청'}
          </Button>
        )}
      </Toolbar>

      {createCheckJob.isError && (
        <Card className="mb-4 border-danger-border bg-danger-bg p-4">
          <p className="text-[12.5px] font-semibold text-danger">
            대조 작업을 접수하지 못했습니다.
          </p>
          <p className="mt-1 text-xs text-text-secondary">
            {createCheckJob.error instanceof ApiError
              ? createCheckJob.error.message
              : '알 수 없는 오류가 발생했습니다.'}
          </p>
        </Card>
      )}

      {checkJob && !draft && (
        <Card className="mb-4 p-4">
          <div className="mb-2 flex items-center gap-2">
            <Pill tone={checkJob.status === 'FAILED' ? 'danger' : 'warn'}>
              {checkJob.status === 'PENDING'
                ? '대기 중'
                : checkJob.status === 'RUNNING'
                  ? '대조 중'
                  : checkJob.status === 'SUCCEEDED'
                    ? '완료'
                    : '실패'}
            </Pill>
            <span className="text-[11.5px] text-text-quaternary">작업 #{checkJob.id}</span>
          </div>
          {isChecking && (
            <p className="text-[12.5px] text-text-tertiary">
              AI 워커가 최신 사전집과 본문을 대조하고 있습니다. 완료되면 이 화면이 자동으로
              갱신됩니다.
            </p>
          )}
          {isPollingExhausted && (
            <p className="text-[12.5px] text-warn">
              5분 동안 끝나지 않아 상태 확인을 중단했습니다. 작업은 서버에서 계속 진행되거나
              제한 시간 뒤 실패로 회수됩니다.
            </p>
          )}
          {checkJob.status === 'FAILED' && (
            <p className="text-[12.5px] text-danger">
              대조에 실패했습니다 — {checkJob.failureReason ?? '원인이 기록되지 않았습니다.'}
            </p>
          )}
        </Card>
      )}

      <TwoCol>
        <ColFlex>
          <Card className="whitespace-pre-wrap px-[30px] py-[26px] text-sm leading-[2.1] text-[#2A2D33]">
            {isLoadingContrast && <p className="text-text-tertiary">불러오는 중…</p>}
            {!isLoadingContrast && !draft && (
              <p className="text-text-tertiary">
                아직 이 문서의 대조 결과가 없습니다. 위의 「최신 사전집으로 갱신」으로 대조를
                실행하세요.
              </p>
            )}
            {draft && segments.length === 0 && (
              <p className="text-text-tertiary">초안 본문이 비어 있습니다.</p>
            )}
            {segments.map((segment, index) =>
              segment.kind === 'text' ? (
                <span key={index}>{segment.text}</span>
              ) : (
                <span key={index}>
                  {renderSuggestionSpan(segment.suggestion, segment.text)}
                </span>
              ),
            )}

            {draft && totalCount === 0 && (
              <Banner tone="neutral" className="mt-6">
                대조 결과 제안된 용어가 없습니다 — 본문이 이미 최신 사전집 기준과 맞습니다.
              </Banner>
            )}
            {draft && (
              <p className="mt-6 text-[10.5px] text-text-faint">
                수락하면 새 버전으로 커밋됩니다 · 검토본은 따로 만들지 않습니다 · 리뷰어는
                [처리 내역] 탭에서 이 기록을 봅니다
              </p>
            )}
          </Card>
        </ColFlex>

        <PrThread className="w-[320px] rounded-md border border-border bg-surface p-[18px]">
          <TabRow
            tabs={[
              { value: 'suggestions', label: `제안 ${totalCount}건` },
              { value: 'history', label: '처리 내역' },
            ]}
            value={sideTab}
            onChange={setSideTab}
          />

          {sideTab === 'suggestions' ? (
            <>
              <p className="my-3 text-xs font-semibold text-text-tertiary">
                처리 {resolvedCount}/{totalCount}
              </p>
              <div className="flex flex-col gap-[7px] text-xs">
                {totalCount === 0 && (
                  <p className="text-text-tertiary">표시할 제안이 없습니다.</p>
                )}
                {suggestions.map((suggestion) => (
                  <div
                    key={suggestion.id}
                    onClick={() =>
                      suggestion.status === 'PENDING' && openSuggestion(suggestion)
                    }
                    className={cx(
                      'rounded-[7px] border-[1.5px] px-[10px] py-2',
                      suggestion.status === 'PENDING' ? 'cursor-pointer' : 'cursor-default',
                      suggestion.id === activeId
                        ? 'border-accent bg-accent-bg-strong'
                        : 'border-border-strong',
                      suggestion.status !== 'PENDING' && 'text-text-quaternary',
                    )}
                  >
                    {suggestion.originTerm} → {suggestion.suggestionTerm}
                    {suggestion.status !== 'PENDING' && (
                      <span className="ml-1 font-semibold text-success">✓</span>
                    )}
                  </div>
                ))}
              </div>
            </>
          ) : (
            <div className="mt-3 overflow-x-auto">
              {(!history || history.length === 0) && (
                <p className="text-xs text-text-tertiary">아직 처리한 제안이 없습니다.</p>
              )}
              {history && history.length > 0 && (
                <DataTable className="text-xs">
                  <thead>
                    <tr>
                      <Th className="px-2 text-[10.5px]">원래</Th>
                      <Th className="px-2 text-[10.5px]">결과</Th>
                      <Th className="px-2 text-[10.5px]">처리</Th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.map((item) => (
                      <Tr key={`${item.original}-${item.result}`}>
                        <Td className="px-2 py-2">
                          {item.action === 'ignored' && (
                            <span className="mr-1 inline-block h-[5px] w-[5px] rounded-full bg-danger" />
                          )}
                          {item.original}
                        </Td>
                        <Td className="px-2 py-2">{item.result}</Td>
                        <Td
                          className={cx(
                            'px-2 py-2',
                            item.action === 'applied' && 'font-semibold text-success',
                            item.action === 'manual' && 'font-semibold text-accent-strong',
                            item.action === 'ignored' && 'text-text-tertiary',
                          )}
                        >
                          {item.action === 'applied'
                            ? '적용'
                            : item.action === 'manual'
                              ? `직접 입력 → "${item.manualValue}"`
                              : `무시 — "${item.reason}"`}
                        </Td>
                      </Tr>
                    ))}
                  </tbody>
                </DataTable>
              )}
            </div>
          )}
        </PrThread>
      </TwoCol>
    </div>
  )
}
