import { Link, useParams } from 'react-router-dom'
import { Button, Card, Pill } from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { useDocument } from '../../features/document/hooks/useDocument'

export function DocumentDetailPage() {
  const { workspaceId = '', documentId = '' } = useParams<{
    workspaceId: string
    documentId: string
  }>()
  const { data: document, isLoading, isError } = useDocument(documentId)

  if (isLoading) return <p className="text-sm text-text-tertiary">불러오는 중…</p>
  if (isError || !document) {
    return <p className="text-sm text-danger">문서를 찾을 수 없습니다.</p>
  }

  return (
    <div className="mx-auto max-w-3xl">
      <div className="mb-4 flex items-start justify-between gap-3">
        <div>
          <div className="mb-1 flex items-center gap-2">
            <h1 className="font-display text-[19px] font-bold text-text">
              {document.title}
            </h1>
            {document.label && <Pill tone="outline">{document.label.name}</Pill>}
          </div>
          <p className="text-[12px] text-text-tertiary">
            {document.currentVersionNo > 0 ? `r${document.currentVersionNo}` : '초안'} ·
            {' '}
            {document.ownerName} 작성 · {document.updaterName} 최종 수정 ·{' '}
            {new Date(document.updatedAt).toLocaleString('ko-KR')}
          </p>
        </div>
        <div className="flex shrink-0 gap-2">
          <Link to={routes.documentHistory(workspaceId, documentId)}>
            <Button variant="outline">버전 이력</Button>
          </Link>
          <Link to={routes.termExtraction(workspaceId, documentId)}>
            <Button variant="outline">용어 추출</Button>
          </Link>
          <Link to={routes.documentReview(workspaceId, documentId)}>
            <Button variant="primary">문서 검토</Button>
          </Link>
        </div>
      </div>

      <Card className="p-6">
        <p className="whitespace-pre-wrap text-sm leading-[2.1] text-text">
          {document.content}
        </p>
      </Card>
    </div>
  )
}
