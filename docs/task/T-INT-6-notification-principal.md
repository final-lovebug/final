# T-INT-6 — NotificationController 인증 주체 전환

상태: 대기 | 담당자: (미정)
근거: `docs/plan/INTEGRATION_PLAN.md` 0절("T-INT-3 거의 완료" 표) · `docs/plan/EXECUTION_ORDER.md`(T-INT-3, `memberId` 파라미터 제거)
의존: 없음(다른 태스크와 무관하게 언제든 진행 가능)

`T-INT-3`이 컨트롤러 21/24개를 `@AuthenticationPrincipal`로 전환했지만
`NotificationController`(4개 엔드포인트)만 아직 `@RequestParam Long memberId`를 쓴다.
같은 패턴으로 마무리하는 잡일이다.

## 체크리스트

- [ ] `backend/src/main/java/com/ubidict/backend/notification/presentation/NotificationController.java`
      확인 — 대상 엔드포인트 4개(`GET` 목록, `GET /unread-count`, `PATCH /{id}/read`,
      `PATCH /read-all`) 특정
- [ ] 4개 모두 `@RequestParam Long memberId` → `@AuthenticationPrincipal Long memberId`로 교체
- [ ] 다른 도메인 컨트롤러(예: `WorkspaceController`)의 기존 패턴과 동일한지 대조
- [ ] `NotificationControllerTest`가 있다면 `@RequestParam` 기반 요청을 principal 주입
      방식으로 수정(다른 컨트롤러 테스트의 `@WithLoginMember` 패턴 재사용)
- [ ] `docs/API.md`의 Notification 절에 `memberId` 쿼리 파라미터가 언급돼 있으면 함께 정정
- [ ] `./gradlew spotlessApply && ./gradlew check` 통과
- [ ] 커밋 브랜치 `chore/WLSH-{티켓}-t-int-6`, PR 생성
