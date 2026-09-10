import { useParams } from 'react-router-dom'
import { Button, Card, Pill } from '../../shared/ui'
import { useNotificationSettings } from '../../features/notification/hooks/useNotificationSettings'

// ui/main.js renderSettingsNotif() 이식. Slack/메일/웹 push는 MVP2/MVP3라 토글이 있어도
// 비활성 상태로만 보여준다(fe-task.md Phase 6 체크리스트 항목).
export function SettingsNotificationsPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const { data } = useNotificationSettings(workspaceId)

  return (
    <div className="flex max-w-[720px] flex-col gap-[22px]">
      <Card className="px-[22px] py-[6px]">
        {data?.channels.map((channel, idx) => (
          <div
            key={channel.channel}
            className={`flex items-center justify-between py-4 ${
              idx < data.channels.length - 1 ? 'border-b border-border-soft' : ''
            } ${channel.on ? '' : 'opacity-60'}`}
          >
            <div>
              <div className="flex items-center gap-2 text-[13.5px] font-bold">
                {channel.name}
                {channel.badge && <Pill tone="neutral">{channel.badge}</Pill>}
              </div>
              {channel.desc && (
                <p className="mt-[2px] text-[11.5px] text-text-quaternary">{channel.desc}</p>
              )}
            </div>
            <span
              className={`inline-block h-[22px] w-[38px] shrink-0 rounded-[11px] ${
                channel.on ? 'bg-accent' : 'bg-border-strong'
              } ${channel.badge ? 'cursor-not-allowed' : 'cursor-pointer'}`}
            />
          </div>
        ))}
      </Card>

      <div className="rounded-sm bg-neutral-bg p-3 text-[12.5px] text-text-secondary">
        카카오톡은 지원하지 않습니다 — 알림톡 발신에 사업자등록증과 템플릿 심사가 필요합니다.
      </div>

      <Card className="p-5">
        <table className="w-full border-collapse text-xs">
          <thead>
            <tr>
              <th className="border-b border-border-soft px-2 py-2 text-left text-text-quaternary">
                트리거
              </th>
              {['인앱', 'Slack', '메일', '웹push'].map((h, i) => (
                <th
                  key={h}
                  className={`border-b border-border-soft px-2 py-2 text-center text-text-quaternary ${
                    i > 0 ? 'opacity-50' : ''
                  }`}
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data?.matrix.map((row) => (
              <tr key={row.trigger}>
                <td className="border-b border-border-faint px-2 py-2 text-text-secondary">
                  {row.trigger}
                </td>
                <td className="border-b border-border-faint px-2 py-2 text-center font-bold text-success">
                  ✓
                </td>
                <td className="border-b border-border-faint px-2 py-2 text-center opacity-30">
                  –
                </td>
                <td className="border-b border-border-faint px-2 py-2 text-center opacity-30">
                  –
                </td>
                <td className="border-b border-border-faint px-2 py-2 text-center opacity-30">
                  –
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <p className="mt-[10px] text-[11.5px] leading-[1.7] text-text-tertiary">
          재검사 등급 리비전에서, 그 용어를 실제로 쓰는 문서의 작성자에게만 보냅니다. 용어
          추가만 있는 리비전은 보내지 않습니다.
        </p>
      </Card>

      <div className="flex justify-end">
        <Button variant="primary">저장</Button>
      </div>
    </div>
  )
}
