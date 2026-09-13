import { delay } from '../../../shared/lib/delay'
import type { WorkspaceId } from '../../../shared/types/ids'
import { NOTIFICATION_SETTINGS_FIXTURE } from '../model/fixtures'
import type { NotificationSettings } from '../model/types'

// docs/API.md "알림 설정 조회" 목업 구현체. 설정이 없는 워크스페이스는 이 호출이 기본값을
// 만든다는 규칙(5개 유형 × ["IN_APP"])이 있지만, 목업은 워크스페이스 구분 없이 고정 픽스처
// 하나를 공유한다 — 실 연동 시 워크스페이스별 최초 조회 생성 로직은 서버가 갖는다.
export async function fetchNotificationSettings(
  _workspaceId: WorkspaceId,
): Promise<NotificationSettings> {
  await delay()
  return NOTIFICATION_SETTINGS_FIXTURE
}
