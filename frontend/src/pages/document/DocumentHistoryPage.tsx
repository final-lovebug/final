import { useParams } from 'react-router-dom'
import { Card, Pill } from '../../shared/ui'
import { useDocumentVersions } from '../../features/document/hooks/useDocumentVersions'
import { useSuggestionHistory } from '../../features/document/hooks/useSuggestionHistory'
import { cx } from '../../shared/lib/cx'

export function DocumentHistoryPage() {
  const { documentId = '' } = useParams<{ documentId: string }>()
  const { data: versions, isLoading, isError } = useDocumentVersions(documentId)
  const { data: history } = useSuggestionHistory(documentId)

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

      <div className="flex items-start gap-5">
        <div className="flex w-[360px] shrink-0 flex-col gap-3">
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

        {history && history.length > 0 && (
          <Card className="flex-1 p-[22px]">
            <p className="mb-2 text-[12.5px] font-bold text-text">처리 내역</p>
            <p className="mb-[10px] text-[11.5px] text-text-quaternary">
              리뷰어는 이 기록으로 무엇이 왜 바뀌었는지 확인합니다
            </p>
            <table className="w-full border-collapse text-xs">
              <thead>
                <tr>
                  {['원래', '결과', '처리'].map((h) => (
                    <th
                      key={h}
                      className="border-b border-border-soft px-2 py-2 text-left text-[10.5px] text-text-quaternary"
                    >
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {history.map((h) => (
                  <tr key={h.original}>
                    <td className="border-b border-border-faint px-2 py-2">
                      {h.action === 'ignored' && (
                        <span className="mr-1 inline-block h-[5px] w-[5px] rounded-full bg-danger" />
                      )}
                      {h.original}
                    </td>
                    <td className="border-b border-border-faint px-2 py-2">{h.result}</td>
                    <td
                      className={cx(
                        'border-b border-border-faint px-2 py-2',
                        h.action === 'applied' && 'font-semibold text-success',
                        h.action === 'manual' && 'font-semibold text-accent-strong',
                        h.action === 'ignored' && 'text-text-tertiary',
                      )}
                    >
                      {h.action === 'applied'
                        ? '적용'
                        : h.action === 'manual'
                          ? `직접 입력 → "${h.manualValue}"`
                          : `무시 — "${h.reason}"`}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        )}
      </div>
    </div>
  )
}
