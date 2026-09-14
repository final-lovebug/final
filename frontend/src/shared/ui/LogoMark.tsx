import { cx } from '../lib/cx'

// ui/style.css .logo-mark 이식. 로그인 카드(44px)·워크스페이스 탑바(26px)·사이드바(24px)가
// 크기만 다르게 쓴다. Avatar와 달리 항상 accent 배경에 흰 글자이고 모서리가 둥근 사각형이다.
export function LogoMark({
  size = 26,
  radius,
  children = 'U',
  className,
}: {
  size?: number
  radius?: number
  children?: string
  className?: string
}) {
  return (
    <span
      className={cx(
        'inline-flex shrink-0 items-center justify-center bg-accent font-display font-bold text-white',
        className,
      )}
      style={{
        width: size,
        height: size,
        borderRadius: radius ?? Math.round(size * 0.27),
        fontSize: Math.round(size * 0.42),
      }}
    >
      {children}
    </span>
  )
}
