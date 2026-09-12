import { delay } from '../../../shared/lib/delay'
import {
  NOTIFICATION_CHANNEL_FIXTURES,
  NOTIFICATION_MATRIX_FIXTURES,
  type NotificationChannelSetting,
  type NotificationMatrixRow,
} from '../model/fixtures'
import type { WorkspaceId } from '../../../shared/types/ids'

export interface NotificationSettings {
  channels: NotificationChannelSetting[]
  matrix: NotificationMatrixRow[]
}

export async function fetchNotificationSettings(
  _workspaceId: WorkspaceId,
): Promise<NotificationSettings> {
  await delay()
  return { channels: NOTIFICATION_CHANNEL_FIXTURES, matrix: NOTIFICATION_MATRIX_FIXTURES }
}
