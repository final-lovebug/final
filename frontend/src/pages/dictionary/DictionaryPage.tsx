import { Link, useParams } from 'react-router-dom'
import { Button, Card, Pill } from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { useDictionary } from '../../features/dictionary/hooks/useDictionary'
import { TERM_IN_FOCUS_ID } from '../../features/dictionary/model/fixtures'
import { CURRENT_DICTIONARY_REVISION_ID } from '../../features/review/model/fixtures'
import { cx } from '../../shared/lib/cx'

export function DictionaryPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const { data, isLoading, isError } = useDictionary(workspaceId)

  if (isLoading) return <p className="text-sm text-text-tertiary">불러오는 중…</p>
  if (isError || !data) {
    return <p className="text-sm text-danger">사전집을 불러오지 못했습니다.</p>
  }

  const { dictionary, terms } = data

  return (
    <div>
      <div className="mb-1 flex items-center gap-3">
        <Button variant="outline" size="sm">
          r{dictionary.currentVersionNo} ▾
        </Button>
        <Pill tone="success">공식</Pill>
      </div>
      <p className="mb-3 text-[11.5px] text-text-quaternary">
        공식 리비전 · 대조 결과가 문서 검사에 그대로 적용됩니다
      </p>

      <Card className="mb-2 flex items-center justify-between bg-accent-bg-strong p-3 text-[12.5px]">
        <span>사전집 개정안에서 승인 대기 중인 변경 8건 — 용어 추가 5 · 정의 수정 3</span>
        <Link
          to={routes.dictionaryRevision(workspaceId, CURRENT_DICTIONARY_REVISION_ID)}
          className="font-semibold text-accent-strong"
        >
          개정안 보기 →
        </Link>
      </Card>
      <p className="mb-[18px] text-[11.5px] text-text-quaternary">
        승인이 완료되면 리비전이 자동으로 발행됩니다.
      </p>

      <div className="flex items-start gap-5">
        <Card className="flex-1 overflow-hidden">
          <table className="w-full border-collapse text-[12.5px]">
            <thead>
              <tr>
                {['통일 용어', '정의', '최종 수정'].map((heading) => (
                  <th
                    key={heading}
                    className="whitespace-nowrap border-b border-border-soft px-4 py-[11px] text-left text-[11px] font-semibold text-text-quaternary"
                  >
                    {heading}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {terms.map((term) => (
                <tr
                  key={term.id}
                  className={cx(term.id === TERM_IN_FOCUS_ID && 'bg-accent-bg-strong')}
                >
                  <td className="border-b border-border-faint px-4 py-3 font-bold text-text">
                    {term.preferredForm}
                    {term.englishName && (
                      <span className="ml-1 font-normal text-text-tertiary">
                        ({term.englishName})
                      </span>
                    )}
                  </td>
                  <td className="border-b border-border-faint px-4 py-3 text-text-secondary">
                    {term.definition}
                  </td>
                  <td className="border-b border-border-faint px-4 py-3 text-text-quaternary">
                    {new Date(term.updatedAt).toLocaleDateString('ko-KR')}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
        <Card className="w-[280px] shrink-0 bg-surface-muted p-4 opacity-60">
          <div className="mb-[6px] flex items-center gap-[6px]">
            <span className="text-[12.5px] font-bold">역인덱스 · 변경 이력</span>
            <Pill tone="neutral">MVP2</Pill>
          </div>
          <p className="text-[11.5px] leading-[1.6] text-text-tertiary">
            이 용어가 쓰인 문서 목록과 변경 이력 타임라인은 추후 제공됩니다. 지금은 사전집
            리비전 이력에서 확인할 수 있습니다.
          </p>
        </Card>
      </div>

      <div className="mt-4 flex gap-[10px]">
        <Link to={routes.dictionaryDraft(workspaceId)}>
          <Button variant="outline">직접 후보 등록</Button>
        </Link>
        <Button variant="outline">내보내기</Button>
      </div>
    </div>
  )
}
