import { useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  Button,
  Card,
  DataTable,
  FilterChip,
  Pill,
  Segmented,
  Td,
  Th,
  TextInput,
  Toolbar,
  ToolbarSpacer,
  Tr,
} from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { useDocuments } from '../../features/document/hooks/useDocuments'
import { useLabels } from '../../features/document/hooks/useLabels'
import { isSameLabelName } from '../../features/document/model/labelName'

type ViewMode = 'list' | 'card'

// ui/main.js renderDocsScreen() 이식. 프로토타입에서는 목록/카드 전환과 라벨·검색 필터가
// 장식이었지만(README "Notes / judgment calls"), 실제 데이터가 붙은 화면에서 죽은 컨트롤은
// 오히려 혼란스러워 전부 동작하게 만들었다 — 셋 다 이미 받아 온 목록 위에서 거르므로
// 추가 요청이 없다.
export function DocumentListPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const navigate = useNavigate()
  const { data: documents, isLoading, isError } = useDocuments(workspaceId)
  const { data: labels } = useLabels(workspaceId)

  const [view, setView] = useState<ViewMode>('list')
  const [labelFilter, setLabelFilter] = useState('')
  const [keyword, setKeyword] = useState('')

  const visibleDocuments = useMemo(() => {
    const needle = keyword.trim().toLowerCase()
    return (documents ?? []).filter((doc) => {
      const matchesLabel =
        labelFilter === '' || (doc.labels ?? []).some((label) => isSameLabelName(label, labelFilter))
      const matchesKeyword = needle === '' || doc.title.toLowerCase().includes(needle)
      return matchesLabel && matchesKeyword
    })
  }, [documents, labelFilter, keyword])

  return (
    <div>
      <Toolbar>
        <Segmented
          value={view}
          onChange={setView}
          options={[
            { value: 'list', label: '목록' },
            { value: 'card', label: '카드' },
          ]}
        />

        <FilterChip className="p-0">
          <select
            value={labelFilter}
            onChange={(event) => setLabelFilter(event.target.value)}
            aria-label="라벨 필터"
            className="cursor-pointer bg-transparent px-3 py-[7px] text-[12.5px] text-text-secondary outline-none"
          >
            <option value="">라벨 전체</option>
            {labels?.map((label) => (
              <option key={label.id} value={label.name}>
                {label.name}
              </option>
            ))}
          </select>
        </FilterChip>

        <TextInput
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="문서 검색"
          className="w-[180px] py-[7px] text-[12.5px]"
        />

        <ToolbarSpacer />

        <Button variant="outline" onClick={() => navigate(routes.documentUpload(workspaceId))}>
          문서 업로드
        </Button>
        <Button variant="primary" onClick={() => navigate(routes.termExtraction(workspaceId))}>
          용어 추출 실행
        </Button>
      </Toolbar>

      {isLoading && <p className="text-sm text-text-tertiary">불러오는 중…</p>}
      {isError && <p className="text-sm text-danger">문서 목록을 불러오지 못했습니다.</p>}
      {documents?.length === 0 && (
        <p className="text-sm text-text-tertiary">
          이 워크스페이스에는 아직 문서가 없습니다.
        </p>
      )}
      {documents && documents.length > 0 && visibleDocuments.length === 0 && (
        <p className="text-sm text-text-tertiary">조건에 맞는 문서가 없습니다.</p>
      )}

      {visibleDocuments.length > 0 && view === 'list' && (
        <Card className="overflow-hidden">
          <DataTable>
            <thead>
              <tr>
                <Th>문서명</Th>
                <Th>사전집 버전</Th>
                <Th>라벨</Th>
                <Th>작성자</Th>
                <Th>최종 수정자</Th>
                <Th>수정일시</Th>
              </tr>
            </thead>
            <tbody>
              {visibleDocuments.map((doc) => (
                <Tr
                  key={doc.id}
                  clickable
                  onClick={() => navigate(routes.documentDetail(workspaceId, doc.id))}
                >
                  <Td>
                    <div className="font-bold text-text">{doc.title}</div>
                    {doc.content && (
                      <div className="mt-[2px] line-clamp-1 text-[11.5px] text-text-quaternary">
                        {doc.content}
                      </div>
                    )}
                  </Td>
                  <Td>
                    <span className="inline-flex items-center gap-[6px]">
                      <Pill tone="outline">
                        {doc.currentVersionNo > 0 ? `r${doc.currentVersionNo}` : '—'}
                      </Pill>
                      {doc.badge && (
                        <Pill tone={doc.badge === 'danger' ? 'danger' : 'neutral'}>
                          {doc.badge === 'danger' ? '재검사 필요' : '뒤처짐'}
                        </Pill>
                      )}
                    </span>
                  </Td>
                  <Td>{doc.label ? <Pill tone="outline">{doc.label.name}</Pill> : '—'}</Td>
                  <Td>{doc.ownerName ?? '—'}</Td>
                  {/* 실 API 문서 응답엔 최종 수정자 필드가 없다(T-INT-10) — "—"로 둔다. */}
                  <Td>{doc.updaterName ?? '—'}</Td>
                  <Td className="text-text-quaternary">
                    {new Date(doc.updatedAt).toLocaleString('ko-KR')}
                  </Td>
                </Tr>
              ))}
            </tbody>
          </DataTable>
        </Card>
      )}

      {visibleDocuments.length > 0 && view === 'card' && (
        <div className="grid grid-cols-1 gap-[18px] sm:grid-cols-2 xl:grid-cols-3">
          {visibleDocuments.map((doc) => (
            <Link
              key={doc.id}
              to={routes.documentDetail(workspaceId, doc.id)}
              className="flex flex-col gap-[10px] rounded-md border border-border bg-surface p-[18px] shadow-card hover:border-accent-border hover:shadow-card-hover"
            >
              <div className="font-display text-[14px] font-bold text-text">{doc.title}</div>
              <div className="flex flex-wrap items-center gap-[6px]">
                <Pill tone="outline">
                  {doc.currentVersionNo > 0 ? `r${doc.currentVersionNo}` : '—'}
                </Pill>
                {doc.label && <Pill tone="outline">{doc.label.name}</Pill>}
                {doc.badge && (
                  <Pill tone={doc.badge === 'danger' ? 'danger' : 'neutral'}>
                    {doc.badge === 'danger' ? '재검사 필요' : '뒤처짐'}
                  </Pill>
                )}
              </div>
              <div className="flex justify-between border-t border-border-soft pt-[10px] text-[11px] text-text-quaternary">
                <span>{doc.ownerName ?? '—'}</span>
                <span>{new Date(doc.updatedAt).toLocaleDateString('ko-KR')}</span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
