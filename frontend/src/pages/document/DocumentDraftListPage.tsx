import { useNavigate, useParams } from 'react-router-dom'
import {
  Card,
  DataTable,
  Pill,
  ScreenSubtitle,
  ScreenTitle,
  Td,
  Th,
  Tr,
} from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { useDraftDocuments } from '../../features/document/hooks/useDraftDocuments'

// "문서 > 초안" 사이드바 목록(프로토타입에는 없던 화면 — 여러 문서가 동시에 초안 단계에
// 있을 수 있어 신설했다). 사전집 대조 후 아직 제안(SuggestionTerm)이 남아 있는 문서를
// 모은다 — 골라 들어가면 검토 화면(적용/무시)으로 이어지고, 전부 처리하면 거기서
// 「검토 완료」로 개정안 단계로 넘어간다.
//
// 표 모양은 ui/style.css의 .dtable을 따른다 — 다른 목록 화면과 같은 규격이다.
export function DocumentDraftListPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const navigate = useNavigate()
  const { data: documents, isLoading, isError } = useDraftDocuments(workspaceId)

  return (
    <div>
      <ScreenTitle>초안</ScreenTitle>
      <ScreenSubtitle>
        사전집 대조 후 유비쿼터스 언어 적용/무시가 아직 끝나지 않은 문서 목록입니다. 전부
        처리하면 리뷰를 요청해 개정안 단계로 넘길 수 있습니다.
      </ScreenSubtitle>

      {isLoading && <p className="text-sm text-text-tertiary">불러오는 중…</p>}
      {isError && <p className="text-sm text-danger">초안 목록을 불러오지 못했습니다.</p>}
      {documents?.length === 0 && (
        <p className="text-sm text-text-tertiary">초안 진행 중인 문서가 없습니다.</p>
      )}

      {documents && documents.length > 0 && (
        <Card className="overflow-hidden">
          <DataTable>
            <thead>
              <tr>
                <Th>문서명</Th>
                <Th>미해결 제안</Th>
                <Th>최종 수정</Th>
              </tr>
            </thead>
            <tbody>
              {documents.map((document) => (
                <Tr
                  key={document.draftDocumentId}
                  clickable
                  onClick={() =>
                    navigate(routes.documentDraft(workspaceId, document.documentId))
                  }
                >
                  <Td className="font-bold text-text">{document.title}</Td>
                  <Td>
                    {document.pendingCount === 0 ? (
                      <Pill tone="success">처리 완료</Pill>
                    ) : (
                      <Pill tone="warn">미해결 {document.pendingCount}건</Pill>
                    )}
                  </Td>
                  <Td className="text-text-quaternary">
                    {new Date(document.updatedAt).toLocaleString('ko-KR')}
                  </Td>
                </Tr>
              ))}
            </tbody>
          </DataTable>
        </Card>
      )}
    </div>
  )
}
