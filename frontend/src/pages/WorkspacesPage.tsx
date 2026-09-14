import { Link } from 'react-router-dom'
import { Card } from '../shared/ui'
import { routes } from '../shared/config/routes'
import { useWorkspaces } from '../features/workspace/hooks/useWorkspaces'

// Phase 5 데이터 계층 검증용 첫 화면. TanStack Query(목업 api) → hooks → 페이지로 이어지는
// 경로가 실제로 동작하는지 확인하는 목적이 커서, 카드 디자인은 ui/style.css의
// .workspace-card 수준까지는 아직 안 갔다 — 시각적 완성도는 Phase 6에서 맞춘다.
export function WorkspacesPage() {
  const { data: workspaces, isLoading, isError } = useWorkspaces()

  return (
    <div className="min-h-screen bg-bg p-10">
      <h1 className="mb-6 font-display text-xl font-bold text-text">
        워크스페이스 선택
      </h1>

      {isLoading && (
        <p className="text-sm text-text-tertiary">불러오는 중…</p>
      )}
      {isError && (
        <p className="text-sm text-danger">워크스페이스를 불러오지 못했습니다.</p>
      )}

      <div className="grid grid-cols-3 gap-[18px]">
        {workspaces?.map((workspace) => (
          <Link key={workspace.id} to={routes.documents(workspace.id)}>
            <Card className="flex flex-col gap-2 p-[22px] shadow-card hover:shadow-card-hover">
              <span className="font-display text-[15px] font-bold text-text">
                {workspace.name}
              </span>
              {/* 실 API(WorkspaceResponse)는 updatedAt을 안 내려준다(docs/API.md 461행) —
                  T-INT-9(2026-09-14)에서 발견. 목업엔 있었으므로 있으면 "최근 수정",
                  없으면 createdAt을 "생성일"로 대신 보여준다. */}
              <span className="text-[11px] text-text-tertiary">
                {workspace.updatedAt
                  ? `최근 수정 ${new Date(workspace.updatedAt).toLocaleDateString('ko-KR')}`
                  : `생성일 ${new Date(workspace.createdAt).toLocaleDateString('ko-KR')}`}
              </span>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  )
}
