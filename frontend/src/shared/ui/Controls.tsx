import { cx } from '../lib/cx'

// ui/style.css .checkbox / .radio-dot / .toggle 이식.
// 프로토타입에서는 전부 정적 div였다 — 여기서는 onChange를 받으면 버튼으로, 없으면
// 읽기 전용 표시로 동작한다(예: 추출 대상 문서 목록은 서버가 고르므로 표시만 한다).

interface CheckboxProps {
  checked: boolean
  onChange?: (checked: boolean) => void
  disabled?: boolean
  label?: string
}

export function Checkbox({ checked, onChange, disabled, label }: CheckboxProps) {
  const box = (
    <span
      className={cx(
        'flex h-[15px] w-[15px] shrink-0 items-center justify-center rounded-[4px] text-[10px] text-white',
        checked ? 'bg-accent' : 'border-[1.5px] border-text-disabled',
      )}
    >
      {checked ? '✓' : ''}
    </span>
  )

  if (!onChange) return box

  return (
    <button
      type="button"
      aria-label={label}
      aria-pressed={checked}
      disabled={disabled}
      onClick={() => onChange(!checked)}
      className="flex cursor-pointer items-center disabled:cursor-not-allowed"
    >
      {box}
    </button>
  )
}

export function RadioDot({ checked }: { checked: boolean }) {
  return (
    <span
      className={cx(
        'relative block h-[15px] w-[15px] shrink-0 rounded-full border-[1.5px]',
        checked ? 'border-accent' : 'border-text-disabled',
      )}
    >
      {checked && <span className="absolute inset-[3px] rounded-full bg-accent" />}
    </span>
  )
}

interface ToggleProps {
  on: boolean
  onChange?: (on: boolean) => void
  disabled?: boolean
  label?: string
}

export function Toggle({ on, onChange, disabled, label }: ToggleProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={on}
      aria-label={label}
      disabled={disabled || !onChange}
      onClick={() => onChange?.(!on)}
      className={cx(
        'relative h-[22px] w-[38px] shrink-0 rounded-pill transition-colors',
        on ? 'bg-accent' : 'bg-border-strong',
        onChange && !disabled ? 'cursor-pointer' : 'cursor-default',
      )}
    >
      <span
        className={cx(
          'absolute top-[2px] h-[18px] w-[18px] rounded-full bg-white transition-[left]',
          on ? 'left-[18px]' : 'left-[2px]',
        )}
      />
    </button>
  )
}
