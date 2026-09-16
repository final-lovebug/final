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
import { markdownToPlainText } from '../../shared/lib/markdown'
import { useDocuments } from '../../features/document/hooks/useDocuments'
import { useLabels } from '../../features/document/hooks/useLabels'
import { dictionaryVersionLabel } from '../../features/document/model/dictionaryVersionLabel'
import { isSameLabelName } from '../../features/document/model/labelName'

type ViewMode = 'list' | 'card'

// ui/main.js renderDocsScreen() 이식. 프로토타입에서는 목록/카드 전환과 라벨·검색 필터가
// 장식이었지만(README "Notes / judgment calls"), 실제 데이터가 붙은 화면에서 죽은 컨트롤은
// 오히려 혼란스러워 전부 동작하게 만들었다 — 셋 다 이미 받아 온 목록 위에서 거르므로
// 추가 요청이 없다.
//
// **(2026-09-16)** 툴바의 「용어 추출 실행」을 걷어냈다. 사전집이 없는 워크스페이스에서는
// 같은 버튼이 이 화면과 사전집 화면 양쪽에 떠 어느 쪽이 정본인지 알 수 없었다. 추출은
// **사전집을 만들어 내는 행위**이므로 진입점을 사전집 그룹에 하나로 모은다 — 사전집이
// 없으면 `DictionaryPage`의 빈 상태가, 있으면 `DictionaryDraftPage`의 빈 상태가 안내한다.
// 이 화면은 추출의 입력(문서)을 관리하는 곳으로 남긴다.
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

        <Button variant="primary" onClick={() => navigate(routes.documentUpload(workspaceId))}>
          문서 업로드
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
                        {markdownToPlainText(doc.content)}
                      </div>
                    )}
                  </Td>
                  <Td>
                    <span className="inline-flex items-center gap-[6px]">
                      <Pill tone="outline">{dictionaryVersionLabel(doc.dictionaryVersionNo)}</Pill>
                      {doc.badge && (
                        <Pill tone={doc.badge === 'danger' ? 'danger' : 'neutral'}>
                          {doc.badge === 'danger' ? '재검사 필요' : '뒤처짐'}
                        </Pill>
                      )}
                    </span>
                  </Td>
                  <Td>
                    {/* 라벨은 문서당 최대 5개고 전부 보여준다. 표 칸에 그대로 풀면 긴 이름
                        다섯이 문서명 칸을 밀어내므로, 너비를 묶어 두고 안에서 줄바꿈시킨다
                        (`td`의 max-width는 auto 레이아웃에서 잘 안 먹어 div로 감쌌다). */}
                    {doc.labels && doc.labels.length > 0 ? (
                      <div className="flex max-w-[240px] flex-wrap gap-[6px]">
                        {doc.labels.map((label) => (
                          <Pill key={label} tone="outline">
                            {label}
                          </Pill>
                        ))}
                      </div>
                    ) : (
                      '—'
                    )}
                  </Td>
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
              {/* 상태(사전집 버전·배지)와 라벨을 줄로 나눈다 — 한 줄에 섞으면 라벨 다섯이
                  밀고 들어와 배지가 카드마다 다른 자리에 떨어진다. */}
              <div className="flex flex-wrap items-center gap-[6px]">
                <Pill tone="outline">{dictionaryVersionLabel(doc.dictionaryVersionNo)}</Pill>
                {doc.badge && (
                  <Pill tone={doc.badge === 'danger' ? 'danger' : 'neutral'}>
                    {doc.badge === 'danger' ? '재검사 필요' : '뒤처짐'}
                  </Pill>
                )}
              </div>
              {doc.labels && doc.labels.length > 0 && (
                <div className="flex flex-wrap items-center gap-[6px]">
                  {doc.labels.map((label) => (
                    <Pill key={label} tone="outline">
                      {label}
                    </Pill>
                  ))}
                </div>
              )}
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
