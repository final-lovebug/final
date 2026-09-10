import { useParams } from 'react-router-dom'
import { Card, Pill } from '../../shared/ui'
import { useDocumentVersions } from '../../features/document/hooks/useDocumentVersions'

export function DocumentHistoryPage() {
  const { documentId = '' } = useParams<{ documentId: string }>()
  const { data: versions, isLoading, isError } = useDocumentVersions(documentId)

  return (
    <div>
      <h1 className="mb-1 font-display text-[19px] font-bold text-text">
        문서 버전 이력
      </h1>
      <p className="mb-5 text-[12.5px] text-text-tertiary">
        확정된 버전만 표시됩니다. 작업 중인 최신 본문은 문서 상세에서 볼 수 있습니다.
      </p>

      {isLoading && <p className="text-sm text-text-tertiary">불러오는 중…</p>}
      {isError && <p className="text-sm text-danger">이력을 불러오지 못했습니다.</p>}
      {versions?.length === 0 && (
        <p className="text-sm text-text-tertiary">아직 확정된 버전이 없습니다.</p>
      )}

      <div className="flex flex-col gap-3">
        {versions?.map((version) => (
          <Card key={version.id} className="flex items-start gap-3 p-4">
            <Pill tone="accent">r{version.versionNo}</Pill>
            <div className="flex-1">
              <p className="text-[12px] text-text-quaternary">
                {new Date(version.publishedAt).toLocaleString('ko-KR')}
                {version.dictionaryVersionNo
                  ? ` · 기준 사전집 r${version.dictionaryVersionNo}`
                  : ''}
              </p>
              <p className="mt-1 text-[12.5px] text-text-secondary">{version.body}</p>
            </div>
          </Card>
        ))}
      </div>
    </div>
  )
}
