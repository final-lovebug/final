import { Avatar, Button, Card, Pill } from './shared/ui'

// 임시 스모크 테스트 화면. fe-task.md Phase 2(디자인 시스템 이식)에서 만든 shared/ui
// 컴포넌트가 실제로 렌더링되는지 확인하는 용도이며, Phase 3(라우팅/화면 골격)에서
// react-router-dom 기반 라우터와 AppLayout으로 교체된다.
function App() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-bg">
      <Card className="flex w-[360px] flex-col gap-4 p-6 shadow-card">
        <div className="flex items-center gap-3">
          <Avatar initial="민" tone="accent" />
          <div>
            <h1 className="font-display text-[15px] font-bold text-text">
              유비쿼터스 언어 사전
            </h1>
            <p className="text-[11px] text-text-tertiary">
              디자인 시스템 이식 확인 (Phase 2)
            </p>
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          <Pill tone="accent">사전집 r7</Pill>
          <Pill tone="warn">검토 대기</Pill>
          <Pill tone="success">적용됨</Pill>
        </div>

        <div className="flex gap-2">
          <Button variant="primary">확인</Button>
          <Button variant="outline">취소</Button>
          <Button variant="link">자세히 보기</Button>
        </div>
      </Card>
    </div>
  )
}

export default App
