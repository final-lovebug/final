import type { HTMLAttributes, InputHTMLAttributes, TextareaHTMLAttributes } from 'react'
import { cx } from '../lib/cx'

// ui/style.css .field-label / .input-box / .req-mark 이식.
// 프로토타입은 입력을 전부 죽은 div로 그렸지만 여기서는 실제 input/textarea다 —
// 시각 규격(테두리·반경·여백·글자 크기)만 그대로 가져온다.

interface FieldLabelProps extends HTMLAttributes<HTMLDivElement> {
  /** 필수 표시(*). ui의 `.req-mark`. */
  required?: boolean
}

export function FieldLabel({ required, children, className, ...rest }: FieldLabelProps) {
  return (
    <div className={cx('mb-2 text-xs font-bold text-text', className)} {...rest}>
      {children}
      {required && <span className="ml-1 text-danger">*</span>}
    </div>
  )
}

export function TextInput({ className, ...rest }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={cx(
        'w-full rounded-sm border border-border-strong bg-surface px-3 py-[9px] text-[13px] text-text outline-none placeholder:text-text-quaternary focus:border-accent',
        className,
      )}
      {...rest}
    />
  )
}

export function TextArea({ className, ...rest }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <textarea
      className={cx(
        'w-full rounded-sm border border-border-strong bg-surface px-3 py-[9px] text-[13px] leading-[1.7] text-text outline-none placeholder:text-text-quaternary focus:border-accent',
        className,
      )}
      {...rest}
    />
  )
}
