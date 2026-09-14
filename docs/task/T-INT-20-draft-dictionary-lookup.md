# T-INT-20 — 워크스페이스의 진행 중 사전 초안 조회(백엔드)

상태: **완료(2026-09-14)** | 담당자: (세션 진행)
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

## 설계 확정(2026-09-14, 사용자 결정)

**방향 1 — `GET /api/draft-dictionaries?workspaceId={id}&status=` 목록 엔드포인트
신설**로 확정. `DraftDocument API`의 `GET /api/draft-documents?documentId=&status=
&page=&size=&sort=`(참여 중인 워크스페이스로 암묵 필터링)와 같은 패턴이라
일관성이 있다. 응답은 `PageResponse<DraftDictionaryResponse>`(기존
`GET /api/draft-dictionaries/{id}` 단건 응답과 같은 shape의 배열)로, `status`
쿼리 파라미터로 `EXAMINING` 등 진행 상태 필터링을 지원한다.

`docs/plan/CONFLICTS.md`에 `D-64`로 등재 완료.

## 체크리스트

- [x] `GET /api/draft-dictionaries?workspaceId=&status=&page=&size=` 구현 —
      `DraftDictionarySearchQuery`(검증) + `DraftDictionaryRepository`(배치 조회
      2종) + `DraftDictionaryReader.search` + `DraftDictionaryService.search`
      (참여자 검증 포함) + `DraftDictionaryController.search`. `DraftDocument`의
      목록 조회 구현과 동일 패턴 — 단 `DraftDictionary`는 엔티티에 `workspaceId`가
      직접 있어 `DocumentQueryPort` 같은 크로스 도메인 접근 id 조회가 필요 없었다
- [x] 테스트 — `DraftDictionaryControllerTest.search`(컨트롤러 슬라이스),
      `DraftDictionaryServiceTest.search`/`search_filterByStatus`/
      `search_notParticipant`(통합)
- [x] `docs/API.md` "DraftDictionary API" 절에 "워크스페이스 기준 목록 조회" 추가
- [x] `docs/plan/CONFLICTS.md`에 `D-64`로 등재
- [ ] `docs/task/T-INT-11-dictionary.md`의 후보어 3개 항목 의존 해제(다음에 그
      태스크 진행 시 처리)
- [x] `./gradlew spotlessApply`, `./gradlew test --tests
      "com.ubidict.backend.draftdictionary.*"` 통과
- [ ] 커밋 브랜치 `feat/WLSH-{티켓}-t-int-20`, PR — **사용자 지시 시 진행**
