// Phase 3(라우팅/화면 골격) 전용 임시 스텁. 각 페이지 컴포넌트는 실제 화면 대신
// 이 컴포넌트를 렌더링해 라우팅 자체가 맞는지만 확인한다. 실제 UI 구현은 Phase 6에서
// 이 자리를 채워 넣는다 — 페이지 파일 이름과 경로는 이미 최종 구조를 따르므로
// Phase 6에서는 각 파일 내부만 교체하면 된다.
interface PagePlaceholderProps {
  title: string
  routeKey?: string
}

export function PagePlaceholder({ title, routeKey }: PagePlaceholderProps) {
  return (
    <div className="rounded-md border border-dashed border-border-strong bg-surface-muted p-6">
      <h2 className="font-display text-base font-bold text-text">{title}</h2>
      <p className="mt-1 text-xs text-text-tertiary">
        Phase 6에서 구현 예정
        {routeKey ? ` · ui/main.js state.screen: "${routeKey}"` : ''}
      </p>
    </div>
  )
}
