import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  Button,
  Card,
  ColFlex,
  DataTable,
  Markdown,
  Pill,
  Td,
  Th,
  TimelineItem,
  Toolbar,
  ToolbarSpacer,
  Tr,
  TwoCol,
} from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { cx } from '../../shared/lib/cx'
import { useDocument } from '../../features/document/hooks/useDocument'
import { useDocumentVersions } from '../../features/document/hooks/useDocumentVersions'
import { useDocumentVersionDiff } from '../../features/document/hooks/useDocumentVersionBodies'
import { toDiffSource } from '../../features/document/model/textDiff'
import { useSuggestionHistory } from '../../features/document/hooks/useSuggestionHistory'

// ui/main.js renderDocHistoryScreen() 이식 — 타임라인(360px) + 비교·처리 내역 2단.
//
// **(2026-09-14 디자인 정합)** 프로토타입에 하드코딩돼 있던 "v2 → v3" 취소선 비교를 실제
// 두 버전의 본문 비교로 바꿨다. 백엔드가 본문 diff를 주지 않으므로(`D-61`) 버전 단건
// 조회로 본문을 받아 화면에서 만든다 — 한계는 `model/textDiff.ts` 주석 참고.
export function DocumentHistoryPage() {
  const { workspaceId = '', documentId = '' } = useParams<{
    workspaceId: string
    documentId: string
  }>()
  const { data: document } = useDocument(workspaceId, documentId)
  const { data: versions, isLoading, isError } = useDocumentVersions(workspaceId, documentId)
  const { data: history } = useSuggestionHistory(documentId)

  // 기본 비교는 최근 두 버전이다(목록은 versionNo 내림차순으로 온다). 고른 값이 있으면
  // 그것이 이긴다 — 기본값을 effect로 state에 밀어 넣지 않고 렌더 중에 파생시킨다.
  const [fromOverride, setFromOverride] = useState<number | null>(null)
  const [toOverride, setToOverride] = useState<number | null>(null)
  const compareFrom = fromOverride ?? versions?.[1]?.versionNo ?? null
  const compareTo = toOverride ?? versions?.[0]?.versionNo ?? null

  const { data: comparison, isLoading: isLoadingDiff } = useDocumentVersionDiff(
    workspaceId,
    documentId,
    compareFrom,
    compareTo,
  )

  const currentVersionNo = versions?.[0]?.versionNo

  // diff 조각을 「한 벌의 본문 + 구간」으로 합쳐 마크다운 뷰어에 넘긴다(`toDiffSource`).
  const diffSource = useMemo(
    () => toDiffSource(comparison?.diff.parts ?? []),
    [comparison?.diff.parts],
  )
  const diffDecorations = useMemo(
    () =>
      diffSource.ranges.map((range) => ({
        start: range.start,
        end: range.end,
        render: (text: string) => (
          <span
            className={cx(
              range.kind === 'removed' && 'text-text-faint line-through',
              range.kind === 'added' && 'font-bold text-text',
            )}
          >
            {text}
          </span>
        ),
      })),
    [diffSource],
  )

  return (
    <div>
      <Toolbar className="mb-[18px]">
        <h1 className="font-display text-[19px] font-bold text-text">
          {document?.title ?? '문서'} · 버전 이력
        </h1>
        <ToolbarSpacer />
        <Link to={routes.documentDetail(workspaceId, documentId)}>
          <Button variant="outline">문서로 돌아가기</Button>
        </Link>
      </Toolbar>

      {isLoading && <p className="text-sm text-text-tertiary">불러오는 중…</p>}
      {isError && <p className="text-sm text-danger">이력을 불러오지 못했습니다.</p>}
      {versions?.length === 0 && (
        <p className="text-sm text-text-tertiary">아직 확정된 버전이 없습니다.</p>
      )}

      {versions && versions.length > 0 && (
        <TwoCol>
          <div className="flex w-[360px] shrink-0 flex-col gap-[10px]">
            {versions.map((version) => {
              const isCurrent = version.versionNo === currentVersionNo
              return (
                <TimelineItem
                  key={version.id}
                  tone={isCurrent ? 'current' : 'default'}
                  dim={!isCurrent && version.versionNo === 1}
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-bold">v{version.versionNo}</span>
                    <div className="flex items-center gap-2">
                      {version.edited && <Pill tone="neutral">직접 편집</Pill>}
                      {isCurrent && (
                        <span className="rounded-[5px] bg-accent-bg px-[7px] py-[2px] text-[10.5px] font-bold text-accent-strong">
                          현재
                        </span>
                      )}
                    </div>
                  </div>
                  <p className="my-1 text-[11px] text-text-quaternary">
                    {new Date(version.publishedAt).toLocaleString('ko-KR')} ·{' '}
                    {version.publishedByName ?? '—'}
                  </p>
                  <p className="text-xs text-text-secondary">
                    {version.edited
                      ? '사람이 본문을 직접 고친 버전입니다.'
                      : '업로드본 또는 교정 반영본입니다.'}
                  </p>
                  {version.dictionaryVersionNo !== undefined && (
                    <p className="mt-[6px] text-[10.5px] font-semibold text-accent-strong">
                      적용 사전집 r{version.dictionaryVersionNo}
                    </p>
                  )}
                </TimelineItem>
              )
            })}
          </div>

          <ColFlex>
            <Card className="flex flex-col gap-5 p-[22px]">
              <div>
                <div className="mb-3 flex items-center gap-2">
                  <span className="font-display text-[14.5px] font-bold">본문 비교</span>
                  <select
                    aria-label="비교 시작 버전"
                    value={compareFrom ?? ''}
                    onChange={(event) => setFromOverride(Number(event.target.value))}
                    className="cursor-pointer rounded-sm border border-border-strong px-2 py-1 text-xs"
                  >
                    {versions.map((version) => (
                      <option key={version.versionNo} value={version.versionNo}>
                        v{version.versionNo}
                      </option>
                    ))}
                  </select>
                  <span className="text-text-quaternary">→</span>
                  <select
                    aria-label="비교 대상 버전"
                    value={compareTo ?? ''}
                    onChange={(event) => setToOverride(Number(event.target.value))}
                    className="cursor-pointer rounded-sm border border-border-strong px-2 py-1 text-xs"
                  >
                    {versions.map((version) => (
                      <option key={version.versionNo} value={version.versionNo}>
                        v{version.versionNo}
                      </option>
                    ))}
                  </select>
                </div>

                {versions.length < 2 && (
                  <p className="text-sm text-text-tertiary">
                    비교하려면 버전이 둘 이상 필요합니다.
                  </p>
                )}
                {isLoadingDiff && <p className="text-sm text-text-tertiary">본문을 읽는 중…</p>}

                {comparison?.diff.tooLarge && (
                  <p className="text-[12.5px] text-text-tertiary">
                    본문이 너무 길어 인라인 비교를 생략했습니다.
                  </p>
                )}

                {comparison && !comparison.diff.tooLarge && (
                  <>
                    {/* 삭제분·추가분을 한 벌로 합친 본문을 마크다운으로 그리고, 어디가
                        지워지고 더해졌는지는 문자 구간으로 얹는다 — 문서 상세와 같은 모양으로
                        보면서 취소선·굵게 표시도 그대로 남는다. */}
                    <Markdown
                      source={diffSource.text}
                      decorations={diffDecorations}
                      className="text-[13.5px] leading-[1.9] text-text-secondary"
                    />
                    <p className="mt-2 text-[11px] text-text-quaternary">
                      공백 단위 근사 비교입니다 — 백엔드가 본문 diff를 만들지 않으므로(D-61)
                      화면에서 계산합니다. 바뀐 쪽과 지워진 쪽을 한 본문에 겹쳐 그리므로 제목·표
                      같은 블록이 통째로 바뀐 자리는 형태가 한쪽으로 치우쳐 보일 수 있습니다.
                    </p>
                  </>
                )}
              </div>

              <div className="border-t border-border-soft pt-4">
                <div className="mb-2 text-[12.5px] font-bold">처리 내역</div>
                <p className="mb-[10px] text-[11.5px] text-text-quaternary">
                  리뷰어는 이 기록으로 무엇이 왜 바뀌었는지 확인합니다
                </p>
                {(!history || history.length === 0) && (
                  <p className="text-xs text-text-tertiary">아직 처리한 제안이 없습니다.</p>
                )}
                {history && history.length > 0 && (
                  <DataTable className="text-xs">
                    <thead>
                      <tr>
                        <Th className="text-[10.5px]">원래</Th>
                        <Th className="text-[10.5px]">결과</Th>
                        <Th className="text-[10.5px]">처리</Th>
                      </tr>
                    </thead>
                    <tbody>
                      {history.map((item) => (
                        <Tr key={`${item.original}-${item.result}`}>
                          <Td>
                            {item.action === 'ignored' && (
                              <span className="mr-1 inline-block h-[5px] w-[5px] rounded-full bg-danger" />
                            )}
                            {item.original}
                          </Td>
                          <Td>{item.result}</Td>
                          <Td
                            className={cx(
                              item.action === 'applied' && 'font-semibold text-success',
                              item.action === 'manual' && 'font-semibold text-accent-strong',
                              item.action === 'ignored' && 'text-text-tertiary',
                            )}
                          >
                            {item.action === 'applied'
                              ? '적용'
                              : item.action === 'manual'
                                ? `직접 입력 → "${item.manualValue}"`
                                : `무시 — "${item.reason}"`}
                          </Td>
                        </Tr>
                      ))}
                    </tbody>
                  </DataTable>
                )}
              </div>

              <div className="flex items-center gap-3 border-t border-border-soft pt-4">
                <Button variant="outline" disabled>
                  이전 버전으로 되돌리기
                </Button>
                <Pill tone="neutral">MVP2</Pill>
                <span className="text-[11px] text-text-quaternary">
                  되돌리면 이전 내용이 새 버전으로 쌓이고 현재 버전은 이력에 남습니다.
                </span>
              </div>
            </Card>
          </ColFlex>
        </TwoCol>
      )}
    </div>
  )
}
