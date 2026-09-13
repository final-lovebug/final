import { delay } from '../../../shared/lib/delay'
import type { NotificationChannel } from '../../../shared/types/common'
import type { WorkspaceId } from '../../../shared/types/ids'
import { NOTIFICATION_SETTINGS_FIXTURE } from '../model/fixtures'
import type { NotificationSettings, NotificationType } from '../model/types'

export interface UpdateNotificationSettingsInput {
  settings: { type: NotificationType; channels: NotificationChannel[] }[]
}

// docs/API.md "알림 설정 수정" 목업 구현체(ADMIN 이상, 이 화면 단에서는 아직 권한 분기를
// 걸지 않는다 — 인증 연동 체크리스트 6번). 보낸 유형만 교체하고 나머지는 그대로 둔다.
// channels가 빈 배열이면 그 유형은 알림을 만들지 않는다는 뜻이라 유효한 입력이다.
export async function updateNotificationSettings(
  _workspaceId: WorkspaceId,
  input: UpdateNotificationSettingsInput,
): Promise<NotificationSettings> {
  await delay(200)

  const byType = new Map(input.settings.map((entry) => [entry.type, entry.channels]))
  NOTIFICATION_SETTINGS_FIXTURE.settings = NOTIFICATION_SETTINGS_FIXTURE.settings.map((setting) =>
    byType.has(setting.type) ? { type: setting.type, channels: byType.get(setting.type)! } : setting,
  )

  // channels 요약은 settings에서 파생한다 — 유형 하나라도 그 채널을 쓰면 enabled다.
  NOTIFICATION_SETTINGS_FIXTURE.channels = NOTIFICATION_SETTINGS_FIXTURE.channels.map((channel) => ({
    ...channel,
    enabled: NOTIFICATION_SETTINGS_FIXTURE.settings.some((setting) =>
      setting.channels.includes(channel.channel),
    ),
  }))

  return NOTIFICATION_SETTINGS_FIXTURE
}
