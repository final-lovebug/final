import { useParams } from 'react-router-dom'
import { PagePlaceholder } from '../PagePlaceholder'

export function DocumentReviewThreadPage() {
  const { documentId, reviewId } = useParams<{
    documentId: string
    reviewId: string
  }>()
  return (
    <PagePlaceholder
      title={`문서 개정안 리뷰 · PR형 (documentId: ${documentId}, reviewId: ${reviewId})`}
      routeKey="reviewThread"
    />
  )
}
