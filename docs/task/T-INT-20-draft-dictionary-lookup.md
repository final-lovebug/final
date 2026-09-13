# T-INT-20 — 워크스페이스의 진행 중 사전 초안 조회(백엔드)

상태: 대기 | 담당자: (미정)
근거: `T-INT-11` 진행 중 발견(2026-09-14) — `docs/task/T-INT-11-dictionary.md` 참고
의존: 없음. **`T-INT-11`의 후보어 관련 3개 항목(fetchCandidates/createCandidateTerm/
updateCandidateTerm)의 선행**

## 문제

`DraftDictionary API`(`docs/API.md` 1336행)는 `GET /api/draft-dictionaries/
{draftDictionaryId}`처럼 **id로만** 조회할 수 있고, "이 워크스페이스의 현재 진행 중인
사전 초안"을 찾는 목록/조회 엔드포인트가 없다(`GET /api/draft-dictionaries?workspaceId=`
같은 것이 없음). 프론트의 사전 초안 교정 화면(`DictionaryDraftPage`,
`fetchCandidates(workspaceId)` 등)은 workspaceId만 갖고 시작하므로 이 lookup이 없으면
후보어 목록을 볼 방법이 없다.

`T-INT-14`(참여자 이름/이메일)와 같은 성격의 gap이다 — 백엔드가 "id로 하나 조회"만
지원하고 "워크스페이스 기준으로 찾기"를 지원하지 않는 패턴이 반복되고 있다.

## 설계는 여기서 확정하지 않는다

두 방향이 있다(임의로 확정하지 않는다 — 루트 `CLAUDE.md`).

1. **`GET /api/draft-dictionaries?workspaceId={id}&status=`** 목록 엔드포인트 신설.
   `DraftDocument API`의 `GET /api/draft-documents?documentId=&status=&page=&size=&sort=`
   (참여 중인 워크스페이스로 암묵 필터링)와 같은 패턴 — 이미 선례가 있다.
2. `GET /api/workspaces/{workspaceId}` 같은 기존 워크스페이스 조회 응답에
   `activeDraftDictionaryId`(있으면)를 얹는 방식. 크로스 도메인 조회가 늘어나는
   대가가 있다.

1번이 `DraftDocument`와 패턴이 같아 일관성 있어 보이지만, 실제 착수 시
`docs/plan/CONFLICTS.md`에 필요하면 새 결정 ID(`D-63`~)로 남기고 진행한다.

## 체크리스트(설계 확정 후 채움 — 지금은 시작하지 않음)

- [ ] 위 두 방향 중 하나 선택(필요하면 사용자에게 확인)
- [ ] 엔드포인트 구현 + 테스트
- [ ] `docs/API.md` "DraftDictionary API" 절에 반영
- [ ] `docs/task/T-INT-11-dictionary.md`의 후보어 3개 항목 의존 해제
- [ ] `./gradlew spotlessApply && ./gradlew check` 통과
- [ ] 커밋 브랜치 `feat/WLSH-{티켓}-t-int-20`, PR 생성
