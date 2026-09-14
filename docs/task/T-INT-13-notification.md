# T-INT-13 — notification 실연동

상태: **보류(이번 트랙 A 진행에서 건너뜀)** | 담당자: (미정)
근거: `docs/plan/INTEGRATION_PLAN.md` 2절 Track A · 2026-09-14 사용자 결정(아래 참고)
의존: 없음(다른 도메인 태스크와 독립). `T-INT-6`(NotificationController 인증 주체
전환)과 겹치지 않지만 같이 확인하면 좋다.

## 보류 사유(2026-09-14 조사)

- `fetchNotificationSettings.ts`가 흉내내던 "알림 설정(채널/매트릭스)" 기능은 **백엔드에
  아예 없다** — `D-54`로 MVP1에서 `NotificationChannel`·`NotificationSetting` 자체가
  완전히 제거됐다(`docs/plan/CONFLICTS.md` D-54). 즉 원래 이 태스크가 대체하려던 목업
  자체가 대응할 실 API가 없다.
- 반대로 알림 **목록 조회**(`GET /api/workspaces/{workspaceId}/notifications`)·**안읽음
  수**(`.../unread-count`)·**읽음 처리**(`PATCH .../{id}/read`, `.../read-all`)는 실제로
  존재하지만(`docs/API.md` 1565~1657행), 프론트에는 이 4개를 호출하는 api 파일이 **하나도
  없다**(목업조차 없음 — 처음부터 새로 만들어야 하는 범위라 원래 태스크 정의보다 크다).
- 사용자 결정: **이번 트랙 A 진행에서는 notification 전체를 건너뛴다.** 목록/읽음 처리를
  새로 만드는 작업은 범위가 다르므로 별도 태스크로 다시 정의해야 한다(필요해지면
  `T-INT-13a`(알림 목록·읽음 처리 신규 연동)로 쪼개서 `INTEGRATION_PLAN.md`에 추가하고
  진행).

## 체크리스트(재개 시 참고용 — 지금은 진행하지 않음)

- [ ] `api/fetchNotificationSettings.ts` — 대응 백엔드 없음. 설정 화면 자체를 없앨지
      목업으로 남길지는 백엔드가 `D-54`를 뒤집어 채널 개념을 추가할 때 재논의
- [ ] (신규 범위) 알림 목록/안읽음수/읽음처리 4개 api 파일 신설 — `docs/API.md` 1565행
      "Notification API" 절 참고. **단, 이 절 자체가 "인증 전까지 운영 배포 대상이
      아니다"(`memberId` 쿼리 파라미터 임시 방식, 1571행)라고 명시하므로 `T-INT-6`이
      끝나 인증 주체 방식으로 바뀐 뒤 진행하는 게 맞다**
- [ ] `model/types.ts` — 실제 API 응답에 맞춰 조정
- [ ] 화면 확인: 알림 패널에서 실 데이터 표시 QA (설정 화면은 대상 아님)
