import { useNavigate, useParams } from 'react-router-dom'
import { Button, Card } from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { useExtractionEligibleDocuments } from '../../features/dictionary/hooks/useExtractionEligibleDocuments'

export function TermExtractionPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const navigate = useNavigate()
  const { data: documents, isLoading } = useExtractionEligibleDocuments(workspaceId)

  const eligibleCount = documents?.filter((doc) => doc.eligible).length ?? 0

  return (
    <div className="max-w-xl">
      <h1 className="mb-1 font-display text-[19px] font-bold text-text">
        후보 작성용 용어 추출
      </h1>
      <p className="mb-5 text-[12.5px] text-text-tertiary">
        최신 사전집까지 갱신·대조를 마치고 최종본으로 확정된 문서에서 후보를 뽑습니다.
      </p>

      {isLoading && <p className="text-sm text-text-tertiary">불러오는 중…</p>}

      {documents && (
        <Card className="p-5">
          <p className="mb-3 text-[13px] font-bold text-text">
            최종본 {eligibleCount}건 중 {eligibleCount}건 선택됨
          </p>
          <div className="flex flex-col gap-[9px]">
            {documents.map((doc) => (
              <div
                key={doc.documentId}
                className={`flex items-center gap-[9px] text-[13px] ${
                  doc.eligible ? 'font-medium text-text' : 'text-text-faint'
                }`}
              >
                <span
                  className={`inline-block h-[15px] w-[15px] shrink-0 rounded ${
                    doc.eligible ? 'bg-accent' : 'border-[1.5px] border-text-disabled'
                  }`}
                />
                {doc.title}
                {doc.reason && (
                  <span className="ml-1 text-[10.5px] text-text-faint">
                    — {doc.reason}
                  </span>
                )}
              </div>
            ))}
          </div>
          <div className="mt-5 flex items-center justify-between border-t border-border-soft pt-4">
            <span className="text-xs text-text-quaternary">
              예상 소요 약 2~3분 · ADMIN 이상만 실행 가능
            </span>
            <Button
              variant="primary"
              onClick={() => navigate(routes.dictionaryDraft(workspaceId))}
            >
              추출 실행
            </Button>
          </div>
        </Card>
      )}
    </div>
  )
}
