import { useParams } from 'react-router-dom'
import { PagePlaceholder } from '../PagePlaceholder'

export function DocumentReviewPage() {
  const { documentId } = useParams<{ documentId: string }>()
  return (
    <PagePlaceholder
      title={`문서 검토 · 대조 결과 (documentId: ${documentId})`}
      routeKey="reviewDoc"
    />
  )
}
