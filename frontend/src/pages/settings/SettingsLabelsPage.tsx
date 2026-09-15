import { Link, useParams } from 'react-router-dom'
import { Banner, Card, DataTable, Pill, Td, Th, Tr } from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { useLabels } from '../../features/document/hooks/useLabels'
import { useDocuments } from '../../features/document/hooks/useDocuments'

// ui/main.js renderSettingsLabels() 이식.
//
// **(2026-09-14) 목업을 걷어냈다.** 프로토타입의 「추가」·「이름 변경」·「삭제」는 대응하는
// 백엔드 엔드포인트가 없다 — 라벨은 문서 생성/수정 요청의 `labels` 필드로만 생기고
// (`docs/API.md` 라벨 절에 목록 조회만 있다), 독립 라벨 리소스가 없어 이름 변경·삭제도
// 불가능하다(T-INT-10에서 사용자와 확인한 사실이다). 그래서 가짜로 동작하던 「추가」를
// 없애고 라벨이 실제로 생기는 자리(문서 업로드)로 안내한다.
//
// 응답에 `documentCount`가 없어 문서 목록의 `labels`로 직접 센다 — 라벨 수·문서 수가
// 모두 작은 화면이라 클라이언트 집계로 충분하다.
export function SettingsLabelsPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const { data: labels, isLoading } = useLabels(workspaceId)
  const { data: documents } = useDocuments(workspaceId)

  const documentCountByLabel = new Map<string, number>()
  for (const document of documents ?? []) {
    for (const label of document.labels ?? []) {
      documentCountByLabel.set(label, (documentCountByLabel.get(label) ?? 0) + 1)
    }
  }

  return (
    <div className="max-w-[640px]">
      <Banner className="mb-5 leading-[1.7]">
        여기서 만든 라벨은 문서 목록의 라벨 컬럼과 필터, 후보 근거 문장의 출처 표시에
        쓰입니다. 문서 하나에 라벨을 여러 개 지정할 수 있습니다.
      </Banner>

      {isLoading && <p className="text-sm text-text-tertiary">불러오는 중…</p>}

      <Card className="overflow-hidden">
        <DataTable>
          <thead>
            <tr>
              <Th>라벨</Th>
              <Th>쓰이는 문서</Th>
            </tr>
          </thead>
          <tbody>
            {labels?.length === 0 && (
              <Tr>
                <Td colSpan={2} className="py-6 text-center text-text-tertiary">
                  아직 라벨이 없습니다.
                </Td>
              </Tr>
            )}
            {labels?.map((label) => (
              <Tr key={label.id}>
                <Td className="font-semibold text-text">
                  <Pill tone="outline">{label.name}</Pill>
                </Td>
                <Td className="text-text-quaternary">
                  문서 {documentCountByLabel.get(label.name) ?? 0}건
                </Td>
              </Tr>
            ))}
          </tbody>
        </DataTable>
      </Card>

      <Banner tone="neutral" className="mt-4 leading-[1.7]">
        라벨은 <strong>문서를 올릴 때 함께 생깁니다</strong> — 독립적으로 만들거나 이름을
        바꾸거나 지우는 API가 아직 없습니다.{' '}
        <Link
          to={routes.documentUpload(workspaceId)}
          className="font-semibold text-accent-strong underline"
        >
          문서 업로드 화면
        </Link>
        의 라벨 칸에서 새 이름을 적으면 그 라벨이 만들어집니다.
      </Banner>
    </div>
  )
}
