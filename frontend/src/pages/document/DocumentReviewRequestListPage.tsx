import { Link, useParams } from 'react-router-dom'
import { Card, Pill } from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { useDocuments } from '../../features/document/hooks/useDocuments'
import { useDocumentReviewRequests } from '../../features/review/hooks/useDocumentReviewRequests'

// "문서 > 개정안" 사이드바 목록. review 도메인의 리뷰 요청 목록과 document 도메인의 문서
// 목록, 두 훅의 결과를 여기(페이지)에서 조합한다 — features 간 직접 참조를 피하면서도
// 화면에는 "어떤 문서의 개정안인지" 제목을 보여줘야 하기 때문이다
// (frontend/docs/ARCHITECTURE.md 의존성 규칙 참고).
export function DocumentReviewRequestListPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const { data: reviewRequests, isLoading } = useDocumentReviewRequests(workspaceId)
  const { data: documents } = useDocuments(workspaceId)

  const documentTitleById = new Map(documents?.map((doc) => [doc.id, doc.title]) ?? [])

  return (
    <div>
      <h1 className="mb-1 font-display text-[19px] font-bold text-text">개정안</h1>
      <p className="mb-5 text-[12.5px] text-text-tertiary">
        초안 작업이 끝나 리뷰어의 코멘트를 기다리고 있는 문서 개정안 목록입니다.
      </p>

      {isLoading && <p className="text-sm text-text-tertiary">불러오는 중…</p>}
      {reviewRequests?.length === 0 && (
        <p className="text-sm text-text-tertiary">진행 중인 개정안이 없습니다.</p>
      )}

      <div className="flex flex-col gap-3">
        {reviewRequests?.map((request) => (
          <Link
            key={request.reviewRequestId}
            to={routes.documentReviewThread(
              workspaceId,
              request.documentId,
              request.reviewRequestId,
            )}
          >
            <Card className="flex items-center gap-3 p-4 hover:shadow-card-hover">
              <div className="flex-1">
                <p className="text-[11px] text-text-quaternary">
                  {documentTitleById.get(request.documentId) ?? request.documentId}
                </p>
                <p className="font-semibold text-text">{request.title}</p>
              </div>
              <Pill tone="accent">리뷰어 {request.reviewerCount}명</Pill>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  )
}
