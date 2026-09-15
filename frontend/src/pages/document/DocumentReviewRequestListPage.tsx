import { useNavigate, useParams } from 'react-router-dom'
import {
  Card,
  DataTable,
  Pill,
  ScreenSubtitle,
  ScreenTitle,
  Td,
  Th,
  Tr,
} from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { useDocuments } from '../../features/document/hooks/useDocuments'
import { useDocumentReviewRequests } from '../../features/review/hooks/useDocumentReviewRequests'
import type { PillTone } from '../../shared/ui'
import type { ReviewRequestStatus } from '../../features/review/model/types'

const STATUS_TONE: Record<ReviewRequestStatus, PillTone> = {
  PENDING_REVIEW: 'accent',
  IN_REVIEW: 'accent',
  CHANGES_REQUESTED: 'warn',
  APPROVED: 'success',
  REVISED: 'neutral',
  CANCELED: 'neutral',
}

const STATUS_LABEL: Record<ReviewRequestStatus, string> = {
  PENDING_REVIEW: '검토 대기',
  IN_REVIEW: '검토 중',
  CHANGES_REQUESTED: '변경 요청',
  APPROVED: '승인',
  REVISED: '반영 완료',
  CANCELED: '취소',
}

// "문서 > 개정안" 사이드바 목록(프로토타입에는 없던 화면). review 도메인의 리뷰 요청 목록과
// document 도메인의 문서 목록, 두 훅의 결과를 여기(페이지)에서 조합한다 — features 간 직접
// 참조를 피하면서도 "어떤 문서의 개정안인지"를 보여줘야 하기 때문이다
// (frontend/docs/ARCHITECTURE.md 의존성 규칙 참고).
export function DocumentReviewRequestListPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const navigate = useNavigate()
  const { data: reviewRequests, isLoading, isError } = useDocumentReviewRequests(workspaceId)
  const { data: documents } = useDocuments(workspaceId)

  const documentTitleById = new Map(documents?.map((doc) => [doc.id, doc.title]) ?? [])

  return (
    <div>
      <ScreenTitle>개정안</ScreenTitle>
      <ScreenSubtitle>
        초안 작업이 끝나 리뷰어의 코멘트를 기다리고 있는 문서 개정안 목록입니다.
      </ScreenSubtitle>

      {isLoading && <p className="text-sm text-text-tertiary">불러오는 중…</p>}
      {isError && <p className="text-sm text-danger">개정안 목록을 불러오지 못했습니다.</p>}
      {reviewRequests?.length === 0 && (
        <p className="text-sm text-text-tertiary">진행 중인 개정안이 없습니다.</p>
      )}

      {reviewRequests && reviewRequests.length > 0 && (
        <Card className="overflow-hidden">
          <DataTable>
            <thead>
              <tr>
                <Th>개정안</Th>
                <Th>대상 문서</Th>
                <Th>상태</Th>
                <Th>리뷰어</Th>
                <Th>요청일</Th>
              </tr>
            </thead>
            <tbody>
              {reviewRequests.map((request) => (
                <Tr
                  key={request.reviewRequestId}
                  clickable
                  onClick={() =>
                    navigate(
                      routes.documentReviewThread(
                        workspaceId,
                        request.documentId,
                        request.reviewRequestId,
                      ),
                    )
                  }
                >
                  <Td className="font-bold text-text">{request.title}</Td>
                  <Td>{documentTitleById.get(request.documentId) ?? request.documentId}</Td>
                  <Td>
                    <Pill tone={STATUS_TONE[request.status]}>
                      {STATUS_LABEL[request.status] ?? request.status}
                    </Pill>
                  </Td>
                  <Td>{request.reviewerCount}명</Td>
                  <Td className="text-text-quaternary">
                    {new Date(request.createdAt).toLocaleDateString('ko-KR')}
                  </Td>
                </Tr>
              ))}
            </tbody>
          </DataTable>
        </Card>
      )}
    </div>
  )
}
