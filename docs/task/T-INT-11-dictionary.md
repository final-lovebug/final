# T-INT-11 — dictionary 실연동

상태: 진행중(범위 축소, 2026-09-14) | 담당자: (세션 진행)
근거: `docs/plan/INTEGRATION_PLAN.md` 2절 Track A
의존: 없음(다른 도메인 태스크와 독립). 후보어 3개 항목만 `T-INT-20` 의존

`frontend/src/features/dictionary/**`가 전부 목업이다.

## 체크리스트 — api 파일별

- [x] `api/fetchDictionary.ts` (사전집 조회) — 완료. `definition`/`updatedAt` 없는 용어는
      화면(`DictionaryPage.tsx`)에서 "—"로 표시하도록 함께 수정. `Dictionary.id`/`name`은
      `workspaceId` 기반으로 합성(실 API에 없음)
- [x] ~~`api/fetchCandidates.ts`~~ / ~~`api/createCandidateTerm.ts`~~ /
      ~~`api/updateCandidateTerm.ts`~~ — **보류(2026-09-14 사용자 결정).** 후보어는
      `draftDictionaryId`가 있어야 조회 가능한데, "이 워크스페이스의 현재 사전 초안"을
      찾는 목록 엔드포인트가 백엔드에 없다(id로만 조회 가능). `T-INT-20`(백엔드 보강)이
      선행돼야 한다 — `docs/task/T-INT-20-draft-dictionary-lookup.md` 참고
- [x] ~~`api/fetchDictionaryRevisionTimeline.ts`~~ — **보류(2026-09-14 사용자 결정).**
      백엔드 `revisionlog` 도메인은 `domain`/`infra`(엔티티·리포지토리)만 있고
      서비스·컨트롤러·조회 API가 전혀 없다(`backend/src/main/java/com/ubidict/backend/
      revisionlog/` 확인 완료 — phase-1은 모델만, phase-2 조회 API 미착수).
      목업 그대로 두고 이번 태스크에서 건너뛴다. 백엔드에 조회 API가 생기면 별도
      태스크(`T-INT-11a`)로 재정의
- [x] `api/fetchExtractionEligibleDocuments.ts` — 완료. `GET /api/workspaces/{id}/documents`
      를 직접 호출해 `aligned` 필드로 클라이언트에서 필터링(size=100 상한, 후속 과제로
      메모)

## 나머지

- [x] `model/types.ts` — `Dictionary`/`Term`의 실 API 미제공 필드를 옵셔널로 조정,
      `Dictionary.publishedAt`/`publishedBy` 추가
- [x] `model/fixtures.ts`·`model/versionFixtures.ts` — 그대로 둠(후보어 3개가 아직
      이걸 참조 중)
- [x] `npx tsc -b` 통과(DictionaryPage.tsx의 definition/updatedAt 렌더링도 "—" fallback으로
      함께 수정)
- [ ] 화면 확인: 사전집 조회에서 실 데이터 표시 QA — **로컬 백엔드 기동 후 사용자가
      직접 확인 필요**(이 세션은 백엔드를 띄우지 않음). 후보어 교정·개정 이력은 보류라
      QA 대상 아님
- [ ] 커밋 브랜치 `feat/WLSH-{티켓}-fe-dictionary-real-api`, PR 생성 — **사용자 지시 시
      진행**
