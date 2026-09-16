import { DataTable, Td, Th, Tr } from '../../../shared/ui'
import { cx } from '../../../shared/lib/cx'
import type { SuggestionHistoryItem } from '../model/types'

/**
 * 「처리 내역」 표 — 교정에서 무엇을 제안어로 바꿨고 무엇을 그대로 뒀는지.
 *
 * 두 화면이 같은 표를 그린다(초안 화면의 탭, 문서 버전 이력). 예전에는 각자 마크업을 들고
 * 있어 색과 열 구성이 달랐다 — 한 곳으로 모은다. 개정안 검토 화면은 행마다 코멘트를 달아야
 * 해서 이 표를 쓰지 않는다.
 *
 * **적용은 초록, 무시는 빨강**이다. 같은 회색 계열이면 표를 훑을 때 둘이 구분되지 않는다.
 */
interface Props {
  items: SuggestionHistoryItem[]
  /** 320px 사이드 패널처럼 좁은 자리에서 여백을 줄인다. */
  dense?: boolean
  emptyText?: string
}

export function SuggestionHistoryTable({
  items,
  dense = false,
  emptyText = '아직 처리한 제안이 없습니다.',
}: Props) {
  if (items.length === 0) {
    return <p className="text-xs text-text-tertiary">{emptyText}</p>
  }

  const headClass = cx('text-[10.5px]', dense && 'px-2')
  const cellClass = dense ? 'px-2 py-2' : undefined

  return (
    <DataTable className="text-xs">
      <thead>
        <tr>
          <Th className={headClass}>원래</Th>
          <Th className={headClass}>결과</Th>
          <Th className={headClass}>처리</Th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => (
          <Tr key={`${item.original}-${item.result}`}>
            <Td className={cellClass}>
              {item.action === 'ignored' && (
                <span className="mr-1 inline-block h-[5px] w-[5px] rounded-full bg-danger" />
              )}
              {item.original}
            </Td>
            <Td className={cellClass}>{item.result}</Td>
            <Td className={cellClass}>
              {item.action === 'applied' && (
                <span className="font-semibold text-success">적용</span>
              )}
              {item.action === 'manual' && (
                <span className="font-semibold text-accent-strong">
                  직접 입력 → "{item.manualValue}"
                </span>
              )}
              {item.action === 'ignored' && (
                <>
                  <span className="font-semibold text-danger">무시</span>
                  <span className="text-text-tertiary"> — "{item.reason}"</span>
                </>
              )}
            </Td>
          </Tr>
        ))}
      </tbody>
    </DataTable>
  )
}
