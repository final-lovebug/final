import { useMemo, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  Avatar,
  Banner,
  Button,
  Card,
  ColFlex,
  CommentCard,
  DataTable,
  DetailPanel,
  FieldLabel,
  FilterChip,
  Pill,
  QuoteBlock,
  RadioDot,
  ScreenSubtitle,
  ScreenTitle,
  Td,
  TextArea,
  TextInput,
  Th,
  Toolbar,
  ToolbarSpacer,
  Tr,
  TwoCol,
} from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { cx } from '../../shared/lib/cx'
import { toRelativeTime } from '../../shared/lib/relativeTime'
import { ApiError } from '../../shared/api/httpClient'
import { useAuthStore } from '../../shared/stores/authStore'
import { useDictionaryDraft } from '../../features/dictionary/hooks/useDictionaryDraft'
import { useCreateCandidateTerm } from '../../features/dictionary/hooks/useCreateCandidateTerm'
import { useUpdateCandidateTerm } from '../../features/dictionary/hooks/useUpdateCandidateTerm'
import { useSubmitDictionaryRevision } from '../../features/dictionary/hooks/useSubmitDictionaryRevision'
import { useWorkspaceMembers } from '../../features/member/hooks/useWorkspaceMembers'
import { ReviewerSelectDialog } from '../../features/review/components/ReviewerSelectDialog'
import { useDictionaryRevision } from '../../features/review/hooks/useDictionaryRevision'
import { usePerformReexamine } from '../../features/review/hooks/useReviewLifecycle'
import type { CandidateTermType } from '../../features/dictionary/model/types'
import type { CandidateTermListItem } from '../../features/dictionary/model/fixtures'

const TYPE_LABELS: Record<CandidateTermType, string> = {
  SYNONYM: '동의어',
  HOMOGRAPH: '동형이의',
  VARIANT: '표기 변형',
}
const CANDIDATE_TYPES = Object.keys(TYPE_LABELS) as CandidateTermType[]

/** 로딩 중에도 참조가 안 바뀌게 고정한다 — 매 렌더마다 새 배열을 만들면 useMemo가 헛돈다. */
const NO_CANDIDATES: CandidateTermListItem[] = []

/**
 * 사전집 초안 — 단일 페이지(`docs/plan/DRAFT_PLAN.md`).
 *
 * 후보어 묶음의 **대표어와 정의만** 정리하고, 전부 채워지면 곧바로 리뷰를 요청한다. 예전에
 * 있던 후보어별 작성자·담당자·판정 상태·체크박스·일괄 처리는 전부 걷어냈다 — 준비 여부를
 * 판정 건수로 세지 않고 「모든 후보어가 대표어와 정의를 가졌는가」로만 본다.
 *
 * **작성자는 화면 상단 한 곳에만 있다.** 초안 하나에 작성자는 한 명이고, 이름은 프런트가
 * 공용 회원 조회로 해석한다(`D-62`).
 *
 * **재교정으로도 들어온다.** 개정안 화면의 「재교정」이 `?reviewRequest={id}`를 붙여 보내면
 * 리뷰어 코멘트를 함께 보여주고, 주 버튼이 「리뷰 요청」에서 「재교정 완료」로 바뀐다 —
 * 고칠 대상은 개정안 표가 아니라 이 초안의 후보어이기 때문이다.
 */
