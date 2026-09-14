import { useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Button,
  Card,
  Checkbox,
  ColFlex,
  DataTable,
  DetailPanel,
  FieldLabel,
  FilterChip,
  Pill,
  QuoteBlock,
  RadioDot,
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
import { ApiError } from '../../shared/api/httpClient'
import { useAuthStore } from '../../shared/stores/authStore'
import { useCandidates } from '../../features/dictionary/hooks/useCandidates'
import { useCreateCandidateTerm } from '../../features/dictionary/hooks/useCreateCandidateTerm'
import { useUpdateCandidateTerm } from '../../features/dictionary/hooks/useUpdateCandidateTerm'
import { useDecideCandidateTerm } from '../../features/dictionary/hooks/useDecideCandidateTerm'
import { useBulkDecideCandidateTerms } from '../../features/dictionary/hooks/useBulkDecideCandidateTerms'
import {
  useCandidateExamineProgress,
  useSubmitDictionaryRevision,
} from '../../features/dictionary/hooks/useSubmitDictionaryRevision'
import { useWorkspaceMembers } from '../../features/member/hooks/useWorkspaceMembers'
import type { CandidateTermType } from '../../features/dictionary/model/types'

const TYPE_LABELS: Record<CandidateTermType, string> = {
  SYNONYM: '동의어',
  HOMOGRAPH: '동형이의',
  VARIANT: '표기 변형',
}
const CANDIDATE_TYPES = Object.keys(TYPE_LABELS) as CandidateTermType[]

const STATUS_LABELS: Record<string, string> = {
  PENDING: '대기',
  REGISTRATION_APPROVED: '등재 승인',
  MERGED_AS_SYNONYM: '동의어 편입',
  REJECTED: '거절',
  ON_HOLD: '보류',
  KEPT: '승계 유지',
}

// ui/main.js renderDraftScreen() 이식 — 후보 표(넓게) + 상세 패널(420px) master-detail.
//
// **분류(type)는 사람이 등록할 때만 붙는다** — 추출이 만든 후보어에는 없어서 "미분류"로
// 표시한다(T-INT-11 결정 1). **근거 문장에 출처·분리 표시가 없다** — 백엔드가 주는 건
// contextSnippets 문자열뿐이라 어느 문서 몇 문단인지 복원할 수 없다(결정 3).
//
// **(2026-09-14 디자인 정합)** 프로토타입에만 있던 셋을 살렸다 — 헤더의 「개정안 제출」
// (실 API: 교정완료 → 리뷰 요청), 툴바의 유형·검색 필터, 표 발치의 일괄 판정.
// 「직접 입력」 표준어 옵션도 프로토타입대로 되살렸다(수정 API가 form을 받는다).
export function DictionaryDraftPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const navigate = useNavigate()
  const { data: candidates, isLoading } = useCandidates(workspaceId)
  const { data: progress } = useCandidateExamineProgress(workspaceId)
  const { data: members } = useWorkspaceMembers(workspaceId)
  const createCandidate = useCreateCandidateTerm(workspaceId)
  const updateCandidate = useUpdateCandidateTerm(workspaceId)
  const decideCandidate = useDecideCandidateTerm(workspaceId)
  const bulkDecide = useBulkDecideCandidateTerms(workspaceId)
  const submitRevision = useSubmitDictionaryRevision(workspaceId)
  const currentMember = useAuthStore((state) => state.currentMember)

  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [checkedIds, setCheckedIds] = useState<string[]>([])
  // 정의 입력은 "어느 후보의 편집인지"를 함께 들고 있다 — 후보를 옮기면 그 값이 버려지고
  // 서버 값이 다시 보인다. effect로 state를 되채우지 않기 위한 방식이다.
  const [definitionEdit, setDefinitionEdit] = useState<{ id: string; value: string } | null>(null)
  const [directForm, setDirectForm] = useState('')
  const [newWord, setNewWord] = useState('')
  const [newType, setNewType] = useState<CandidateTermType>('SYNONYM')
  const [typeFilter, setTypeFilter] = useState('')
  const [keyword, setKeyword] = useState('')
  const [submitError, setSubmitError] = useState<string | null>(null)

  const visibleCandidates = useMemo(() => {
    const needle = keyword.trim().toLowerCase()
    return (candidates ?? []).filter((candidate) => {
      const matchesType =
        typeFilter === '' ||
        (typeFilter === 'NONE' ? candidate.type === undefined : candidate.type === typeFilter)
      const matchesKeyword =
        needle === '' || candidate.words.some((word) => word.toLowerCase().includes(needle))
      return matchesType && matchesKeyword
    })
  }, [candidates, typeFilter, keyword])

  const selected =
    candidates?.find((candidate) => candidate.id === selectedId) ?? visibleCandidates[0] ?? null

  const definitionDraft =
    definitionEdit !== null && definitionEdit.id === selected?.id
      ? definitionEdit.value
      : (selected?.proposedDefinition ?? '')

  if (isLoading) return <p className="text-sm text-text-tertiary">불러오는 중…</p>

  if (!candidates || candidates.length === 0) {
    return (
      <div>
        <ScreenTitle>사전집 초안 — 후보 작성</ScreenTitle>
        <p className="mb-5 text-[12.5px] text-text-tertiary">
          교정 중인 사전 초안이 없습니다. 용어 추출을 실행하면 후보어가 담긴 초안이 만들어집니다.
        </p>
        <Button variant="primary" onClick={() => navigate(routes.termExtraction(workspaceId))}>
          용어 추출 실행
        </Button>
      </div>
    )
  }

  function handleAddCandidate() {
    if (!newWord.trim() || !currentMember) return
    createCandidate.mutate(
      {
        workspaceId,
        form: newWord.trim(),
        type: newType,
        ownerName: currentMember.displayName,
      },
      { onSuccess: (created) => setSelectedId(created.id) },
    )
    setNewWord('')
  }

  function handleSelectWord(word: string) {
    if (!selected) return
    updateCandidate.mutate({
      candidateId: selected.id,
      selectedWord: word,
      ownerName: selected.ownerName,
    })
  }

  function handleSaveDefinition() {
    if (!selected || definitionDraft === (selected.proposedDefinition ?? '')) return
    updateCandidate.mutate(
      {
        candidateId: selected.id,
        proposedDefinition: definitionDraft,
        ownerName: selected.ownerName,
      },
      { onSuccess: () => setDefinitionEdit(null) },
    )
  }

  function toggleChecked(id: string) {
    setCheckedIds((current) =>
      current.includes(id) ? current.filter((value) => value !== id) : [...current, id],
    )
  }

  function handleSubmitRevision() {
    setSubmitError(null)
    const reviewerMemberIds = (members ?? [])
      .filter((member) => member.id !== currentMember?.id)
      .map((member) => member.id)

    submitRevision.mutate(
      {
        workspaceId,
        title: `사전집 개정안 — 후보어 ${candidates?.length ?? 0}건`,
        reviewerMemberIds,
      },
      {
        onSuccess: () => navigate(routes.dictionaryRevision(workspaceId, 'current')),
        onError: (error) =>
          setSubmitError(
            error instanceof ApiError ? error.message : (error as Error).message,
          ),
      },
    )
  }

  const pendingCount = progress?.pending ?? 0

  return (
    <div>
      <div className="mb-1 flex items-center justify-between gap-3">
        <ScreenTitle className="mb-0">사전집 초안 — 후보 작성</ScreenTitle>
        <Button
          variant="primary"
          size="sm"
          disabled={pendingCount > 0 || submitRevision.isPending}
          title={pendingCount > 0 ? `미판정 후보어 ${pendingCount}건이 남아 있습니다` : undefined}
          onClick={handleSubmitRevision}
        >
          {submitRevision.isPending ? '제출 중…' : '개정안 제출'}
        </Button>
      </div>
      <p className="mb-[18px] text-xs text-text-quaternary">
        담당자를 지정해 표준어와 정의를 채우면, 개정안 제출 후 다른 멤버가 사전집 개정안에서
        승인합니다.
      </p>

      {submitError && <p className="mb-3 text-xs text-danger">{submitError}</p>}

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
          후보 {candidates.length}건 · 미판정 {pendingCount}건
        </span>
      </Toolbar>

      <TwoCol>
        <ColFlex>
          <Card className="overflow-hidden">
            <DataTable>
              <thead>
                <tr>
                  <Th className="w-[14px]" />
                  <Th>후보 단어</Th>
                  <Th>유형</Th>
                  <Th>출현/문서</Th>
                  <Th>판정</Th>
                  <Th>작성자</Th>
                </tr>
              </thead>
              <tbody>
                {visibleCandidates.map((candidate) => (
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
                    <Td onClick={(event) => event.stopPropagation()}>
                      <Checkbox
                        checked={checkedIds.includes(candidate.id)}
                        onChange={() => toggleChecked(candidate.id)}
                        label={`${candidate.form} 선택`}
                      />
                    </Td>
                    <Td className="font-semibold text-text">{candidate.words.join(', ')}</Td>
                    <Td>{candidate.type ? TYPE_LABELS[candidate.type] : '미분류'}</Td>
                    <Td>
                      {candidate.occurrenceCount} / {candidate.occurredDocumentIds.length}
                    </Td>
                    <Td>
                      {candidate.status === 'PENDING' ? (
                        <span className="text-text-quaternary">대기</span>
                      ) : (
                        <Pill tone={candidate.status === 'REJECTED' ? 'danger' : 'success'}>
                          {STATUS_LABELS[candidate.status] ?? candidate.status}
                        </Pill>
                      )}
                    </Td>
                    <Td>{candidate.ownerName}</Td>
                  </Tr>
                ))}
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

            {checkedIds.length > 0 && (
              <div className="flex items-center justify-between gap-3 border-t border-border-soft bg-surface-muted px-4 py-3">
                <span className="text-[11.5px] text-text-secondary">
                  {checkedIds.length}건 선택됨
                </span>
                <div className="flex gap-2">
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={bulkDecide.isPending}
                    onClick={() =>
                      bulkDecide.mutate(
                        { workspaceId, candidateIds: checkedIds, decision: 'ON_HOLD' },
                        { onSuccess: () => setCheckedIds([]) },
                      )
                    }
                  >
                    일괄 보류
                  </Button>
                  <Button
                    size="sm"
                    variant="primary"
                    disabled={bulkDecide.isPending}
                    onClick={() =>
                      bulkDecide.mutate(
                        {
                          workspaceId,
                          candidateIds: checkedIds,
                          decision: 'REGISTRATION_APPROVED',
                        },
                        { onSuccess: () => setCheckedIds([]) },
                      )
                    }
                  >
                    일괄 등재 승인
                  </Button>
                </div>
              </div>
            )}

            {bulkDecide.data && bulkDecide.data.failed.length > 0 && (
              <p className="border-t border-border-soft px-4 py-3 text-[11.5px] text-danger">
                {bulkDecide.data.failed.length}건은 판정하지 못했습니다 —{' '}
                {bulkDecide.data.failed[0].message}
              </p>
            )}
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
              <FieldLabel>표준어</FieldLabel>
              <div className="flex flex-col gap-[7px]">
                {selected.words.map((word) => {
                  const isSelected = (selected.selectedWord ?? selected.words[0]) === word
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
            </div>

            <div>
              <FieldLabel>정의</FieldLabel>
              <TextArea
                value={definitionDraft}
                onChange={(event) =>
                  selected &&
                  setDefinitionEdit({ id: selected.id, value: event.target.value })
                }
                onBlur={handleSaveDefinition}
                rows={3}
                className="min-h-16 bg-surface-muted text-[12.5px] text-text-secondary"
              />
              <p className="mt-[5px] text-[10.5px] text-text-quaternary">
                AI 초안 · 입력창 밖을 클릭하면 저장됩니다
              </p>
            </div>

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

            <div className="flex flex-col gap-[9px] border-t border-border-soft pt-[14px]">
              <div className="flex items-center gap-[10px]">
                <FieldLabel className="mb-0">작성 담당자</FieldLabel>
                <span className="rounded-sm border border-border-strong bg-surface px-[10px] py-[6px] text-xs">
                  {selected.ownerName}
                </span>
              </div>
              <div className="flex gap-2">
                <Button
                  size="sm"
                  variant="primary"
                  disabled={decideCandidate.isPending || !selected.proposedDefinition}
                  title={
                    selected.proposedDefinition
                      ? undefined
                      : '정의를 먼저 채워야 승인할 수 있습니다'
                  }
                  onClick={() =>
                    decideCandidate.mutate({
                      kind: 'approve',
                      candidateId: selected.id,
                      ownerName: selected.ownerName,
                    })
                  }
                >
                  등재 승인
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  disabled={decideCandidate.isPending}
                  onClick={() =>
                    decideCandidate.mutate({
                      kind: 'hold',
                      candidateId: selected.id,
                      ownerName: selected.ownerName,
                    })
                  }
                >
                  보류
                </Button>
              </div>
            </div>
          </DetailPanel>
        )}
      </TwoCol>
    </div>
  )
}
