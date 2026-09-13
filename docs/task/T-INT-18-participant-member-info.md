# T-INT-18 — 회원 이름/이메일 조회 수단 부재 (백엔드, 범위 확장 2026-09-14)

상태: 대기 | 담당자: (미정)
근거: `T-INT-14` 진행 중 발견(2026-09-14) — `docs/task/T-INT-14-member.md` "보류 사유" 참고.
      `T-INT-10` 진행 중 같은 gap을 한 번 더 확인(아래 "범위 확장" 참고)
의존: 없음. **`T-INT-14`(프론트, 전체 보류)와 `T-INT-10`(프론트, 일부 컬럼만 영향)의 선행**

## 문제 — 이건 한 엔드포인트만의 문제가 아니다

`Member API`가 `/api/members/me`(본인 전용)뿐이라 **다른 회원을 `memberId`로 조회할
방법이 어디에도 없다.** 아래처럼 여러 도메인이 "행위자는 id만 준다"는 같은 패턴을
공유한다.

- `GET /api/workspaces/{workspaceId}/participants`(`ParticipantResponse`) —
  `{participantId, workspaceId, memberId, permission, joinedAt}`뿐, 이름·이메일 없음
- `GET /api/workspaces/{workspaceId}/documents`(`DocumentListItem`류) — `uploaderId`만
  있고 이름 없음(문서 목록·상세 화면의 "작성자"·"최종 수정자" 열)
- **(2026-09-14 확인 완료)** 같은 패턴이 3개 도메인에 더 있다:
  - `dictionary` — `GET /api/workspaces/{id}/dictionary` 응답의 `publishedBy`(`docs/API.md:862`)
  - `draftdictionary` — `CandidateTermResponse.java:19,38`의 `handledBy`
  - `reviewrequest` — `ReviewRequestResponse.java:14,28`의 `requesterId`,
    `ReviewerResponse`의 `memberId`

  → 총 5개 도메인(workspace·document·dictionary·draftdictionary·reviewrequest)이
  전부 "행위자는 id만 준다"는 같은 문제를 공유한다. 도메인마다 각자 조인을 만드는
  것보다 **공용 조회 하나**가 맞다는 판단에 힘을 싣는 근거다.

## 범위 확장(2026-09-14, `T-INT-10` 진행 중)

원래 참여자 목록(`T-INT-14`) 하나만 막는 문제인 줄 알았는데, 문서 목록/상세의
"작성자"·"최종 수정자" 표시(`ownerName`/`updaterName`)도 똑같이 막혀 있었다. **다만
문서 쪽은 통째로 보류하지 않고 이름 칸만 "—"로 비워둔 채 나머지(제목·라벨·버전 등)는
실연동했다** — `T-INT-14`(전체가 이름 기능인 태스크)와 달리 `T-INT-10`은 이름이 여러
컬럼 중 하나일 뿐이라 판단을 다르게 했다(`docs/task/T-INT-10-document.md` 참고).

## 설계 확정 (2026-09-14, 사용자 결정)

**회원 배치 조회 + 단건 조회 API를 `member` 도메인에 새로 만드는 것으로 확정했다.**
도메인별 조인(각 도메인이 `member`를 개별 크로스 도메인 조회하는 방식)은 채택하지
않는다 — 5개 도메인이 같은 문제를 공유하는 상황에서 공용 조회 하나가 반복 구현을
막는다.

### 엔드포인트

| Method | Path | 용도 |
| --- | --- | --- |
| GET | `/api/members/{memberId}` | 단건 조회 |
| GET | `/api/members?ids=1,2,3` | 배치 조회(콤마 구분 id 목록) |

응답 형식(제안, 착수 시 확정):
```json
{ "memberId": 7, "displayName": "홍길동", "email": "member@example.com" }
```
배치는 위 항목의 배열. **`status`·`role`은 넣지 않는다** — 다른 회원의 사이트 권한·
계정 상태까지 공개할 이유가 없다(참여자 목록·작성자 표시엔 이름/이메일이면 충분).

### 아직 착수 시 정해야 할 것 (임의로 확정하지 않는다 — 루트 `CLAUDE.md`)

