import { useParams } from 'react-router-dom'
import { PagePlaceholder } from '../PagePlaceholder'

export function DictionaryRevisionPage() {
  const { revisionId } = useParams<{ revisionId: string }>()
  return (
    <PagePlaceholder
      title={`사전집 개정안 리뷰 · PR형 (revisionId: ${revisionId})`}
      routeKey="revision"
    />
  )
}
