import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  Banner,
  Button,
  Card,
  ColFlex,
  DataTable,
  Pill,
  Td,
  Th,
  Tr,
  TwoCol,
} from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { cx } from '../../shared/lib/cx'
import { downloadCsv } from '../../shared/lib/downloadCsv'
import { useDictionary } from '../../features/dictionary/hooks/useDictionary'
import { useDictionaryVersions } from '../../features/dictionary/hooks/useDictionaryVersions'
import { useDictionaryRevision } from '../../features/review/hooks/useDictionaryRevision'
import { CURRENT_DICTIONARY_REVISION_ID } from '../../features/review/model/fixtures'

// ui/main.js renderDictionaryScreen() 이식.
//
// **(2026-09-14 디자인 정합)** 프로토타입의 버전 드롭다운(r7 ▾ → r6·r5·r4·all)과
// 「승인 대기 중인 변경 N건」 배너를 실제 데이터로 살렸다. 전자는 버전 목록 API로,
// 후자는 진행 중인 사전 개정안의 후보어 행을 세어서 만든다 — 둘 다 하드코딩이었다.
export function DictionaryPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const { data: versions } = useDictionaryVersions(workspaceId)
  const [selectedVersionNo, setSelectedVersionNo] = useState<number | undefined>(undefined)
  const { data, isLoading, isError } = useDictionary(workspaceId, selectedVersionNo)

  // 진행 중인 개정안이 없으면 에러다 — 배너를 감추기만 하고 화면은 그대로 둔다.
  const { data: pendingRevision } = useDictionaryRevision(
    workspaceId,
    CURRENT_DICTIONARY_REVISION_ID,
  )

  const [versionMenuOpen, setVersionMenuOpen] = useState(false)
  const versionMenuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!versionMenuOpen) return
    function handleClickOutside(event: MouseEvent) {
      if (versionMenuRef.current && !versionMenuRef.current.contains(event.target as Node)) {
        setVersionMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [versionMenuOpen])

  if (isLoading) return <p className="text-sm text-text-tertiary">불러오는 중…</p>
  if (isError || !data) {
    return (
      <div>
        <p className="text-sm text-text-tertiary">
          아직 이 워크스페이스에는 사전집이 없습니다. 사전집은 문서에서 용어를 추출해 리뷰를
          통과시키면 만들어집니다.
        </p>
        <Link to={routes.termExtraction(workspaceId)}>
          <Button variant="primary" className="mt-4">
            용어 추출 실행
          </Button>
        </Link>
      </div>
    )
  }

  const { dictionary, terms } = data
  const isArchived = dictionary.status === 'ARCHIVED'

  // 개정안 행의 변경 유형은 `추가`(신규)와 `승계`(이전 버전에서 넘어옴) 둘이다(`D-89`).
  // 판정을 걷어낸 뒤 「정의 수정」은 판별할 수 없게 됐다 — 활성 사전집과 값을 비교해야
  // 알 수 있고 목록 응답에 정의가 없다(`D-41`). 그래서 세는 축도 그 둘로 바꿨다.
  const addedCount = pendingRevision?.rows.filter((row) => row.change === '추가').length ?? 0
  const carriedOverCount = pendingRevision?.rows.filter((row) => row.change === '승계').length ?? 0
  const pendingCount = pendingRevision?.rows.length ?? 0

  function handleExport() {
    const rows = [
      ['표준어', '영문명', '정의', '최종 수정'],
      ...terms.map((term) => [
        term.preferredForm,
        term.englishName ?? '',
        term.definition ?? '',
        term.updatedAt ?? '',
      ]),
    ]
    downloadCsv(`사전집_r${dictionary.currentVersionNo}.csv`, rows)
  }

  return (
    <div>
      <div ref={versionMenuRef} className="relative mb-[6px] flex items-center gap-3">
        <Button variant="outline" size="sm" onClick={() => setVersionMenuOpen((open) => !open)}>
          r{dictionary.currentVersionNo} ▾
        </Button>
        <Pill tone={isArchived ? 'neutral' : 'success'}>{isArchived ? '보관' : '공식'}</Pill>

        {versionMenuOpen && (
          <div className="absolute left-0 top-[38px] z-10 w-[180px] overflow-hidden rounded-[10px] border border-border bg-surface text-[12.5px] shadow-pop">
            {versions?.map((version) => (
              <button
                key={version.versionNo}
                type="button"
                onClick={() => {
                  setSelectedVersionNo(
                    version.status === 'ACTIVE' ? undefined : version.versionNo,
                  )
                  setVersionMenuOpen(false)
                }}
                className={cx(
                  'block w-full cursor-pointer border-b border-border-soft px-[13px] py-[9px] text-left last:border-b-0 hover:bg-bg',
                  version.versionNo === dictionary.currentVersionNo
                    ? 'bg-accent-bg font-bold text-accent-strong'
                    : 'text-text-tertiary',
                )}
              >
                r{version.versionNo}
              </button>
            ))}
            <Link
              to={routes.dictionaryHistory(workspaceId)}
              className="block border-t border-border-strong px-[13px] py-[9px] font-semibold text-accent-strong hover:bg-bg"
            >
              all
            </Link>
          </div>
        )}
      </div>

      <p className="mb-[14px] text-[11.5px] text-text-quaternary">
        {isArchived
          ? '보관 리비전 · 현재 문서 검사에는 활성 리비전이 쓰입니다'
          : '공식 리비전 · 대조 결과가 문서 검사에 그대로 적용됩니다'}
      </p>

      {pendingCount > 0 && (
        <>
          <Banner className="mb-2 flex items-center justify-between gap-3">
            <span>
              사전집 개정안에서 승인 대기 중인 용어 {pendingCount}건 — 신규 {addedCount} ·
              승계 {carriedOverCount}
            </span>
            <Link
              to={routes.dictionaryRevision(workspaceId, CURRENT_DICTIONARY_REVISION_ID)}
              className="shrink-0 text-xs font-semibold text-accent-strong"
            >
              개정안 보기 →
            </Link>
          </Banner>
          <p className="mb-[18px] text-[11.5px] text-text-quaternary">
            승인이 완료되면 리비전이 자동으로 발행됩니다.
          </p>
        </>
      )}

      <TwoCol>
        <ColFlex>
          <Card className="overflow-hidden">
            <DataTable>
              <thead>
                <tr>
                  <Th>통일 용어</Th>
                  <Th>정의</Th>
                  <Th>최종 수정</Th>
                </tr>
              </thead>
              <tbody>
                {terms.length === 0 && (
                  <Tr>
                    <Td colSpan={3} className="text-text-tertiary">
                      이 리비전에는 용어가 없습니다.
                    </Td>
                  </Tr>
                )}
                {terms.map((term) => (
                  <Tr key={term.id}>
                    <Td className="font-bold text-text">
                      {term.preferredForm}
                      {term.englishName && (
                        <span className="ml-1 font-normal text-text-tertiary">
                          ({term.englishName})
                        </span>
                      )}
                    </Td>
                    {/* 실 API 목록 응답엔 definition·updatedAt이 없다(D-41) — "—"로 표시한다. */}
                    <Td>{term.definition ?? '—'}</Td>
                    <Td className="text-text-quaternary">
                      {term.updatedAt
                        ? new Date(term.updatedAt).toLocaleDateString('ko-KR')
                        : '—'}
                    </Td>
                  </Tr>
                ))}
              </tbody>
            </DataTable>
          </Card>
        </ColFlex>

        <Card className="w-[280px] shrink-0 border-border-soft bg-surface-muted p-4 opacity-60">
          <div className="mb-[6px] flex items-center gap-[6px]">
            <span className="text-[12.5px] font-bold">역인덱스 · 변경 이력</span>
            <Pill tone="neutral">MVP2</Pill>
          </div>
          <p className="text-[11.5px] leading-[1.6] text-text-tertiary">
            이 용어가 쓰인 문서 목록과 변경 이력 타임라인은 추후 제공됩니다. 지금은 사전집
            리비전 이력에서 확인할 수 있습니다.
          </p>
        </Card>
      </TwoCol>

      <div className="mt-4 flex gap-[10px]">
        <Link to={routes.dictionaryDraft(workspaceId)}>
          <Button variant="outline">직접 후보 등록</Button>
        </Link>
        <Button variant="outline" onClick={handleExport}>
          내보내기
        </Button>
      </div>
    </div>
  )
}
