import { useState, type FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import {
  Avatar,
  Banner,
  Button,
  Card,
  DataTable,
  FieldLabel,
  Modal,
  Pill,
  Td,
  Th,
  TextInput,
  Tr,
} from '../../shared/ui'
import { ApiError } from '../../shared/api/httpClient'
import { routes } from '../../shared/config/routes'
import { useWorkspaceMembers } from '../../features/member/hooks/useWorkspaceMembers'
import { WORKSPACE_MEMBER_CAPACITY } from '../../features/member/model/fixtures'
import { useWorkspace } from '../../features/workspace/hooks/useWorkspace'
import {
  useChangeParticipantPermission,
  useRemoveParticipant,
} from '../../features/workspace/hooks/useParticipantAdmin'
import {
  useCancelInvitation,
  useInvitations,
  useIssueInvitation,
} from '../../features/workspace/hooks/useInvitations'
import type { InvitablePermission, Invitation } from '../../features/workspace/model/types'
import { useAuthStore } from '../../shared/stores/authStore'

// ui/main.js renderSettingsMembers() 이식.
//
// **(2026-09-14 디자인 정합)** 프로토타입에서 장식이던 「제외」를 실제
// `DELETE /api/workspaces/{id}/participants/{participantId}`로 연결하고, 역할 칸을
// OWNER가 바꿀 수 있는 선택으로 만들었다(`PATCH .../permission`).
// 소유권 이전은 별도 엔드포인트라 여기서 다루지 않는다.
//
// **(2026-09-16)** 멤버 초대를 붙였다(REQ-WS-003). 백엔드는 이미 있었고
// (`workspace/presentation/InvitationController`) 화면만 없었다 — 발급·대기 목록·취소가
// 여기 있고, 링크를 받은 사람이 들어가는 수락 화면은 `/invitations/:token`이다.
// **이메일 발송은 아직 없다** — 발급 응답의 토큰으로 링크를 만들어 복사해 전달한다.
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
  const memberCount = members?.length ?? 0
  const isFull = memberCount >= WORKSPACE_MEMBER_CAPACITY

  // 대기 중인 초대. 목록 조회가 ADMIN 이상 전용이라 Regular에게는 아예 호출하지 않는다.
  const { data: pendingInvitations } = useInvitations(workspaceId, 'PENDING', {
    enabled: isAdminOrAbove,
  })
  const issueInvitation = useIssueInvitation(workspaceId)
  const cancelInvitation = useCancelInvitation(workspaceId)

  const [isInviteOpen, setIsInviteOpen] = useState(false)
  const [inviteEmail, setInviteEmail] = useState('')
  const [invitePermission, setInvitePermission] = useState<InvitablePermission>('REGULAR')
  /** 발급 직후에만 손에 들어오는 토큰 — 모달을 닫으면 다시 볼 수 없다. */
  const [issuedInvitation, setIssuedInvitation] = useState<Invitation | null>(null)
  const [isLinkCopied, setIsLinkCopied] = useState(false)

  const pendingCount = pendingInvitations?.length ?? 0
  // 초대가 전부 수락되면 정원을 넘는 상태. 정원 판정 자체는 수락 시점에 서버가 한다.
  const isOverbooked = memberCount + pendingCount > WORKSPACE_MEMBER_CAPACITY
  const inviteLink = issuedInvitation?.token
    ? `${window.location.origin}${routes.invitationAccept(issuedInvitation.token)}`
    : ''

  function openInviteModal() {
    setInviteEmail('')
    setInvitePermission('REGULAR')
    setIssuedInvitation(null)
    setIsLinkCopied(false)
    issueInvitation.reset()
    setIsInviteOpen(true)
  }

  function closeInviteModal() {
    if (issueInvitation.isPending) return
    setIsInviteOpen(false)
  }

  function handleIssueInvitation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)

    const trimmedEmail = inviteEmail.trim()
    issueInvitation.mutate(
      {
        workspaceId,
        // 링크 복사 방식이면 이메일은 생략한다 — 빈 문자열을 보내면 @Email 검증에 걸린다.
        inviteeEmail: trimmedEmail === '' ? undefined : trimmedEmail,
        permission: invitePermission,
      },
      {
        onSuccess: (invitation) => {
          setIssuedInvitation(invitation)
          setIsLinkCopied(false)
        },
      },
    )
  }

  async function handleCopyLink() {
    try {
      await navigator.clipboard.writeText(inviteLink)
      setIsLinkCopied(true)
    } catch {
      // http(비 localhost)나 권한 거부 등으로 클립보드를 못 쓰는 환경이 있다 —
      // 그때는 입력칸에 그대로 보이는 링크를 직접 복사하면 된다.
      setIsLinkCopied(false)
    }
  }

  function handleCancelInvitation(invitation: Invitation) {
    const target = invitation.inviteeEmail ?? '링크 초대'
    if (!window.confirm(`${target} 초대를 취소하시겠습니까?`)) return
    setError(null)
    cancelInvitation.mutate(
      { workspaceId, invitationId: invitation.id },
      {
        onError: (caught) =>
          setError(caught instanceof ApiError ? caught.message : '초대를 취소하지 못했습니다.'),
      },
    )
  }

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

  const issueErrorMessage =
    issueInvitation.error instanceof ApiError
      ? issueInvitation.error.message
      : '초대를 만들지 못했습니다.'

  return (
    <div className="max-w-[880px]">
      <div className="mb-[14px] flex items-center justify-between">
        <p className="text-sm font-bold">
          멤버{' '}
          <span className="text-text-quaternary">
            {memberCount}/{WORKSPACE_MEMBER_CAPACITY}
          </span>
        </p>
        {isAdminOrAbove && (
          <Button variant="primary" size="sm" disabled={isFull} onClick={openInviteModal}>
            멤버 초대
          </Button>
        )}
      </div>

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
                    {/* 못 빼는 멤버도 같은 버튼을 비활성으로 둔다 — 글자만 두면 크기와
                        위치가 달라져 행마다 「제외」가 어긋나 보인다. */}
                    <Button
                      variant="dangerText"
                      size="sm"
                      disabled={!canRemove || removeParticipant.isPending}
                      onClick={() => handleRemove(member.participantId!, member.name)}
                    >
                      제외
                    </Button>
                  </Td>
                </Tr>
              )
            })}
          </tbody>
        </DataTable>
      </Card>

      {isAdminOrAbove && pendingCount > 0 && (
        <>
          <p className="mt-6 mb-[14px] text-sm font-bold">
            대기 중인 초대 <span className="text-text-quaternary">{pendingCount}</span>
          </p>
          <Card className="overflow-hidden">
            <DataTable>
              <thead>
                <tr>
                  <Th>초대 대상</Th>
                  <Th>역할</Th>
                  <Th>만료</Th>
                  <Th />
                </tr>
              </thead>
              <tbody>
                {pendingInvitations?.map((invitation) => (
                  <Tr key={invitation.id}>
                    <Td className="font-semibold text-text">
                      {invitation.inviteeEmail ?? (
                        <span className="font-normal text-text-tertiary">링크 초대</span>
                      )}
                    </Td>
                    <Td>
                      <Pill tone="neutral">{invitation.permission}</Pill>
                    </Td>
                    <Td>{new Date(invitation.expiresAt).toLocaleDateString('ko-KR')}</Td>
                    <Td className="text-right">
                      <Button
                        variant="dangerText"
                        size="sm"
                        disabled={cancelInvitation.isPending}
                        onClick={() => handleCancelInvitation(invitation)}
                      >
                        초대 취소
                      </Button>
                    </Td>
                  </Tr>
                ))}
              </tbody>
            </DataTable>
          </Card>
        </>
      )}

      {error && <p className="mt-3 text-xs text-danger">{error}</p>}

      {!isAdminOrAbove && (
        <Banner tone="neutral" className="mt-4">
          멤버를 초대하거나 제외하려면 ADMIN 이상 권한이 필요합니다.
        </Banner>
      )}

      {isFull && (
        <Banner className="mt-4">
          자리가 모두 찼습니다({memberCount}/{WORKSPACE_MEMBER_CAPACITY}). 더 초대하려면
          기존 멤버를 제외해야 합니다.
        </Banner>
      )}

      {!isFull && isOverbooked && (
        <Banner className="mt-4">
          대기 중인 초대가 모두 수락되면 정원({WORKSPACE_MEMBER_CAPACITY}명)을 넘습니다. 먼저
          수락한 사람까지만 참여할 수 있습니다.
        </Banner>
      )}

      <Modal
        open={isInviteOpen}
        onClose={closeInviteModal}
        title={issuedInvitation ? '초대 링크가 만들어졌습니다' : '멤버 초대'}
        footer={
          issuedInvitation ? (
            <>
              <Button variant="outline" onClick={openInviteModal}>
                계속 초대
              </Button>
              <Button variant="primary" onClick={() => setIsInviteOpen(false)}>
                완료
              </Button>
            </>
          ) : (
            <>
              <Button
                variant="outline"
                onClick={closeInviteModal}
                disabled={issueInvitation.isPending}
              >
                취소
              </Button>
              <Button
                type="submit"
                form="invite-member-form"
                variant="primary"
                disabled={issueInvitation.isPending}
              >
                {issueInvitation.isPending ? '만드는 중…' : '초대 링크 만들기'}
              </Button>
            </>
          )
        }
      >
        {issuedInvitation ? (
          <div className="flex flex-col gap-3">
            <p className="text-[12.5px] leading-[1.7] text-text-secondary">
              아래 링크를 초대할 사람에게 전달하세요. 링크를 연 사람은{' '}
              <b>{issuedInvitation.permission}</b> 권한으로 참여합니다.
              {issuedInvitation.inviteeEmail && (
                <> 대상으로 <b>{issuedInvitation.inviteeEmail}</b>을(를) 기록해 뒀습니다.</>
              )}
            </p>
            <div className="flex items-center gap-2">
              <TextInput
                readOnly
                value={inviteLink}
                aria-label="초대 링크"
                onFocus={(event) => event.target.select()}
              />
              <Button variant="outline" onClick={handleCopyLink}>
                {isLinkCopied ? '복사됨' : '복사'}
              </Button>
            </div>
            <p className="text-[11.5px] text-text-quaternary">
              이 링크는 {new Date(issuedInvitation.expiresAt).toLocaleDateString('ko-KR')}에
              만료됩니다. 지금 복사하지 않으면 다시 볼 수 없어 새로 발급해야 합니다.
            </p>
          </div>
        ) : (
          <form id="invite-member-form" onSubmit={handleIssueInvitation}>
            <FieldLabel>초대할 사람의 이메일</FieldLabel>
            <TextInput
              type="email"
              value={inviteEmail}
              onChange={(event) => setInviteEmail(event.target.value)}
              maxLength={320}
              autoFocus
              placeholder="비워두면 누구나 쓸 수 있는 링크가 됩니다"
            />
            <p className="mt-2 text-[11.5px] text-text-quaternary">
              이메일 발송은 아직 없습니다 — 적어두면 이 초대가 누구 것인지 대기 목록에
              표시되고, 이미 참여 중인 사람이면 발급을 막습니다.
            </p>

            <div className="mt-4">
              <FieldLabel required>역할</FieldLabel>
              <select
                aria-label="초대할 역할"
                value={invitePermission}
                onChange={(event) =>
                  setInvitePermission(event.target.value as InvitablePermission)
                }
                className="w-full cursor-pointer rounded-sm border border-border-strong bg-surface px-3 py-[9px] text-[13px] text-text outline-none focus:border-accent"
              >
                <option value="REGULAR">REGULAR</option>
                <option value="ADMIN">ADMIN</option>
              </select>
              <p className="mt-2 text-[11.5px] text-text-quaternary">
              </p>
            </div>

            {issueInvitation.isError && (
              <p className="mt-3 text-xs text-danger">{issueErrorMessage}</p>
            )}
          </form>
        )}
      </Modal>
    </div>
  )
}
