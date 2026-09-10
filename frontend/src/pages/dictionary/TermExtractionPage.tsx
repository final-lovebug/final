import { useParams } from 'react-router-dom'
import { PagePlaceholder } from '../PagePlaceholder'

export function TermExtractionPage() {
  const { documentId } = useParams<{ documentId: string }>()
  return (
    <PagePlaceholder
      title={`용어 추출 (documentId: ${documentId})`}
      routeKey="extract"
    />
  )
}
