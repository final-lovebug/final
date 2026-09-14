import { type FormEvent, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Card } from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { useAuthStore } from '../../shared/stores/authStore'
import { DOCUMENT_CONTENT_MAX_LENGTH } from '../../features/document/api/createDocument'
import { useCreateDocument } from '../../features/document/hooks/useCreateDocument'

// docs/DOMAIN.md 정책(업로드 가능 형식 txt/md, 본문 10,000자 이내)의 실제 파일 업로드 UI는
// 이번 목업 단계에서 다루지 않고 텍스트 입력만 받는다. 파일 업로드 위젯은 Phase 6 후속 작업.
export function DocumentUploadPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const navigate = useNavigate()
  const currentMember = useAuthStore((state) => state.currentMember)
  const createDocument = useCreateDocument()

  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!currentMember) return

    createDocument.mutate(
      {
        workspaceId,
        title,
        content,
        ownerId: currentMember.id,
        ownerName: currentMember.displayName,
      },
      {
        onSuccess: (created) => {
          navigate(routes.documentDetail(workspaceId, created.id))
        },
      },
    )
  }

  return (
    <div className="mx-auto max-w-xl">
      <h1 className="mb-1 font-display text-[19px] font-bold text-text">
        문서 업로드
      </h1>
      <p className="mb-5 text-[12.5px] text-text-tertiary">
        txt·md 형식, 본문 {DOCUMENT_CONTENT_MAX_LENGTH.toLocaleString()}자 이내
        (docs/DOMAIN.md 정책)
      </p>

      <form onSubmit={handleSubmit}>
        <Card className="flex flex-col gap-4 p-5">
          <label className="flex flex-col gap-2">
            <span className="text-xs font-bold text-text">제목</span>
            <input
              className="rounded-sm border border-border-strong px-3 py-[9px] text-[13px] text-text"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              required
            />
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-bold text-text">본문</span>
            <textarea
              className="min-h-[200px] rounded-sm border border-border-strong px-3 py-[9px] text-[13px] text-text"
              value={content}
              onChange={(event) => setContent(event.target.value)}
              maxLength={DOCUMENT_CONTENT_MAX_LENGTH}
              required
            />
            <span className="text-right text-[11px] text-text-quaternary">
              {content.length.toLocaleString()} / {DOCUMENT_CONTENT_MAX_LENGTH.toLocaleString()}
            </span>
          </label>

          {createDocument.isError && (
            <p className="text-xs text-danger">
              {(createDocument.error as Error).message}
            </p>
          )}

          <div className="flex justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => navigate(routes.documents(workspaceId))}
            >
              취소
            </Button>
            <Button type="submit" variant="primary" disabled={createDocument.isPending}>
              {createDocument.isPending ? '업로드 중…' : '업로드'}
            </Button>
          </div>
        </Card>
      </form>
    </div>
  )
}