export function DictionaryDraftPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const [searchParams] = useSearchParams()
  const reexamineRequestId = searchParams.get('reviewRequest')
  const isReexamining = reexamineRequestId !== null

  const navigate = useNavigate()
  const { data, isLoading } = useDictionaryDraft(workspaceId)
  const { data: members } = useWorkspaceMembers(workspaceId)
  const currentMember = useAuthStore((state) => state.currentMember)
  const createCandidate = useCreateCandidateTerm(workspaceId)
  const updateCandidate = useUpdateCandidateTerm(workspaceId)
  const submitRevision = useSubmitDictionaryRevision(workspaceId)

  // 재교정 문맥에서만 개정안을 읽는다 — 리뷰어 코멘트를 후보어별로 붙이기 위해서다.
  const { data: revision } = useDictionaryRevision(
    workspaceId,
    isReexamining ? reexamineRequestId : '',
  )
  const reexamine = usePerformReexamine(reexamineRequestId ?? '')

  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [definitionEdit, setDefinitionEdit] = useState<{ id: string; value: string } | null>(null)
  const [directForm, setDirectForm] = useState('')
  const [newWord, setNewWord] = useState('')
  const [newType, setNewType] = useState<CandidateTermType>('SYNONYM')
  const [typeFilter, setTypeFilter] = useState('')
  const [keyword, setKeyword] = useState('')
  const [actionError, setActionError] = useState<string | null>(null)
  // 리뷰 요청을 보내기 전에 알림 받을 리뷰어를 고른다(전에는 전원 자동 지정이었다).
  const [reviewerDialogOpen, setReviewerDialogOpen] = useState(false)

  const candidates = data?.candidates ?? NO_CANDIDATES

  const visibleCandidates = useMemo(() => {
    const needle = keyword.trim().toLowerCase()
    return candidates.filter((candidate) => {
      const matchesType =
        typeFilter === '' ||
        (typeFilter === 'NONE' ? candidate.type === undefined : candidate.type === typeFilter)
      const matchesKeyword =
        needle === '' || candidate.words.some((word) => word.toLowerCase().includes(needle))
      return matchesType && matchesKeyword
    })
  }, [candidates, typeFilter, keyword])

  const selected =
    candidates.find((candidate) => candidate.id === selectedId) ?? visibleCandidates[0] ?? null

  const definitionDraft =
    definitionEdit !== null && definitionEdit.id === selected?.id
      ? definitionEdit.value
      : (selected?.proposedDefinition ?? '')

  // 후보어별 리뷰어 코멘트. 개정안 코멘트의 targetItemId가 후보어 id다(`D-63`).
  const commentsByCandidateId = useMemo(() => {
    type RevisionComment = NonNullable<typeof revision>['comments'][number]
    const map = new Map<string, RevisionComment[]>()
    if (!revision) return map
    for (const comment of revision.comments) {
      if (comment.targetItemId === undefined) continue
      const bucket = map.get(comment.targetItemId) ?? []
      bucket.push(comment)
      map.set(comment.targetItemId, bucket)
    }
    return map
  }, [revision])

  const nameByMemberId = new Map((members ?? []).map((member) => [member.id, member.name]))

  if (isLoading) return <p className="text-sm text-text-tertiary">불러오는 중…</p>

  if (!data) {
    return (
      <div>
        <ScreenTitle>사전집 초안</ScreenTitle>
        <ScreenSubtitle>
          진행 중인 사전 초안이 없습니다. 용어 추출을 실행하면 후보어가 담긴 초안이 만들어집니다.
        </ScreenSubtitle>
        <Button variant="primary" onClick={() => navigate(routes.termExtraction(workspaceId))}>
          용어 추출 실행
        </Button>
      </div>
    )
  }

  function handleAddCandidate() {
    if (!newWord.trim()) return
    setActionError(null)
    createCandidate.mutate(
      { workspaceId, form: newWord.trim(), type: newType },
      {
        onSuccess: (created) => setSelectedId(created.id),
        onError: (error) => setActionError(errorMessageOf(error, '후보어를 추가하지 못했습니다.')),
      },
    )
    setNewWord('')
  }

  function handleSelectWord(word: string) {
    if (!selected) return
    updateCandidate.mutate({ candidateId: selected.id, selectedWord: word })
  }

  function handleSaveDefinition() {
    if (!selected || definitionDraft === (selected.proposedDefinition ?? '')) return
    updateCandidate.mutate(
      { candidateId: selected.id, proposedDefinition: definitionDraft },
      { onSuccess: () => setDefinitionEdit(null) },
    )
  }

  function handleRequestReview(reviewerMemberIds: string[]) {
    setActionError(null)

    submitRevision.mutate(
      {
        workspaceId,
        title: `사전집 개정안 — 용어 ${candidates.length}건`,
        reviewerMemberIds,
      },
      {
        // 중간 탭 없이 생성된 리뷰 요청의 개정안 상세로 바로 간다.
        onSuccess: (created) => {
          setReviewerDialogOpen(false)
          navigate(routes.dictionaryRevision(workspaceId, created.reviewRequestId))
        },
        onError: (error) => setActionError(errorMessageOf(error, '리뷰를 요청하지 못했습니다.')),
      },
    )
  }

  function handleCompleteReexamine() {
    if (!revision) return
    setActionError(null)
    reexamine.mutate(
      { addressedCommentIds: revision.comments.map((comment) => comment.id) },
      {
        onSuccess: () =>
          navigate(routes.dictionaryRevision(workspaceId, reexamineRequestId ?? 'current')),
        onError: (error) => setActionError(errorMessageOf(error, '재교정을 마치지 못했습니다.')),
      },
    )
  }

  const selectedComments = selected ? (commentsByCandidateId.get(selected.id) ?? []) : []

  return (
    <div>
      <div className="mb-1 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-[10px]">
          <ScreenTitle className="mb-0">사전집 초안</ScreenTitle>
          {isReexamining && <Pill tone="warn">재교정 {(revision?.reexamineRound ?? 0) + 1}회차</Pill>}
          <span className="inline-flex items-center gap-[6px] text-xs text-text-tertiary">
            <Avatar initial={data.creatorName.charAt(0)} tone="accent" size={22} />
            작성자 <b className="font-bold text-text">{data.creatorName}</b>
          </span>
        </div>

        {isReexamining ? (
          <div className="flex gap-[10px]">
            <Button
              variant="outline"
              onClick={() =>
                navigate(routes.dictionaryRevision(workspaceId, reexamineRequestId ?? 'current'))
              }
            >
              개정안으로 돌아가기
            </Button>
            <Button
              variant="primary"
              disabled={!data.reviewReady || reexamine.isPending}
              title={data.reviewReady ? undefined : '정의가 빈 후보어가 남아 있습니다'}
              onClick={handleCompleteReexamine}
            >
              {reexamine.isPending ? '제출 중…' : '재교정 완료'}
            </Button>
          </div>
        ) : (
          <Button
            variant="primary"
            disabled={!data.reviewReady || submitRevision.isPending}
            title={
              data.reviewReady
                ? undefined
                : `정의가 빈 후보어가 ${data.missingDefinition.length}건 남아 있습니다`
            }
            onClick={() => {
              setActionError(null)
              setReviewerDialogOpen(true)
            }}
          >
            {submitRevision.isPending ? '요청 중…' : '리뷰 요청'}
          </Button>
        )}
      </div>

      <ScreenSubtitle className="mb-[18px]">
        {isReexamining
          ? '리뷰어가 요청한 부분을 고칩니다. 재교정 완료를 누르면 회차가 올라가고 기존 승인은 무효가 되어 리뷰어가 다시 봅니다.'
          : '후보어 묶음마다 대표어를 고르고 정의를 채웁니다. 전부 채우면 리뷰를 요청할 수 있고, 요청과 동시에 사전 개정안이 만들어집니다.'}
      </ScreenSubtitle>

      {actionError && <p className="mb-3 text-xs text-danger">{actionError}</p>}

      {isReexamining && revision && revision.comments.length > 0 && (
        <Banner className="mb-4 border-warn-border bg-warn-bg">
          <div className="mb-[6px] font-bold">
            변경 요청 — 코멘트 달린 후보어 {commentsByCandidateId.size}건
          </div>
          <div className="flex flex-col gap-[6px]">
            {revision.comments
              .filter((comment) => comment.targetItemId !== undefined)
              .map((comment) => {
                const term = candidates.find(
                  (candidate) => candidate.id === comment.targetItemId,
                )
                return (
                  <div key={comment.id} className="flex items-start gap-2">
                    <Pill tone="warn" className="shrink-0">
                      {term?.form ?? '삭제된 후보어'}
                    </Pill>
                    <span className="leading-[1.6]">
                      <b>{nameByMemberId.get(comment.authorId) ?? '—'}</b> · {comment.content}
                    </span>
                  </div>
                )
              })}
          </div>
        </Banner>
      )}

      {!isReexamining && !data.reviewReady && (
        <Banner className="mb-4">
          정의가 비어 있는 후보어가 <b>{data.missingDefinition.length}건</b> 남아 있어 리뷰를
          요청할 수 없습니다 —{' '}
          {data.missingDefinition
            .slice(0, 5)
            .map((candidate) => candidate.form)
            .join(', ')}
          {data.missingDefinition.length > 5 && ` 외 ${data.missingDefinition.length - 5}건`}.
        </Banner>
      )}

      <Toolbar>
        <FilterChip className="p-0">
          <select
            aria-label="유형 필터"
            value={typeFilter}
            onChange={(event) => setTypeFilter(event.target.value)}
            className="cursor-pointer bg-transparent px-3 py-[7px] text-[12.5px] text-text-secondary outline-none"
          >
            <option value="">유형 전체</option>
            <option value="NONE">미분류</option>
            {CANDIDATE_TYPES.map((type) => (
              <option key={type} value={type}>
                {TYPE_LABELS[type]}
              </option>
            ))}
          </select>
        </FilterChip>
        <TextInput
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="검색"
          className="w-[160px] py-[7px] text-[12.5px]"
        />
        <ToolbarSpacer />
        <span className="text-[11px] text-text-quaternary">
          후보 {candidates.length}건 ·{' '}
          <span
            className={cx(
              'font-bold',
              data.missingDefinition.length === 0 ? 'text-success' : 'text-warn',
            )}
          >
            정의 누락 {data.missingDefinition.length}건
          </span>
        </span>
      </Toolbar>

      <TwoCol>
        <ColFlex>
          <Card className="overflow-hidden">
            <DataTable>
              <thead>
                <tr>
                  <Th>후보 단어</Th>
                  <Th>유형</Th>
                  <Th>출현/문서</Th>
                </tr>
              </thead>
              <tbody>
                {visibleCandidates.length === 0 && (
                  <Tr>
                    <Td colSpan={3} className="py-6 text-center text-text-tertiary">
                      조건에 맞는 후보어가 없습니다.
                    </Td>
                  </Tr>
                )}
                {visibleCandidates.map((candidate) => {
                  const needsDefinition =
                    candidate.proposedDefinition === undefined ||
                    candidate.proposedDefinition.trim() === ''
                  const commentCount = commentsByCandidateId.get(candidate.id)?.length ?? 0
                  const variants = candidate.words.filter((word) => word !== candidate.form)

                  return (
                    <Tr
                      key={candidate.id}
                      clickable
                      onClick={() => {
                        setSelectedId(candidate.id)
                        setDirectForm('')
                      }}
                      className={cx(
                        candidate.id === selected?.id &&
                          'bg-accent-bg-strong shadow-[inset_3px_0_0_var(--color-accent)]',
                      )}
                    >
                      <Td>
                        <span className="font-bold text-text">{candidate.form}</span>
                        {variants.length > 0 && (
                          <span className="text-text-quaternary">, {variants.join(', ')}</span>
                        )}
                        {needsDefinition && (
                          <Pill tone="warn" className="ml-[6px]">
                            정의 필요
                          </Pill>
                        )}
                        {commentCount > 0 && (
                          <Pill tone="warn" className="ml-[6px]">
                            💬 {commentCount}
                          </Pill>
                        )}
                      </Td>
                      <Td>{candidate.type ? TYPE_LABELS[candidate.type] : '미분류'}</Td>
                      <Td>
                        {candidate.occurrenceCount} / {candidate.occurredDocumentIds.length}
                      </Td>
                    </Tr>
                  )
                })}
              </tbody>
            </DataTable>

            <div className="flex items-center gap-2 border-t border-border-soft px-4 py-3">
              <TextInput
                value={newWord}
                onChange={(event) => setNewWord(event.target.value)}
                onKeyDown={(event) => event.key === 'Enter' && handleAddCandidate()}
                placeholder="새 후보 단어"
                className="flex-1 py-[7px] text-[12.5px]"
              />
              <select
                value={newType}
                onChange={(event) => setNewType(event.target.value as CandidateTermType)}
                aria-label="새 후보 유형"
                className="rounded-sm border border-border-strong px-2 py-[7px] text-[12.5px] text-text"
              >
                {CANDIDATE_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {TYPE_LABELS[type]}
                  </option>
                ))}
              </select>
              <Button
                size="sm"
                variant="primary"
                onClick={handleAddCandidate}
                disabled={createCandidate.isPending}
              >
                + 추가
              </Button>
            </div>
          </Card>
        </ColFlex>

        {selected && (
          <DetailPanel className="w-[420px]">
            <div className="flex items-baseline justify-between gap-2">
              <div className="flex items-center gap-2">
                <span className="font-display text-base font-bold">{selected.form}</span>
                <Pill tone="neutral">
                  {selected.type ? TYPE_LABELS[selected.type] : '미분류'}
                </Pill>
              </div>
              <span className="shrink-0 text-[11px] text-text-quaternary">
                출현 {selected.occurrenceCount}회 · 문서 {selected.occurredDocumentIds.length}건
              </span>
            </div>

            <div>
              <FieldLabel required>대표어</FieldLabel>
              <div className="flex flex-col gap-[7px]">
                {selected.words.map((word) => {
                  const isSelected = selected.form === word
                  return (
                    <button
                      key={word}
                      type="button"
                      onClick={() => handleSelectWord(word)}
                      className={cx(
                        'flex cursor-pointer items-center gap-2 rounded-sm px-1 py-[3px] text-left text-[13px] hover:bg-surface-muted',
                        isSelected ? 'text-text' : 'text-text-secondary',
                      )}
                    >
                      <RadioDot checked={isSelected} />
                      {word}
                    </button>
                  )
                })}

                <div className="flex items-center gap-2 px-1 text-[13px] text-text-secondary">
                  <RadioDot checked={false} />
                  직접 입력
                  <TextInput
                    value={directForm}
                    onChange={(event) => setDirectForm(event.target.value)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' && directForm.trim() !== '') {
                        handleSelectWord(directForm.trim())
                      }
                    }}
                    placeholder="Enter로 적용"
                    className="h-[28px] flex-1 bg-surface-muted py-0 text-xs"
                  />
                </div>
              </div>
              <p className="mt-[5px] text-[10.5px] text-text-quaternary">
                고른 표기만 개정안 행으로 올라갑니다 · 나머지 표기는 묶음으로 남습니다
              </p>
            </div>

            <div>
              <FieldLabel required>정의</FieldLabel>
              <TextArea
                value={definitionDraft}
                onChange={(event) =>
                  selected && setDefinitionEdit({ id: selected.id, value: event.target.value })
                }
                onBlur={handleSaveDefinition}
                rows={3}
                placeholder="정의를 입력하세요"
                className={cx(
                  'min-h-16 text-[12.5px]',
                  definitionDraft.trim() === ''
                    ? 'border-[1.5px] border-warn-border bg-warn-bg'
                    : 'bg-surface-muted text-text-secondary',
                )}
              />
              <p
                className={cx(
                  'mt-[5px] text-[10.5px]',
                  definitionDraft.trim() === ''
                    ? 'font-semibold text-warn'
                    : 'text-text-quaternary',
                )}
              >
                {definitionDraft.trim() === ''
                  ? '정의가 비어 있어 리뷰 요청이 막혀 있습니다'
                  : 'AI 초안 · 입력창 밖을 클릭하면 저장됩니다'}
              </p>
            </div>

            {selectedComments.length > 0 && (
              <div>
                <FieldLabel>이 후보어에 달린 리뷰어 코멘트</FieldLabel>
                <div className="flex flex-col gap-2">
                  {selectedComments.map((comment) => {
                    const name = nameByMemberId.get(comment.authorId) ?? '—'
                    return (
                      <CommentCard
                        key={comment.id}
                        name={name}
                        initial={name.charAt(0)}
                        time={toRelativeTime(comment.createdAt)}
                        text={comment.content}
                      />
                    )
                  })}
                </div>
              </div>
            )}

            <div>
              <FieldLabel>근거 문장</FieldLabel>
              <div className="flex flex-col gap-2">
                {(selected.contextSnippets ?? []).length === 0 && (
                  <p className="text-[11px] text-text-quaternary">
                    직접 등록한 후보라 근거 문장이 없습니다.
                  </p>
                )}
                {(selected.contextSnippets ?? []).map((snippet, index) => (
                  <QuoteBlock
                    key={index}
                    text={snippet}
                    source="추출 근거 · 출처 문서는 응답에 없습니다"
                  />
                ))}
              </div>
            </div>
          </DetailPanel>
        )}
      </TwoCol>

      <ReviewerSelectDialog
        open={reviewerDialogOpen}
        candidates={(members ?? []).map((member) => ({
          memberId: member.id,
          name: member.name,
        }))}
        excludeMemberId={currentMember?.id}
        title="리뷰어 지정"
        confirmLabel="리뷰 요청"
        isPending={submitRevision.isPending}
        error={submitRevision.isError ? actionError : null}
        onSubmit={handleRequestReview}
        onClose={() => setReviewerDialogOpen(false)}
      />
    </div>
  )
}

function errorMessageOf(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}
