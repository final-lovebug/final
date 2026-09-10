import { useParams } from 'react-router-dom'
import { Avatar, Card, Pill } from '../../shared/ui'
import { useWorkspaceMembers } from '../../features/member/hooks/useWorkspaceMembers'
import { WORKSPACE_MEMBER_CAPACITY } from '../../features/member/model/fixtures'

export function SettingsMembersPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const { data: members } = useWorkspaceMembers(workspaceId)

  const isFull = (members?.length ?? 0) >= WORKSPACE_MEMBER_CAPACITY

  return (
    <div className="max-w-[880px]">
      <Card className="overflow-hidden">
        <table className="w-full border-collapse text-[12.5px]">
          <thead>
            <tr>
              {['이름', '이메일', '역할', '가입일', ''].map((h) => (
                <th
                  key={h}
                  className="whitespace-nowrap border-b border-border-soft px-4 py-[11px] text-left text-[11px] font-semibold text-text-quaternary"
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {members?.map((member) => (
              <tr key={member.id}>
                <td className="flex items-center gap-[9px] border-b border-border-faint px-4 py-3 font-semibold text-text">
                  <Avatar initial={member.initial} tone="accent" size={26} />
                  {member.name}
                </td>
                <td className="border-b border-border-faint px-4 py-3 text-text-secondary">
                  {member.email}
                </td>
                <td className="border-b border-border-faint px-4 py-3">
                  <Pill tone="neutral">{member.permission}</Pill>
                </td>
                <td className="border-b border-border-faint px-4 py-3 text-text-secondary">
                  {member.joinedAt}
                </td>
                <td className="border-b border-border-faint px-4 py-3 text-right">
                  <span
                    className={`text-[11.5px] font-semibold ${
                      member.removable ? 'text-danger' : 'text-text-disabled'
                    }`}
                  >
                    제외
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
      {isFull && (
        <div className="mt-4 rounded-sm border border-accent-border bg-accent-bg-strong p-3 text-[12.5px] text-text">
          자리가 모두 찼습니다({members?.length}/{WORKSPACE_MEMBER_CAPACITY}). 더 초대하려면
          기존 멤버를 제외해야 합니다.
        </div>
      )}
    </div>
  )
}
