import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Avatar, Banner, Button, Card, DataTable, Pill, Td, Th, Tr } from '../../shared/ui'
import { ApiError } from '../../shared/api/httpClient'
import { useWorkspaceMembers } from '../../features/member/hooks/useWorkspaceMembers'
import { WORKSPACE_MEMBER_CAPACITY } from '../../features/member/model/fixtures'
import { useWorkspace } from '../../features/workspace/hooks/useWorkspace'
import {
  useChangeParticipantPermission,
  useRemoveParticipant,
} from '../../features/workspace/hooks/useParticipantAdmin'
import { useAuthStore } from '../../shared/stores/authStore'

// ui/main.js renderSettingsMembers() 이식.
//
// **(2026-09-14 디자인 정합)** 프로토타입에서 장식이던 「제외」를 실제
// `DELETE /api/workspaces/{id}/participants/{participantId}`로 연결하고, 역할 칸을
// OWNER가 바꿀 수 있는 선택으로 만들었다(`PATCH .../permission`).
// 소유권 이전은 별도 엔드포인트라 여기서 다루지 않는다.
export function SettingsMembersPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const { data: members } = useWorkspaceMembers(workspaceId)
  const { data: workspace } = useWorkspace(workspaceId)
  const currentMember = useAuthStore((state) => state.currentMember)
  const removeParticipant = useRemoveParticipant(workspaceId)
  const changePermission = useChangeParticipantPermission(workspaceId)
  const [error, setError] = useState<string | null>(null)

  const myPermission = workspace?.myPermission
  const isOwner = myPermission === 'OWNER'
  const isAdminOrAbove = isOwner || myPermission === 'ADMIN'
  const isFull = (members?.length ?? 0) >= WORKSPACE_MEMBER_CAPACITY

  function handleRemove(participantId: string, name: string) {
    if (!window.confirm(`${name} 님을 이 워크스페이스에서 제외하시겠습니까?`)) return
    setError(null)
    removeParticipant.mutate(
      { workspaceId, participantId },
      {
        onError: (caught) =>
          setError(caught instanceof ApiError ? caught.message : '제외하지 못했습니다.'),
      },
    )
  }

  return (
    <div className="max-w-[880px]">
      <Card className="overflow-hidden">
        <DataTable>
          <thead>
            <tr>
              <Th>이름</Th>
              <Th>이메일</Th>
              <Th>역할</Th>
              <Th>가입일</Th>
              <Th />
            </tr>
          </thead>
          <tbody>
            {members?.map((member) => {
              const isMe = member.id === currentMember?.id
              // Admin은 Regular만 내보낼 수 있고 Owner는 아무도 내보낼 수 없다.
              const canRemove =
                member.removable &&
                member.participantId !== undefined &&
                !isMe &&
                (isOwner || (myPermission === 'ADMIN' && member.permission === 'REGULAR'))

              return (
                <Tr key={member.id}>
                  <Td className="font-semibold text-text">
                    <span className="flex items-center gap-[9px]">
                      <Avatar initial={member.initial} tone="accent" size={26} />
                      {member.name}
                      {isMe && <span className="text-[10.5px] text-text-quaternary">(나)</span>}
                    </span>
                  </Td>
                  <Td>{member.email}</Td>
                  <Td>
                    {isOwner && member.permission !== 'OWNER' && member.participantId ? (
                      <select
                        aria-label={`${member.name} 역할`}
                        value={member.permission}
                        disabled={changePermission.isPending}
                        onChange={(event) => {
                          setError(null)
                          changePermission.mutate(
                            {
                              workspaceId,
                              participantId: member.participantId!,
                              permission: event.target.value as 'ADMIN' | 'REGULAR',
                            },
                            {
                              onError: (caught) =>
                                setError(
                                  caught instanceof ApiError
                                    ? caught.message
                                    : '역할을 바꾸지 못했습니다.',
                                ),
                            },
                          )
                        }}
                        className="cursor-pointer rounded-xs border border-border-strong bg-neutral-bg px-2 py-1 text-[11px] font-semibold text-text-secondary"
                      >
                        <option value="ADMIN">ADMIN</option>
                        <option value="REGULAR">REGULAR</option>
                      </select>
                    ) : (
                      <Pill tone="neutral">{member.permission}</Pill>
                    )}
                  </Td>
                  <Td>{new Date(member.joinedAt).toLocaleDateString('ko-KR')}</Td>
                  <Td className="text-right">
                    {canRemove ? (
                      <Button
                        variant="dangerText"
                        size="sm"
                        disabled={removeParticipant.isPending}
                        onClick={() => handleRemove(member.participantId!, member.name)}
                      >
                        제외
                      </Button>
                    ) : (
                      <span className="text-[11.5px] font-semibold text-text-disabled">제외</span>
                    )}
                  </Td>
                </Tr>
              )
            })}
          </tbody>
        </DataTable>
      </Card>

      {error && <p className="mt-3 text-xs text-danger">{error}</p>}

      {!isAdminOrAbove && (
        <Banner tone="neutral" className="mt-4">
          멤버를 초대하거나 제외하려면 ADMIN 이상 권한이 필요합니다.
        </Banner>
      )}

      {isFull && (
        <Banner className="mt-4">
          자리가 모두 찼습니다({members?.length}/{WORKSPACE_MEMBER_CAPACITY}). 더 초대하려면
          기존 멤버를 제외해야 합니다.
        </Banner>
      )}
    </div>
  )
}
