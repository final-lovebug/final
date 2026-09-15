import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  Avatar,
  Button,
  FieldLabel,
  LogoMark,
  Modal,
  Pill,
  TextInput,
} from '../shared/ui'
import { routes } from '../shared/config/routes'
import { useAuthStore } from '../shared/stores/authStore'
import { useWorkspaceOverviews } from '../features/workspace/hooks/useWorkspaceOverviews'
import { useCreateWorkspace } from '../features/workspace/hooks/useCreateWorkspace'
import { ApiError } from '../shared/api/httpClient'

// ui/main.js renderWorkspaces() 이식. 프로토타입의 카드는 이름 + 사전집 배지 + 멤버/문서 수
// + 활동 요약으로 되어 있는데, 목록 API가 이름만 주므로 워크스페이스마다 세 조회를 더해
// 채운다(features/workspace/api/fetchWorkspaceOverviews.ts 주석 참고).
//
// 프로토타입의 "새 워크스페이스 만들기" 카드는 `cursor:not-allowed`인 장식이었다 —
// 실제로는 생성 API가 있으므로 눌리게 하고 모달을 연다.
export function WorkspacesPage() {
  const { data: workspaces, isLoading, isError } = useWorkspaceOverviews()
  const currentMember = useAuthStore((state) => state.currentMember)
  const [isCreateFormOpen, setIsCreateFormOpen] = useState(false)
  const [workspaceName, setWorkspaceName] = useState('')
  const createWorkspace = useCreateWorkspace()
  const navigate = useNavigate()

  const trimmedWorkspaceName = workspaceName.trim()

  function closeCreateForm() {
    if (createWorkspace.isPending) return
    setWorkspaceName('')
    setIsCreateFormOpen(false)
  }

  function handleCreateWorkspace(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!trimmedWorkspaceName) return

    createWorkspace.mutate(trimmedWorkspaceName, {
      onSuccess: (workspace) => navigate(routes.documents(workspace.id)),
    })
  }

  const createErrorMessage =
    createWorkspace.error instanceof ApiError
      ? createWorkspace.error.message
      : '워크스페이스를 만들지 못했습니다. 잠시 후 다시 시도해주세요.'

  return (
    <div className="min-h-screen bg-bg">
      <div className="flex h-[72px] items-center justify-between px-10">
        <div className="flex items-center gap-2 font-display text-base font-bold">
          <LogoMark size={26} />
          유비쿼터스
        </div>
        <Avatar initial={currentMember?.displayName.charAt(0) ?? ''} tone="accent" size={32} />
      </div>

      <div className="mx-auto mt-9 max-w-[1040px] px-6 pb-[60px]">
        <h1 className="mb-7 font-display text-[26px] font-bold">워크스페이스를 선택하세요</h1>

        {isLoading && <p className="text-sm text-text-tertiary">불러오는 중…</p>}
        {isError && <p className="text-sm text-danger">워크스페이스를 불러오지 못했습니다.</p>}

        <div className="grid grid-cols-1 gap-[18px] sm:grid-cols-2 lg:grid-cols-3">
          {workspaces?.map((workspace) => (
            <Link
              key={workspace.id}
              to={routes.documents(workspace.id)}
              className="flex flex-col gap-[14px] rounded-[14px] border border-border bg-surface p-[22px] shadow-card hover:border-accent-border hover:shadow-card-hover"
            >
              <div className="font-display text-[15px] font-bold text-text">
                {workspace.name}
              </div>

              <div className="flex items-center gap-2">
                {workspace.dictionaryVersionNo === null ? (
                  <Pill tone="neutral">사전집 없음</Pill>
                ) : (
                  <Pill tone="accent">사전집 r{workspace.dictionaryVersionNo}</Pill>
                )}
                {workspace.myPermission && (
                  <Pill tone="outline">{workspace.myPermission}</Pill>
                )}
              </div>

              <div className="flex justify-between border-t border-border-soft pt-3 text-xs text-text-tertiary">
                <span>멤버 {workspace.memberCount}</span>
                <span>문서 {workspace.documentCount}</span>
              </div>

              <div className="text-[11px] text-text-quaternary">
                {new Date(workspace.createdAt).toLocaleDateString('ko-KR')} 생성
              </div>
            </Link>
          ))}

          <button
            type="button"
            onClick={() => setIsCreateFormOpen(true)}
            className="flex min-h-[150px] cursor-pointer flex-col items-center justify-center gap-2 rounded-[14px] border-[1.5px] border-dashed border-text-disabled p-[22px] text-text-quaternary hover:border-accent hover:text-accent-strong"
          >
            <span className="text-[22px] leading-none">+</span>
            <span className="text-[12.5px] font-semibold">새 워크스페이스 만들기</span>
          </button>
        </div>
      </div>

      <Modal
        open={isCreateFormOpen}
        onClose={closeCreateForm}
        title="새 워크스페이스 만들기"
        footer={
          <>
            <Button variant="outline" onClick={closeCreateForm} disabled={createWorkspace.isPending}>
              취소
            </Button>
            <Button
              type="submit"
              form="create-workspace-form"
              variant="primary"
              disabled={!trimmedWorkspaceName || createWorkspace.isPending}
            >
              {createWorkspace.isPending ? '만드는 중…' : '만들기'}
            </Button>
          </>
        }
      >
        <form id="create-workspace-form" onSubmit={handleCreateWorkspace}>
          <FieldLabel required>워크스페이스 이름</FieldLabel>
          <TextInput
            value={workspaceName}
            onChange={(event) => setWorkspaceName(event.target.value)}
            maxLength={50}
            autoFocus
            required
            placeholder="예: 개발팀"
          />
          <p className="mt-2 text-[11.5px] text-text-quaternary">
            만든 사람이 OWNER로 등록되고, 리뷰 규칙은 기본값(0/0)으로 시작합니다.
          </p>
          {createWorkspace.isError && (
            <p className="mt-2 text-xs text-danger">{createErrorMessage}</p>
          )}
        </form>
      </Modal>
    </div>
  )
}
