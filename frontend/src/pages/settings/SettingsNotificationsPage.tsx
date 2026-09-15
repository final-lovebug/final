import { Banner, Card, DataTable, Pill, Td, Th, Toggle, Tr } from '../../shared/ui'
import { cx } from '../../shared/lib/cx'
import type { NotificationChannel } from '../../shared/types/common'
import {
  NOTIFICATION_CHANNEL_DISPLAY_META,
  NOTIFICATION_CHANNEL_SUMMARY_FIXTURES,
  NOTIFICATION_TYPE_LABEL,
  NOTIFICATION_TYPE_SETTING_FIXTURES,
} from '../../features/notification/model/fixtures'

const CHANNEL_COLUMNS: NotificationChannel[] = ['IN_APP', 'EMAIL', 'SLACK']

// ui/main.js renderSettingsNotif() 이식.
//
// **읽기 전용 화면이다(2026-09-14 정정).** 알림 "설정"에는 대응하는 백엔드가 없다 —
// `D-54`가 MVP1에서 채널 개념(`NotificationChannel`·채널 어댑터·`NotificationSetting`
// 엔티티·서비스·컨트롤러·테이블)을 전부 걷어냈다. MVP1의 전달 수단은 인앱 하나뿐이고
// 인앱은 DB에 행이 있는 것이 곧 전달이라, 고를 채널이 없어 설정 화면이 의미가 없었다.
// `REQ-NTF-009`(수신 설정)는 MVP2로 내려가 대기 상태다.
//
// 그래서 저장 버튼과 편집 가능한 체크박스를 없앴다 — 조회할 API도 저장할 API도 없는데
// 눌리는 버튼을 두면 "저장했다"는 거짓말이 된다. 대신 **지금 실제로 어떻게 동작하는지**
// (전 유형 인앱 발송, 나머지 채널 미지원)를 그대로 보여준다. 여기 쓰는 상수는 서버 응답이
// 아니라 그 고정 정책을 적어 둔 것이다.
export function SettingsNotificationsPage() {
  return (
    <div className="flex max-w-[720px] flex-col gap-[22px]">
      <Banner className="leading-[1.7]">
        지금은 <strong>인앱 알림만</strong> 동작하고, 유형별로 켜고 끄는 설정은 아직
        없습니다(MVP2). 알림은 헤더 벨에서 확인합니다 — 이 화면은 현재 동작을 보여주는
        읽기 전용입니다.
      </Banner>

      <Card className="px-[22px] py-[6px]">
        {NOTIFICATION_CHANNEL_SUMMARY_FIXTURES.map((summary, index) => {
          const meta = NOTIFICATION_CHANNEL_DISPLAY_META[summary.channel]
          return (
            <div
              key={summary.channel}
              className={cx(
                'flex items-center justify-between py-4',
                index < NOTIFICATION_CHANNEL_SUMMARY_FIXTURES.length - 1 &&
                  'border-b border-border-soft',
                !summary.supported && 'opacity-60',
              )}
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
              <Toggle on={summary.enabled} label={`${meta.name} 사용 여부(변경 불가)`} />
            </div>
          )
        })}

        <div className="flex items-center justify-between border-t border-border-soft py-4 opacity-60">
          <div>
            <div className="flex items-center gap-2 text-[13.5px] font-bold">
              웹 push
              <Pill tone="neutral">MVP3</Pill>
            </div>
            <p className="mt-[2px] text-[11.5px] text-text-quaternary">
              브라우저를 닫아도 받습니다. 브라우저 알림 권한이 필요합니다
            </p>
          </div>
          <Toggle on={false} label="웹 push(미지원)" />
        </div>
      </Card>

      <Banner tone="neutral">
        카카오톡은 지원하지 않습니다 — 알림톡 발신에 사업자등록증과 템플릿 심사가 필요합니다.
      </Banner>

      <Card className="p-5">
        <DataTable className="text-xs">
          <thead>
            <tr>
              <Th className="px-2 py-2">트리거</Th>
              {CHANNEL_COLUMNS.map((channel) => {
                const supported = NOTIFICATION_CHANNEL_SUMMARY_FIXTURES.find(
                  (summary) => summary.channel === channel,
                )?.supported
                return (
                  <Th
                    key={channel}
                    className={cx('px-2 py-2 text-center', !supported && 'opacity-50')}
                  >
                    {NOTIFICATION_CHANNEL_DISPLAY_META[channel].name}
                  </Th>
                )
              })}
            </tr>
          </thead>
          <tbody>
            {NOTIFICATION_TYPE_SETTING_FIXTURES.map((setting) => (
              <Tr key={setting.type}>
                <Td className="px-2 py-2">{NOTIFICATION_TYPE_LABEL[setting.type]}</Td>
                {CHANNEL_COLUMNS.map((channel) => {
                  const on = setting.channels.includes(channel)
                  return (
                    <Td
                      key={channel}
                      className={cx(
                        'px-2 py-2 text-center',
                        on ? 'font-bold text-success' : 'opacity-30',
                      )}
                    >
                      {on ? '✓' : '–'}
                    </Td>
                  )
                })}
              </Tr>
            ))}
          </tbody>
        </DataTable>
        <p className="mt-[10px] text-[11.5px] leading-[1.7] text-text-tertiary">
          재검사 등급 리비전에서, 그 용어를 실제로 쓰는 문서의 작성자에게만 보냅니다. 용어
          추가만 있는 리비전은 보내지 않습니다.
        </p>
      </Card>
    </div>
  )
}
