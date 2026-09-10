import { Link, useParams } from 'react-router-dom'
import { Card, Pill } from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { useDraftDocuments } from '../../features/document/hooks/useDraftDocuments'

// "문서 > 초안" 사이드바 목록. 사전집 대조 후 아직 제안(SuggestionTerm)이 남아있는
// 문서들을 모은 화면 — 여기서 문서를 골라 들어가면 기존 DocumentReviewPage(적용/무시
// 화면)로 이어진다. 전부 처리하면 그 화면에서 "검토 완료"로 개정안 단계로 넘어간다.
export function DocumentDraftListPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const { data: documents, isLoading } = useDraftDocuments(workspaceId)

  return (
    <div>
      <h1 className="mb-1 font-display text-[19px] font-bold text-text">초안</h1>
      <p className="mb-5 text-[12.5px] text-text-tertiary">
        사전집 대조 후 유비쿼터스 언어 적용/무시가 아직 끝나지 않은 문서 목록입니다. 전부
        처리하면 리뷰를 요청해 개정안 단계로 넘길 수 있습니다.
      </p>

      {isLoading && <p className="text-sm text-text-tertiary">불러오는 중…</p>}
      {documents?.length === 0 && (
        <p className="text-sm text-text-tertiary">초안 진행 중인 문서가 없습니다.</p>
      )}

      <div className="flex flex-col gap-3">
        {documents?.map((doc) => (
          <Link key={doc.documentId} to={routes.documentReview(workspaceId, doc.documentId)}>
            <Card className="flex items-center gap-3 p-4 hover:shadow-card-hover">
              <div className="flex-1">
                <p className="font-semibold text-text">{doc.title}</p>
                <p className="text-[11px] text-text-quaternary">
                  최종 수정 {new Date(doc.updatedAt).toLocaleString('ko-KR')}
                </p>
              </div>
              <Pill tone="warn">미해결 {doc.pendingCount}건</Pill>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  )
}
