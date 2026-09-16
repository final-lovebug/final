import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { cx } from '../lib/cx'

// ui/style.css의 .btn 계열 클래스를 이식한 것. .btn-disabled는 별도 variant로 두지 않고
// 네이티브 disabled 속성 + Tailwind disabled: 변형으로 대체했다.
export type ButtonVariant = 'default' | 'primary' | 'outline' | 'dangerText' | 'link'
export type ButtonSize = 'md' | 'sm'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
  children: ReactNode
}

const baseClass =
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-sm border border-transparent font-semibold cursor-pointer disabled:cursor-not-allowed'

// **(2026-09-16)** 비활성 표현을 baseClass에서 variant로 내렸다. 박스가 있는 버튼은
// 종전대로 회색 박스로 죽이고, 텍스트처럼 생긴 dangerText·link는 글자색만 죽인다 —
// 텍스트 버튼에 박스를 씌우면 같은 자리의 활성/비활성이 서로 다른 크기로 보인다.
const disabledBoxClass =
  'disabled:border-border-soft disabled:bg-bg disabled:text-text-faint disabled:hover:bg-bg'

const sizeClass: Record<ButtonSize, string> = {
  md: 'px-[18px] py-[9px] text-[13px]',
  sm: 'px-[13px] py-[7px] text-[12.5px]',
}

const variantClass: Record<ButtonVariant, string> = {
  default: `bg-surface text-text hover:bg-bg ${disabledBoxClass}`,
  outline: `border-border-strong bg-surface text-text hover:bg-bg ${disabledBoxClass}`,
  primary: `border-accent bg-accent text-white hover:bg-accent-strong ${disabledBoxClass}`,
  dangerText:
    'bg-surface text-danger hover:bg-bg disabled:bg-surface disabled:text-text-disabled disabled:hover:bg-surface',
  link: 'border-none bg-transparent p-0 text-[12px] text-accent-strong hover:text-accent disabled:text-text-disabled',
}

export function Button({
  variant = 'default',
  size = 'md',
  type = 'button',
  className,
  children,
  ...rest
}: ButtonProps) {
  const isLink = variant === 'link'

  return (
    <button
      type={type}
      className={cx(
        !isLink && baseClass,
        !isLink && sizeClass[size],
        variantClass[variant],
        className,
      )}
      {...rest}
    >
      {children}
    </button>
  )
}
