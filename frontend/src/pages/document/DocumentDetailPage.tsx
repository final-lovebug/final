import { useParams } from 'react-router-dom'
import { PagePlaceholder } from '../PagePlaceholder'

export function DocumentDetailPage() {
  const { documentId } = useParams<{ documentId: string }>()
  return (
    <PagePlaceholder
      title={`문서 상세 (documentId: ${documentId})`}
      routeKey="docDetail"
    />
  )
}
