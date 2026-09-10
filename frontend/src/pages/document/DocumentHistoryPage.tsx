import { useParams } from 'react-router-dom'
import { PagePlaceholder } from '../PagePlaceholder'

export function DocumentHistoryPage() {
  const { documentId } = useParams<{ documentId: string }>()
  return (
    <PagePlaceholder
      title={`문서 버전 이력 (documentId: ${documentId})`}
      routeKey="docHistory"
    />
  )
}
