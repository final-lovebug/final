import { Link, useNavigate, useParams } from 'react-router-dom'
import { Button, Pill } from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { useDocuments } from '../../features/document/hooks/useDocuments'

export function DocumentListPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const navigate = useNavigate()
  const { data: documents, isLoading, isError } = useDocuments(workspaceId)

  return (
    <div>
      <div className="mb-4 flex items-center gap-3">
        <div className="flex-1">
          <h1 className="font-display text-[19px] font-bold text-text">문서</h1>
          <p className="text-[12.5px] text-text-tertiary">
            워크스페이스에 등록된 문서 목록입니다.
          </p>
        </div>
        <Button
          variant="primary"
          onClick={() => navigate(routes.documentUpload(workspaceId))}
        >
          문서 업로드
        </Button>
      </div>

      {isLoading && <p className="text-sm text-text-tertiary">불러오는 중…</p>}
      {isError && (
        <p className="text-sm text-danger">문서 목록을 불러오지 못했습니다.</p>
      )}
      {documents?.length === 0 && (
        <p className="text-sm text-text-tertiary">
          이 워크스페이스에는 아직 문서가 없습니다.
        </p>
      )}

      {documents && documents.length > 0 && (
        <div className="overflow-x-auto rounded-md border border-border bg-surface">
          <table className="w-full border-collapse text-[12.5px]">
            <thead>
              <tr>
                {['문서', '라벨', '버전', '작성자', '최종 수정자', '수정일'].map(
                  (heading) => (
                    <th
                      key={heading}
                      className="whitespace-nowrap border-b border-border-soft px-4 py-[11px] text-left text-[11px] font-semibold text-text-quaternary"
                    >
                      {heading}
                    </th>
                  ),
                )}
              </tr>
            </thead>
            <tbody>
              {documents.map((doc) => (
                <tr key={doc.id} className="hover:bg-surface-muted">
                  <td className="border-b border-border-faint px-4 py-3">
                    <Link
                      to={routes.documentDetail(workspaceId, doc.id)}
                      className="font-semibold text-text hover:text-accent-strong"
                    >
                      {doc.title}
                    </Link>
                    {doc.badge && (
                      <Pill
                        tone={doc.badge === 'danger' ? 'danger' : 'neutral'}
                        className="ml-2"
                      >
                        {doc.badge === 'danger' ? '재검사 필요' : '뒤처짐'}
                      </Pill>
                    )}
                  </td>
                  <td className="border-b border-border-faint px-4 py-3 text-text-secondary">
                    {doc.label ? <Pill tone="outline">{doc.label.name}</Pill> : '—'}
                  </td>
                  <td className="border-b border-border-faint px-4 py-3 text-text-secondary">
                    {doc.currentVersionNo > 0 ? `r${doc.currentVersionNo}` : '—'}
                  </td>
                  <td className="border-b border-border-faint px-4 py-3 text-text-secondary">
                    {doc.ownerName}
                  </td>
                  <td className="border-b border-border-faint px-4 py-3 text-text-secondary">
                    {doc.updaterName}
                  </td>
                  <td className="border-b border-border-faint px-4 py-3 text-text-secondary">
                    {new Date(doc.updatedAt).toLocaleString('ko-KR')}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
