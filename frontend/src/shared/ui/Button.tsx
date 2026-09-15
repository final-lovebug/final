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
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-sm border border-transparent font-semibold cursor-pointer disabled:cursor-not-allowed disabled:border-border-soft disabled:bg-bg disabled:text-text-faint disabled:hover:bg-bg'

const sizeClass: Record<ButtonSize, string> = {
  md: 'px-[18px] py-[9px] text-[13px]',
  sm: 'px-[13px] py-[7px] text-[12.5px]',
}

const variantClass: Record<ButtonVariant, string> = {
  default: 'bg-surface text-text hover:bg-bg',
  outline: 'border-border-strong bg-surface text-text hover:bg-bg',
  primary: 'border-accent bg-accent text-white hover:bg-accent-strong',
  dangerText: 'bg-surface text-danger hover:bg-bg',
  link: 'border-none bg-transparent p-0 text-[12px] text-accent-strong hover:text-accent',
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
