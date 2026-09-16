import { type ChangeEvent, type FormEvent, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
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
import { useAuthStore } from '../../shared/stores/authStore'
import { DOCUMENT_CONTENT_MAX_LENGTH } from '../../features/document/api/createDocument'
import { useCreateDocument } from '../../features/document/hooks/useCreateDocument'
import { useLabels } from '../../features/document/hooks/useLabels'
import {
  isSameLabelName,
  resolveExistingLabelName,
} from '../../features/document/model/labelName'

const ACCEPTED_EXTENSIONS = ['.md', '.txt']
/** 본문 상한이 10,000자라 파일도 그 언저리를 넘을 이유가 없다 — ui 안내 문구와 맞춘다. */
const MAX_FILE_BYTES = 10 * 1024 * 1024

// ui/main.js renderUploadScreen() 이식.
//
// 프로토타입의 드롭존·라벨 칩·파일명 칸은 전부 장식이었다. 여기서는 실제로 동작한다 —
// 파일을 고르거나 끌어다 놓으면 txt/md를 읽어 본문 칸을 채우고(문서명이 비어 있으면
// 파일명에서 따온다), 라벨은 선택한 것을 생성 요청에 함께 보낸다. 백엔드에 독립 라벨
// 생성 API가 없어(T-INT-10) **여기가 라벨이 생기는 유일한 경로**이기도 해서, 목록에 없는
// 이름을 직접 적어 넣을 수도 있게 했다.
export function DocumentUploadPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const navigate = useNavigate()
  const currentMember = useAuthStore((state) => state.currentMember)
  const createDocument = useCreateDocument()
  const { data: labels } = useLabels(workspaceId)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [fileName, setFileName] = useState('')
  const [selectedLabels, setSelectedLabels] = useState<string[]>([])
  const [newLabel, setNewLabel] = useState('')
  const [fileError, setFileError] = useState<string | null>(null)
  const [dragging, setDragging] = useState(false)
  // 마크다운으로 올린 본문이 상세 화면에서 어떻게 보일지 올리기 전에 확인할 수 있게 한다.
  const [contentView, setContentView] = useState<'edit' | 'preview'>('edit')

  const knownLabelNames = (labels ?? []).map((label) => label.name)

  async function readFile(file: File) {
    setFileError(null)
    const lowered = file.name.toLowerCase()
    if (!ACCEPTED_EXTENSIONS.some((extension) => lowered.endsWith(extension))) {
      setFileError('txt 또는 md 파일만 올릴 수 있습니다.')
      return
    }
    if (file.size > MAX_FILE_BYTES) {
      setFileError('파일이 너무 큽니다(최대 10MB).')
      return
    }

    const text = await file.text()
    if (text.length > DOCUMENT_CONTENT_MAX_LENGTH) {
      setFileError(
        `본문이 ${DOCUMENT_CONTENT_MAX_LENGTH.toLocaleString()}자를 넘습니다(${text.length.toLocaleString()}자).`,
      )
      return
    }

    setFileName(file.name)
    setContent(text)
    if (title.trim() === '') {
      setTitle(file.name.replace(/\.(md|txt)$/i, ''))
    }
  }

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (file) void readFile(file)
  }

  function toggleLabel(name: string) {
    setSelectedLabels((current) =>
      current.some((label) => isSameLabelName(label, name))
        ? current.filter((label) => !isSameLabelName(label, name))
        : current.length >= 5
          ? current
          : [...current, name],
    )
  }

  // 라벨명은 대소문자를 구분하지 않는다(`D-94`). `api`가 이미 있는데 `API`를 치면 서버는 기존
  // 라벨을 재사용하고 최초 표기(`api`)를 돌려주므로, 화면에서도 미리 그 표기로 바꿔 보여 준다 —
  // 그러지 않으면 방금 만든 줄 알았던 라벨이 응답에서 다른 이름으로 나타난다.
  function addNewLabel() {
    const typed = newLabel.trim()
    if (typed === '' || selectedLabels.length >= 5) return

    const name = resolveExistingLabelName(typed, knownLabelNames) ?? typed
    if (selectedLabels.some((label) => isSameLabelName(label, name))) {
      setNewLabel('')
      return
    }

    setSelectedLabels((current) => [...current, name])
    setNewLabel('')
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!currentMember) return
    // 미리보기 중에는 textarea가 언마운트돼 required 검사가 돌지 않는다 — 편집으로 되돌려 보여준다.
    if (content.trim() === '') {
      setContentView('edit')
      return
    }

    createDocument.mutate(
      {
        workspaceId,
        title: title.trim(),
        content,
        ownerId: currentMember.id,
        ownerName: currentMember.displayName,
        labels: selectedLabels,
      },
      { onSuccess: (created) => navigate(routes.documentDetail(workspaceId, created.id)) },
    )
  }

  const labelChoices = [
    ...knownLabelNames,
    ...selectedLabels.filter(
      (name) => !knownLabelNames.some((known) => isSameLabelName(known, name)),
    ),
  ]

  return (
    <form onSubmit={handleSubmit} className="max-w-[640px]">
      <ScreenTitle>문서 업로드</ScreenTitle>

      <input
        ref={fileInputRef}
        type="file"
        accept=".md,.txt,text/markdown,text/plain"
        onChange={handleFileChange}
        className="hidden"
      />
      <button
        type="button"
        onClick={() => fileInputRef.current?.click()}
        onDragOver={(event) => {
          event.preventDefault()
          setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(event) => {
          event.preventDefault()
          setDragging(false)
          const file = event.dataTransfer.files?.[0]
          if (file) void readFile(file)
        }}
        className={cx(
          'mb-[22px] w-full cursor-pointer rounded-md border-[1.5px] border-dashed bg-surface p-10 text-center',
          dragging ? 'border-accent bg-accent-bg-strong' : 'border-text-disabled',
        )}
      >
        <div className="mb-[6px] text-[13.5px] font-semibold text-text-secondary">
          파일을 끌어다 놓거나 클릭해 선택하세요
        </div>
        <div className="text-[11.5px] text-text-quaternary">.md, .txt · 최대 10MB</div>
      </button>

      {fileName && (
        <div className="mb-[22px] rounded-sm border border-border-strong bg-surface px-3 py-[9px] text-[13px]">
          {fileName}
        </div>
      )}
      {fileError && <p className="mb-[22px] text-xs text-danger">{fileError}</p>}

      <div className="flex flex-col gap-4">
        <div>
          <FieldLabel>라벨</FieldLabel>
          <div className="flex flex-wrap gap-2">
            {labelChoices.map((name) => {
              const active = selectedLabels.some((label) => isSameLabelName(label, name))
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
            라벨은 문서를 만들 때 함께 생깁니다 · 최대 5개
          </p>
        </div>

        <div>
          <FieldLabel required>문서명</FieldLabel>
          <TextInput
            value={title}
            onChange={(event) => setTitle(event.target.value)}
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
              value={content}
              onChange={(event) => setContent(event.target.value)}
              maxLength={DOCUMENT_CONTENT_MAX_LENGTH}
              required
              className="min-h-[220px]"
              placeholder="파일을 올리거나 직접 붙여 넣으세요 · 마크다운(.md) 문법을 그대로 씁니다"
            />
          ) : (
            // 미리보기는 상세 화면과 같은 뷰어·같은 글자 규격으로 그린다.
            <div className="min-h-[220px] rounded-sm border border-border-strong bg-surface px-4 py-3">
              {content.trim() === '' ? (
                <p className="text-[12.5px] text-text-quaternary">미리볼 본문이 없습니다.</p>
              ) : (
                <Markdown
                  source={content}
                  className="text-[13.5px] leading-[1.9] text-text-secondary"
                />
              )}
            </div>
          )}
          <div className="mt-1 text-right text-[11px] text-text-quaternary">
            {content.length.toLocaleString()} /{' '}
            {DOCUMENT_CONTENT_MAX_LENGTH.toLocaleString()}
          </div>
        </div>
      </div>

      {createDocument.isError && (
        <p className="mt-4 text-xs text-danger">
          {createDocument.error instanceof ApiError
            ? createDocument.error.message
            : (createDocument.error as Error).message}
        </p>
      )}

      <div className="mt-[26px] flex justify-end gap-[10px]">
        <Button variant="outline" onClick={() => navigate(routes.documents(workspaceId))}>
          취소
        </Button>
        <Button type="submit" variant="primary" disabled={createDocument.isPending}>
          {createDocument.isPending ? '업로드 중…' : '업로드'}
        </Button>
      </div>
    </form>
  )
}
