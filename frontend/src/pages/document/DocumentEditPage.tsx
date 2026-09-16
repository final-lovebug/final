import { type FormEvent, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Banner,
  Button,
  FieldLabel,
  Markdown,
  ScreenTitle,
  Segmented,
  TextArea,
  TextInput,
} from '../../shared/ui'
import { cx } from '../../shared/lib/cx'
import { routes } from '../../shared/config/routes'
import { ApiError } from '../../shared/api/httpClient'
import { DOCUMENT_CONTENT_MAX_LENGTH } from '../../features/document/api/createDocument'
import { useDocument } from '../../features/document/hooks/useDocument'
import { useEditDocument } from '../../features/document/hooks/useEditDocument'
import { useLabels } from '../../features/document/hooks/useLabels'
import {
  isSameLabelName,
  resolveExistingLabelName,
} from '../../features/document/model/labelName'

// 이미 있는 문서를 고치는 화면. 업로드 화면(`DocumentUploadPage`)과 같은 폼 규격을 쓰되
// 파일 드롭존은 두지 않는다 — 편집은 이미 있는 본문을 손보는 일이라 파일로 통째로 갈아
// 끼우는 동작과 섞으면 「어느 쪽이 저장되는가」가 모호해진다.
//
// **제목·라벨과 본문은 백엔드에서 경로가 다르다**(docs/API.md «문서 수정» / «본문 편집»).
// 본문 편집만 새 버전을 발행하기 때문이다(`G-9`). 화면은 한 폼으로 받되 저장할 때
// 바뀐 쪽만 골라 보낸다 — 제목만 고쳤는데 버전이 오르면 이력이 거짓말을 한다.
export function DocumentEditPage() {
  const { workspaceId = '', documentId = '' } = useParams<{
    workspaceId: string
    documentId: string
  }>()
  const navigate = useNavigate()
  const { data: document, isLoading, isError } = useDocument(workspaceId, documentId)
  const { data: labels } = useLabels(workspaceId)
  const editDocument = useEditDocument(workspaceId, documentId)

  // 문서를 받아온 뒤에야 초기값을 알 수 있다 — 받아오기 전에는 null로 두고 원본을 그대로
  // 보여주다가, 사용자가 처음 고칠 때 초안 상태가 생긴다(useEffect로 채우면 문서가 갱신될
  // 때마다 사용자가 치던 내용을 덮어쓴다).
  const [draft, setDraft] = useState<DocumentDraft | null>(null)
  const [newLabel, setNewLabel] = useState('')
  const [contentView, setContentView] = useState<'edit' | 'preview'>('edit')

  const knownLabelNames = useMemo(() => (labels ?? []).map((label) => label.name), [labels])

  if (isLoading) return <p className="text-sm text-text-tertiary">불러오는 중…</p>
  if (isError || !document) {
    return <p className="text-sm text-danger">문서를 찾을 수 없습니다.</p>
  }

  const original: DocumentDraft = {
    title: document.title,
    content: document.content,
    labels: document.labels ?? [],
  }
  const form = draft ?? original

  function patch(next: Partial<DocumentDraft>) {
    setDraft({ ...form, ...next })
  }

  function toggleLabel(name: string) {
    patch({
      labels: form.labels.some((label) => isSameLabelName(label, name))
        ? form.labels.filter((label) => !isSameLabelName(label, name))
        : form.labels.length >= 5
          ? form.labels
          : [...form.labels, name],
    })
  }

  // 업로드 화면과 같은 규칙 — 라벨명은 대소문자를 구분하지 않으므로(`D-94`) 이미 있는
  // 표기를 찾아 그쪽으로 바꿔 보여준다.
  function addNewLabel() {
    const typed = newLabel.trim()
    if (typed === '' || form.labels.length >= 5) return

    const name = resolveExistingLabelName(typed, knownLabelNames) ?? typed
    if (form.labels.some((label) => isSameLabelName(label, name))) {
      setNewLabel('')
      return
    }

    patch({ labels: [...form.labels, name] })
    setNewLabel('')
  }

  const contentChanged = form.content !== original.content
  const metadataChanged =
    form.title.trim() !== original.title || labelsChanged(form.labels, original.labels)
  const changed = contentChanged || metadataChanged

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    // 미리보기 중에는 textarea가 언마운트돼 required 검사가 돌지 않는다 — 편집으로 되돌린다.
    if (form.content.trim() === '') {
      setContentView('edit')
      return
    }
    if (!changed) {
      navigate(routes.documentDetail(workspaceId, documentId))
      return
    }

    editDocument.mutate(
      {
        content: contentChanged ? form.content : undefined,
        metadata: metadataChanged
          ? { title: form.title.trim(), labels: form.labels }
          : undefined,
      },
      { onSuccess: () => navigate(routes.documentDetail(workspaceId, documentId)) },
    )
  }

  const labelChoices = [
    ...knownLabelNames,
    ...form.labels.filter(
      (name) => !knownLabelNames.some((known) => isSameLabelName(known, name)),
    ),
  ]

  return (
    <form onSubmit={handleSubmit} className="max-w-[640px]">
      <ScreenTitle>문서 편집</ScreenTitle>

      {/* 편집이 왜 배지를 바꾸는지를 저장 전에 알려 준다 — 저장하고 나서 상세 화면의
          「재검사 필요」를 보고 놀라지 않도록 한다. */}
      <Banner className="mb-[22px]">
        본문을 고쳐 저장하면 <strong>v{document.currentVersionNo + 1}</strong>이 바로 발행되고
        문서가 「재검사 필요」로 바뀝니다. 대조·리뷰를 거치지 않으므로, 최신 사전집에 맞추려면
        저장한 뒤 상세 화면에서 「최신 사전집으로 갱신」을 실행하세요. 제목·라벨만 고치면 버전은
        오르지 않습니다.
      </Banner>

      <div className="flex flex-col gap-4">
        <div>
          <FieldLabel>라벨</FieldLabel>
          <div className="flex flex-wrap gap-2">
            {labelChoices.map((name) => {
              const active = form.labels.some((label) => isSameLabelName(label, name))
              return (
                <button
                  key={name}
                  type="button"
                  onClick={() => toggleLabel(name)}
                  className={cx(
                    'cursor-pointer rounded-[16px] px-[14px] py-[6px] text-[12.5px]',
                    active
                      ? 'border-[1.5px] border-accent bg-accent-bg font-semibold text-accent-strong'
                      : 'border border-border-strong text-text-tertiary hover:bg-bg',
                  )}
                >
                  {name}
                </button>
              )
            })}
          </div>
          <div className="mt-2 flex gap-2">
            <TextInput
              value={newLabel}
              onChange={(event) => setNewLabel(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  event.preventDefault()
                  addNewLabel()
                }
              }}
              placeholder="새 라벨 이름"
              className="flex-1 py-[7px] text-[12.5px]"
            />
            <Button variant="outline" size="sm" onClick={addNewLabel}>
              추가
            </Button>
          </div>
          <p className="mt-2 text-[11px] text-text-quaternary">
            선택을 모두 해제하면 라벨이 떨어집니다 · 최대 5개
          </p>
        </div>

        <div>
          <FieldLabel required>문서명</FieldLabel>
          <TextInput
            value={form.title}
            onChange={(event) => patch({ title: event.target.value })}
            required
            maxLength={100}
          />
        </div>

        <div>
          <div className="flex items-center justify-between">
            <FieldLabel required>본문</FieldLabel>
            {/* FieldLabel의 아래 여백(mb-2)과 맞춰야 두 요소의 가운데가 나란해진다. */}
            <div className="mb-2">
              <Segmented
                options={[
                  { value: 'edit', label: '편집' },
                  { value: 'preview', label: '미리보기' },
                ]}
                value={contentView}
                onChange={setContentView}
              />
            </div>
          </div>
          {contentView === 'edit' ? (
            <TextArea
              value={form.content}
              onChange={(event) => patch({ content: event.target.value })}
              maxLength={DOCUMENT_CONTENT_MAX_LENGTH}
              required
              className="min-h-[320px]"
              placeholder="마크다운(.md) 문법을 그대로 씁니다"
            />
          ) : (
            // 미리보기는 상세 화면과 같은 뷰어·같은 글자 규격으로 그린다.
            <div className="min-h-[320px] rounded-sm border border-border-strong bg-surface px-4 py-3">
              {form.content.trim() === '' ? (
                <p className="text-[12.5px] text-text-quaternary">미리볼 본문이 없습니다.</p>
              ) : (
                <Markdown
                  source={form.content}
                  className="text-[13.5px] leading-[1.9] text-text-secondary"
                />
              )}
            </div>
          )}
          <div className="mt-1 flex justify-between gap-4 text-[11px] text-text-quaternary">
            <span>{contentChanged ? '본문이 바뀌었습니다 · 저장하면 새 버전이 발행됩니다' : ''}</span>
            <span className="shrink-0">
              {form.content.length.toLocaleString()} /{' '}
              {DOCUMENT_CONTENT_MAX_LENGTH.toLocaleString()}
            </span>
          </div>
        </div>
      </div>

      {editDocument.isError && (
        <p className="mt-4 text-xs text-danger">
          {editDocument.error instanceof ApiError
            ? editDocument.error.message
            : (editDocument.error as Error).message}
        </p>
      )}

      <div className="mt-[26px] flex justify-end gap-[10px]">
        <Button
          variant="outline"
          onClick={() => navigate(routes.documentDetail(workspaceId, documentId))}
        >
          취소
        </Button>
        <Button type="submit" variant="primary" disabled={editDocument.isPending || !changed}>
          {editDocument.isPending ? '저장 중…' : '저장'}
        </Button>
      </div>
    </form>
  )
}

interface DocumentDraft {
  title: string
  content: string
  labels: string[]
}

/** 순서가 달라도 같은 라벨 묶음이면 수정 요청을 보내지 않는다 — 서버는 통째로 교체하므로
 *  같은 집합을 다시 보내도 결과는 같지만, 불필요한 요청과 `updatedAt` 갱신이 남는다. */
function labelsChanged(next: readonly string[], original: readonly string[]): boolean {
  if (next.length !== original.length) return true

  return next.some((label) => !original.some((before) => isSameLabelName(before, label)))
}
