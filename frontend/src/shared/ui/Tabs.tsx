import { cx } from '../lib/cx'

// ui/style.css .tab-row / .tab-item / .settings-tabs 이식.
// 설정 화면과 문서 검토 사이드 패널이 같은 탭 모양을 쓴다 — 크기만 다르다.

export interface TabDefinition<T extends string> {
  value: T
  label: string
}

interface TabRowProps<T extends string> {
  tabs: TabDefinition<T>[]
  value: T
  onChange: (value: T) => void
  /** 설정 화면용 큰 탭(글자 13px, 간격 22px). */
  size?: 'sm' | 'lg'
  className?: string
}

export function TabRow<T extends string>({
  tabs,
  value,
  onChange,
  size = 'sm',
  className,
}: TabRowProps<T>) {
  return (
    <div
      className={cx(
        'flex border-b border-border-soft',
        size === 'lg' ? 'gap-[22px]' : 'gap-4',
        className,
      )}
    >
      {tabs.map((tab) => (
        <button
          key={tab.value}
          type="button"
          onClick={() => onChange(tab.value)}
          className={cx(
            'cursor-pointer border-b-2 border-transparent pb-[9px] pt-[9px] text-text-quaternary',
            size === 'lg' ? 'text-[13px] font-semibold' : 'text-[12.5px]',
            tab.value === value && 'border-accent font-bold text-text',
          )}
        >
          {tab.label}
        </button>
      ))}
    </div>
  )
}