- **접근 범위**: 아무 로그인 사용자나 임의의 `memberId`를 조회할 수 있게 할지,
  아니면 "같은 워크스페이스에 참여 중인 회원만" 조회 가능하게 제한할지. 후자가
  더 안전하지만 구현 시 워크스페이스 컨텍스트를 함께 검증해야 한다(어느 워크스페이스
  기준인지도 파라미터로 받아야 할 수 있음).
- `email`/`displayName`은 `MemberFieldEncryptor`로 암호화 저장돼 있어 복호화가
  필요하다 — 배치 조회 시 N건 복호화 비용 확인(참여자 목록 기준 최대 5명이라 크지
  않을 것으로 예상하지만, 문서 목록처럼 더 많은 회원이 섞일 수 있는 곳도 있다).
- 탈퇴 회원(`WITHDRAWN`, 익명화된 email/displayName)을 조회하면 어떻게 보여줄지
  (예: "탈퇴한 회원"으로 표시) — `Member.withdraw()`가 이미 이런 값으로 바꿔 두므로
  자연스럽게 처리될 가능성이 높지만 확인 필요.
- `docs/plan/CONFLICTS.md`에 새 결정 ID(`D-63`~, 착수 시점 최신 번호 확인)로 이
  결정을 등재한다(루트 `CLAUDE.md` "결정이 바뀌면 코드보다 문서를 먼저 갱신").

## 체크리스트

- [ ] 위 "아직 착수 시 정해야 할 것" 확인(필요하면 사용자에게 질문)
- [ ] `GET /api/members/{memberId}` 구현 + 테스트
- [ ] `GET /api/members?ids=` 구현 + 테스트
- [ ] `docs/API.md` "Member API" 절에 두 엔드포인트 추가
- [ ] `docs/plan/CONFLICTS.md`에 `D-6x`로 결정 등재
- [ ] `docs/task/T-INT-14-member.md`·`T-INT-10-document.md`의 "T-INT-18 완료 후" 절
      참고해 해제 작업 진행(아래 "완료 후 다른 태스크 진행 방법" 참고)
- [ ] `./gradlew spotlessApply && ./gradlew check` 통과
- [ ] 커밋 브랜치 `feat/WLSH-{티켓}-t-int-18`, PR 생성

## 완료 후 다른 태스크는 어떻게 진행되는가

| 태스크 | 지금 상태 | T-INT-18 완료 후 |
| --- | --- | --- |
| **T-INT-14**(member 잔여 실연동) | 전체 보류 | 재개 가능. `GET .../participants`로 참여자 목록(권한·참여일)을 받고, 그 `memberId` 배열을 `GET /api/members?ids=`에 넘겨 이름/이메일을 join — 두 호출을 합쳐 `fetchWorkspaceMembers.ts` 완성 |
| **T-INT-10**(document 실연동) | 5/10 완료, 이름 칸만 "—" | **막혀 있던 게 아니라 보강 작업**이다. `fetchDocuments.ts`/`fetchDocument.ts`가 이미 실연동돼 있으니, `uploaderId`를 단건/배치 조회로 바꿔 "—"였던 `ownerName`/`updaterName`만 채우면 끝(파일 구조는 이미 그대로 재사용 가능) |
| **T-INT-11**(dictionary) | 범위 축소 완료, 후보어만 별도 보류(`T-INT-20`) | 이 태스크 자체를 막고 있진 않았다. `publishedBy` 이름을 화면에 보여줄 필요가 생기면 그때 `fetchDictionary.ts`에 같은 방식으로 보강(현재 화면은 `publishedBy`를 안 씀 — 급하지 않음) |
| **T-INT-12**(review) | 전체 보류(구조적 재설계 필요) | **T-INT-18만으론 안 풀린다.** `requesterId`/리뷰어 이름 표시는 도움이 되지만, review가 보류된 진짜 이유(N+1, diff API 없음, 코멘트-검토 결합)는 별개 문제라 그 재설계 세션에서 별도로 다뤄야 한다 |
| **T-INT-9·T-INT-15·T-INT-16** | 완료 | 영향 없음(이름 표시가 필요한 화면이 없었음) |

즉 **T-INT-18이 실질적으로 다시 움직이게 하는 건 T-INT-14 하나**이고, T-INT-10은
이미 끝난 작업의 보강, T-INT-11·T-INT-12는 각자 다른 이유로 별도로 풀어야 한다.
