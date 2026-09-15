import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  Banner,
  Button,
  Card,
  ColFlex,
  FilterChip,
  Pill,
  TimelineItem,
  Toolbar,
  ToolbarSpacer,
  TwoCol,
} from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import {
  useDictionaryVersionDiff,
  useDictionaryVersions,
} from '../../features/dictionary/hooks/useDictionaryVersions'

// ui/main.js renderDictHistoryScreen() 이식.
//
// **목업이던 타임라인·diff를 실 API로 바꿨다**(2026-09-14). 개정 이력 전용 엔드포인트는
// 없지만 버전 목록과 버전별 용어 목록이 있어서 둘로 만들어 낸다 —
// features/dictionary/api/fetchDictionaryVersions.ts 주석에 한계까지 적어 뒀다.
export function DictionaryHistoryPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const { data: versions, isLoading, isError } = useDictionaryVersions(workspaceId)

  // 기본 비교 대상은 가장 최근 두 버전이다(ui의 r6 → r7). 사용자가 고른 값이 있으면
  // 그것이 이긴다 — 기본값을 effect로 state에 밀어 넣지 않고 렌더 중에 파생시킨다.
  const [fromOverride, setFromOverride] = useState<number | null>(null)
  const [toOverride, setToOverride] = useState<number | null>(null)
  const fromVersionNo = fromOverride ?? versions?.[1]?.versionNo ?? null
  const toVersionNo = toOverride ?? versions?.[0]?.versionNo ?? null

  const { data: diff, isLoading: isLoadingDiff } = useDictionaryVersionDiff(
    workspaceId,
    fromVersionNo,
    toVersionNo,
  )

  if (isLoading) return <p className="text-sm text-text-tertiary">불러오는 중…</p>
  if (isError) return <p className="text-sm text-danger">리비전 이력을 불러오지 못했습니다.</p>
  if (!versions || versions.length === 0) {
    return (
      <p className="text-sm text-text-tertiary">
        아직 발행된 사전집이 없습니다. 첫 리비전은 사전 개정안이 승인되면 만들어집니다.
      </p>
    )
  }

  return (
    <div>
      <Toolbar className="mb-[18px]">
        <h1 className="font-display text-[19px] font-bold text-text">
          사전집 리비전 이력 · 비교
        </h1>
        <ToolbarSpacer />
        <FilterChip className="p-0">
          <select
            aria-label="비교 시작 버전"
            value={fromVersionNo ?? ''}
            onChange={(event) => setFromOverride(Number(event.target.value))}
            className="cursor-pointer bg-transparent px-3 py-[7px] text-[12.5px] text-text-secondary outline-none"
          >
            {versions.map((version) => (
              <option key={version.versionNo} value={version.versionNo}>
                r{version.versionNo}
              </option>
            ))}
          </select>
        </FilterChip>
        <span className="text-text-quaternary">→</span>
        <FilterChip className="p-0">
          <select
            aria-label="비교 대상 버전"
            value={toVersionNo ?? ''}
            onChange={(event) => setToOverride(Number(event.target.value))}
            className="cursor-pointer bg-transparent px-3 py-[7px] text-[12.5px] text-text-secondary outline-none"
          >
            {versions.map((version) => (
              <option key={version.versionNo} value={version.versionNo}>
                r{version.versionNo}
              </option>
            ))}
          </select>
        </FilterChip>
        <Link to={routes.dictionary(workspaceId)}>
          <Button variant="outline">돌아가기</Button>
        </Link>
      </Toolbar>

      <TwoCol>
        <div className="flex max-h-[720px] w-[380px] shrink-0 flex-col gap-[9px] overflow-auto">
          {versions.map((version) => (
            <TimelineItem
              key={version.versionNo}
              tone={version.status === 'ACTIVE' ? 'current' : 'default'}
            >
              <div className="flex items-center gap-2">
                <span className="font-bold">r{version.versionNo}</span>
                <Pill tone={version.status === 'ACTIVE' ? 'success' : 'neutral'}>
                  {version.status === 'ACTIVE' ? '공식' : '보관'}
                </Pill>
                {version.status === 'ACTIVE' && (
                  <span className="text-[10px] font-bold text-accent-strong">현재</span>
                )}
              </div>
              <p className="my-1 text-[11px] text-text-quaternary">
                {new Date(version.publishedAt).toLocaleString('ko-KR')} ·{' '}
                {version.publishedByName}
              </p>
              <p className="text-xs text-text-secondary">용어 {version.termCount}개</p>
            </TimelineItem>
          ))}
        </div>

        <ColFlex>
          <Card className="p-[22px]">
            {versions.length < 2 && (
              <p className="text-sm text-text-tertiary">
                비교하려면 리비전이 둘 이상 필요합니다.
              </p>
            )}
            {versions.length >= 2 && isLoadingDiff && (
              <p className="text-sm text-text-tertiary">변경 내역을 계산하는 중…</p>
            )}
            {diff && (
              <>
                <p className="mb-4 font-display text-[15px] font-bold">
                  r{diff.fromVersionNo} → r{diff.toVersionNo} 변경 내역
                </p>

                <div className="flex flex-col gap-4">
                  <DiffSection
                    label={`추가 ${diff.added.length}`}
                    labelClassName="text-success"
                    items={diff.added}
                  />
                  <DiffSection
                    label={`변경 ${diff.changed.length}`}
                    labelClassName="text-warn"
                    items={diff.changed}
                  />
                  <DiffSection
                    label={`삭제 ${diff.removed.length}`}
                    labelClassName="text-text-quaternary"
                    items={diff.removed}
                  />
                </div>

                <Banner className="mt-[18px] leading-[1.7]">
                  {diff.recheckRequired ? (
                    <>
                      사라진 표기가 {diff.removed.length}건 있어 <strong>재검사 등급</strong>
                      입니다. 그 표기를 쓰던 문서는 다시 대조해야 합니다.
                    </>
                  ) : (
                    <>
                      대표어 변경과 용어 삭제가 없어 <strong>'새 지적'</strong>입니다. 기존
                      문서를 다시 볼 필요는 없고, 새 용어에 대한 제안만 늘어납니다 — r
                      {diff.fromVersionNo} 태그를 단 문서는 '뒤처짐'으로만 표시됩니다.
                    </>
                  )}
                  <br />
                  <span className="text-[11.5px] text-text-tertiary">
                    「변경」은 영문명 차이만 잡습니다 — 용어 목록 응답에 정의가 없어(D-41)
                    정의만 고친 용어는 드러나지 않습니다.
                  </span>
                </Banner>
              </>
            )}
          </Card>
        </ColFlex>
      </TwoCol>
    </div>
  )
}

function DiffSection({
  label,
  labelClassName,
  items,
}: {
  label: string
  labelClassName: string
  items: string[]
}) {
  return (
    <div>
      <p className={`mb-2 text-[12.5px] font-bold ${labelClassName}`}>{label}</p>
      {items.length > 0 && (
        <p className="text-[13px] leading-[1.8] text-text-secondary">{items.join(' · ')}</p>
      )}
    </div>
  )
}
