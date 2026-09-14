import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Button, Card, Pill } from '../../shared/ui'
import { useCandidates } from '../../features/dictionary/hooks/useCandidates'
import { useCreateCandidateTerm } from '../../features/dictionary/hooks/useCreateCandidateTerm'
import { useUpdateCandidateTerm } from '../../features/dictionary/hooks/useUpdateCandidateTerm'
import { useAuthStore } from '../../shared/stores/authStore'
import { cx } from '../../shared/lib/cx'
import type { CandidateTermListItem } from '../../features/dictionary/model/fixtures'

const CANDIDATE_TYPES: CandidateTermListItem['type'][] = ['동의어', '동형이의', '표기 변형']

// ui/main.js renderDraftScreen() 이식. 표준어 선택·정의 수정·후보 직접 등록까지 실제로
// 동작한다 — updateCandidateTerm/createCandidateTerm mutation(인메모리 반영).
export function DictionaryDraftPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const { data: candidates, isLoading } = useCandidates(workspaceId)
  const createCandidate = useCreateCandidateTerm(workspaceId)
  const updateCandidate = useUpdateCandidateTerm(workspaceId)
  const currentMember = useAuthStore((state) => state.currentMember)

  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [definitionDraft, setDefinitionDraft] = useState('')
  const [newWord, setNewWord] = useState('')
  const [newType, setNewType] = useState<CandidateTermListItem['type']>('동의어')

  const selected =
    candidates?.find((c) => c.id === selectedId) ?? candidates?.[0] ?? null

  // 선택한 후보가 바뀔 때마다 정의 입력창을 그 후보의 값으로 다시 채운다.
  useEffect(() => {
    setDefinitionDraft(selected?.proposedDefinition ?? '')
  }, [selected?.id, selected?.proposedDefinition])

  if (isLoading) return <p className="text-sm text-text-tertiary">불러오는 중…</p>

  function handleAddCandidate() {
    if (!newWord.trim() || !currentMember) return
    createCandidate.mutate(
      {
        form: newWord.trim(),
        type: newType,
        ownerId: currentMember.id,
        ownerName: currentMember.displayName,
      },
      { onSuccess: (created) => setSelectedId(created.id) },
    )
    setNewWord('')
  }

  function handleSelectWord(word: string) {
    if (!selected) return
    updateCandidate.mutate({ candidateId: selected.id, selectedWord: word })
  }

  function handleSaveDefinition() {
    if (!selected) return
    updateCandidate.mutate({ candidateId: selected.id, proposedDefinition: definitionDraft })
  }

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

          <div className="flex items-center gap-2 border-t border-border-soft px-4 py-3">
            <input
              value={newWord}
              onChange={(event) => setNewWord(event.target.value)}
              onKeyDown={(event) => event.key === 'Enter' && handleAddCandidate()}
              placeholder="새 후보 단어"
              className="flex-1 rounded-sm border border-border-strong px-3 py-[7px] text-[12.5px] text-text placeholder:text-text-quaternary"
            />
            <select
              value={newType}
              onChange={(event) =>
                setNewType(event.target.value as CandidateTermListItem['type'])
              }
              className="rounded-sm border border-border-strong px-2 py-[7px] text-[12.5px] text-text"
            >
              {CANDIDATE_TYPES.map((type) => (
                <option key={type} value={type}>
                  {type}
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
                {selected.words.map((word) => {
                  const isSelected = (selected.selectedWord ?? selected.words[0]) === word
                  return (
                    <div
                      key={word}
                      onClick={() => handleSelectWord(word)}
                      className={cx(
                        'flex cursor-pointer items-center gap-2 rounded-sm px-1 py-[3px] text-[13px] hover:bg-surface-muted',
                        isSelected ? 'font-semibold text-text' : 'text-text-secondary',
                      )}
                    >
                      <span
                        className={cx(
                          'inline-block h-[15px] w-[15px] shrink-0 rounded-full border-[1.5px]',
                          isSelected ? 'border-accent bg-accent' : 'border-text-disabled',
                        )}
                      />
                      {word}
                    </div>
                  )
                })}
              </div>
            </div>

            <div>
              <p className="mb-2 text-xs font-bold text-text">정의</p>
              <textarea
                value={definitionDraft}
                onChange={(event) => setDefinitionDraft(event.target.value)}
                onBlur={handleSaveDefinition}
                rows={3}
                className="min-h-16 w-full rounded-sm border border-border-strong bg-surface-muted px-3 py-[10px] text-[12.5px] leading-[1.6] text-text-secondary"
              />
              <p className="mt-[5px] text-[10.5px] text-text-quaternary">
                AI 초안 · 입력창 밖을 클릭하면 저장됩니다
              </p>
            </div>

            <div>
              <p className="mb-2 text-xs font-bold text-text">근거 문장</p>
              <div className="flex flex-col gap-2">
                {selected.quotes.length === 0 && (
                  <p className="text-[11px] text-text-quaternary">
                    직접 등록한 후보라 근거 문장이 없습니다.
                  </p>
                )}
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
                {selected.ownerName}
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
