import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import type { NotificationChannel } from '../../shared/types/common'
import { Button, Card, Pill } from '../../shared/ui'
import { useNotificationSettings } from '../../features/notification/hooks/useNotificationSettings'
import { useUpdateNotificationSettings } from '../../features/notification/hooks/useUpdateNotificationSettings'
import {
  NOTIFICATION_CHANNEL_DISPLAY_META,
  NOTIFICATION_TYPE_LABEL,
} from '../../features/notification/model/fixtures'
import type { NotificationType } from '../../features/notification/model/types'

const CHANNEL_COLUMNS: NotificationChannel[] = ['IN_APP', 'EMAIL', 'SLACK']

// docs/API.md "알림 설정 조회/수정" 이식. 서버는 유형(NotificationType) × 채널 매트릭스로
// 저장하고, 상단 채널 요약(channels)은 그 매트릭스에서 파생한 값이라 여기서 따로 편집하지
// 않는다 — 실제로 켜고 끄는 지점은 매트릭스 체크박스뿐이다(D-47). EMAIL·SLACK은 MVP2라
// supported가 false로 내려와 체크박스를 잠근다.
export function SettingsNotificationsPage() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const { data, isLoading } = useNotificationSettings(workspaceId)
  const updateSettings = useUpdateNotificationSettings(workspaceId)

  // { [type]: Set<channel> } — 저장 전까지의 편집 상태. 서버 응답이 오면 한 번 채운다.
  const [draft, setDraft] = useState<Record<NotificationType, Set<NotificationChannel>> | null>(null)

  useEffect(() => {
    if (!data) return
    setDraft(
      Object.fromEntries(
        data.settings.map((setting) => [setting.type, new Set(setting.channels)]),
      ) as Record<NotificationType, Set<NotificationChannel>>,
    )
  }, [data])

  function toggle(type: NotificationType, channel: NotificationChannel) {
    setDraft((prev) => {
      if (!prev) return prev
      const next = { ...prev, [type]: new Set(prev[type]) }
      if (next[type].has(channel)) {
        next[type].delete(channel)
      } else {
        next[type].add(channel)
      }
      return next
    })
  }

  function handleSave() {
    if (!draft) return
    updateSettings.mutate({
      settings: (Object.entries(draft) as [NotificationType, Set<NotificationChannel>][]).map(
        ([type, channels]) => ({ type, channels: [...channels] }),
      ),
    })
  }

  if (isLoading || !data || !draft) {
    return <p className="text-sm text-text-tertiary">불러오는 중…</p>
  }

  return (
    <div className="flex max-w-[720px] flex-col gap-[22px]">
      <Card className="px-[22px] py-[6px]">
        {data.channels.map((summary, idx) => {
          const meta = NOTIFICATION_CHANNEL_DISPLAY_META[summary.channel]
          return (
            <div
              key={summary.channel}
              className={`flex items-center justify-between py-4 ${
                idx < data.channels.length - 1 ? 'border-b border-border-soft' : ''
              } ${summary.supported ? '' : 'opacity-60'}`}
            >
              <div>
                <div className="flex items-center gap-2 text-[13.5px] font-bold">
                  {meta.name}
                  {meta.badge && <Pill tone="neutral">{meta.badge}</Pill>}
                </div>
                {meta.desc && (
                  <p className="mt-[2px] text-[11.5px] text-text-quaternary">{meta.desc}</p>
                )}
              </div>
              <span
                className={`inline-block h-[22px] w-[38px] shrink-0 rounded-[11px] ${
                  summary.enabled ? 'bg-accent' : 'bg-border-strong'
                }`}
                title="아래 표에서 유형별로 켜고 끕니다"
              />
            </div>
          )
        })}
        <div
          className="flex items-center justify-between border-t border-border-soft py-4 opacity-60"
        >
          <div>
            <div className="flex items-center gap-2 text-[13.5px] font-bold">
              웹 push
              <Pill tone="neutral">MVP3</Pill>
            </div>
            <p className="mt-[2px] text-[11.5px] text-text-quaternary">
              브라우저를 닫아도 받습니다. 브라우저 알림 권한이 필요합니다
            </p>
          </div>
          <span className="inline-block h-[22px] w-[38px] shrink-0 cursor-not-allowed rounded-[11px] bg-border-strong" />
        </div>
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
              {CHANNEL_COLUMNS.map((channel) => {
                const supported = data.channels.find((c) => c.channel === channel)?.supported
                return (
                  <th
                    key={channel}
                    className={`border-b border-border-soft px-2 py-2 text-center text-text-quaternary ${
                      supported ? '' : 'opacity-50'
                    }`}
                  >
                    {NOTIFICATION_CHANNEL_DISPLAY_META[channel].name}
                  </th>
                )
              })}
            </tr>
          </thead>
          <tbody>
            {data.settings.map((setting) => (
              <tr key={setting.type}>
                <td className="border-b border-border-faint px-2 py-2 text-text-secondary">
                  {NOTIFICATION_TYPE_LABEL[setting.type]}
                </td>
                {CHANNEL_COLUMNS.map((channel) => {
                  const supported =
                    data.channels.find((c) => c.channel === channel)?.supported ?? false
                  const checked = draft[setting.type]?.has(channel) ?? false
                  return (
                    <td
                      key={channel}
                      className="border-b border-border-faint px-2 py-2 text-center"
                    >
                      <input
                        type="checkbox"
                        checked={checked}
                        disabled={!supported}
                        onChange={() => toggle(setting.type, channel)}
                        className={supported ? 'cursor-pointer' : 'cursor-not-allowed opacity-40'}
                      />
                    </td>
                  )
                })}
              </tr>
            ))}
          </tbody>
        </table>
        <p className="mt-[10px] text-[11.5px] leading-[1.7] text-text-tertiary">
          채널이 비어 있는 유형은 알림 자체를 만들지 않습니다. EMAIL·SLACK은 아직 발송
          어댑터가 없어(MVP2) 체크는 저장되지만 실제로 보내지지 않습니다.
        </p>
      </Card>

      <div className="flex justify-end">
        <Button variant="primary" onClick={handleSave} disabled={updateSettings.isPending}>
          {updateSettings.isPending ? '저장 중…' : '저장'}
        </Button>
      </div>
    </div>
  )
}
