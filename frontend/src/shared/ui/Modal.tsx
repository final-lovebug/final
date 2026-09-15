import { useEffect, type ReactNode } from 'react'
import { cx } from '../lib/cx'

// ui/style.css .modal-overlay / .modal-box 이식.
// 프로토타입에는 없던 것 둘을 더했다 — ESC로 닫기와 오버레이 클릭으로 닫기.
// 목업에서는 모달이 상태 하나로만 열리고 닫혔지만 실제 화면에서는 취소 경로가 필요하다.

interface ModalProps {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  /** 하단 버튼 줄. */
  footer?: ReactNode
  className?: string
}

export function Modal({ open, onClose, title, children, footer, className }: ModalProps) {
  useEffect(() => {
    if (!open) return
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [open, onClose])

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-[rgba(20,22,26,0.4)] p-4"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(event) => event.stopPropagation()}
        className={cx(
          'flex w-[480px] max-w-full flex-col gap-4 rounded-[14px] bg-surface p-[26px] shadow-modal',
          className,
        )}
      >
        <div className="font-display text-[17px] font-bold">{title}</div>
        {children}
        {footer && <div className="flex justify-end gap-[10px]">{footer}</div>}
      </div>
    </div>
  )
}
