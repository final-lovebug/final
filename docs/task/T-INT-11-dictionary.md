# T-INT-11 — dictionary 실연동

상태: 진행중(후보어 재개 — 아래 「2026-09-14 후보어 재개(WLSH-171)」 절 참고) | 담당자: (WLSH-171 세션)
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
      선행돼야 한다 — `docs/task/T-INT-20-draft-dictionary-lookup.md` 참고. **여전히
      프론트 연동 자체는 보류지만, 후보어를 "여러 표기 묶음"으로 보여줄 수 있는지 질문이
      나와 백엔드 쪽 유실 지점을 확인·보강했다(`D-65`, Direction A) — `CandidateTerm`/
      `ExtractedTerm`에 `variantForms: List<String>`을 추가해 ubidict-py
      `GroupCandidate.forms` 그룹핑 정보가 끝까지 보존되도록 스키마·API를 먼저 넓혀
      뒀다. `CandidateTermResponse.variantForms`로 이미 노출된다 — 이 태스크가 실제로
      풀릴 때 프론트가 그룹으로 묶어 보여주는 화면을 만들면 된다(데이터는 이미 있음)**
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

---

## 2026-09-14 후보어 재개 (WLSH-171)

위 「보류」 항목을 **해제한다.** 보류 근거였던 "이 워크스페이스의 현재 사전 초안을
찾는 목록 엔드포인트가 없다"는 `T-INT-20` 완료로 해소됐다 —
`GET /api/draft-dictionaries?workspaceId={id}&status={status}`가 실재하는 것을
소스에서 확인했다(`DraftDictionaryController` 34행).

### 사전 확인에서 드러난 목업↔실 API 간극

`DictionaryDraftPage.tsx`가 쓰는 필드 중 백엔드 `CandidateTermResponse`에 대응이
없는 것들이 있다. 화면이 실제로 렌더링하는 값이라 단순 매핑으로 넘어가지 않는다.

| 프론트 목업 | 백엔드 | 처리 |
|---|---|---|
| `words[]` | `variantForms[]` | 매핑(`D-65`로 이미 보존됨) |
| `form`·`occurrenceCount`·`proposedDefinition`·`status` | 동일 | 매핑 |
| `selectedWord` | `PATCH /candidate-terms/{id}`의 `form` | 매핑 |
| `id`·`draftDictionaryId` (string) | `Long` | 문자열 변환 |
| `occurredDocumentIds` (string[]) | `List<Long>` | 문자열 변환 |
| **`type`** | 없음 | ↓ 결정 1 |
| **`ownerName`** | `createdBy`는 엔티티에만, 응답 DTO에 없음 | ↓ 결정 2 |
| **`quotes[].source`·`.split`·`splitNote`** | `contextSnippets: List<String>` | ↓ 결정 3 |

### 결정 (2026-09-14 사용자 결정)

**결정 1 — `type`은 백엔드에 필드를 추가한다.**
`동의어`/`동형이의`/`표기 변형` 분류는 `variantForms` 길이로 유도할 수 없다
(`동형이의`가 판별 불가 — `HOMOGRAPH`는 추출 파이프라인이 건너뛴다). 화면에서
빼는 대신 `CandidateTerm`에 필드를 신설한다. **이 결정으로 이 브랜치에 백엔드
커밋이 하나 섞인다**(엔티티 + Flyway `V550` + DTO + 서비스 + 테스트).
추출로 생성된 후보어는 값을 채울 근거가 없으므로 **nullable**로 두고 화면은
미분류로 표시한다.

**결정 2 — `createdBy`·`handledBy`를 둘 다 응답에 노출한다.**
`createdBy`는 **이미 엔티티에 있고 응답 DTO에만 빠져 있었다**(마이그레이션 불필요).
`ownerName`의 원래 의미가 등록자이므로 화면은 `createdBy`를 쓰고, 처리자
(`handledBy`)는 별도로 표시 가능하게 함께 노출한다. 이름 해석은 `T-INT-18`의
`GET /api/members?ids=`(배치)로 한다.

**결정 3 — 인용문은 스니펫 텍스트만 표시한다.**
`contextSnippets`에 출처 문서·문단 정보와 분리 플래그가 없다. 상세 패널에서
출처 줄·`split` 배지·`splitNote`를 **제거**한다. 백엔드 스키마를 넓히는 것은
추출 파이프라인(`ubidict-py`)까지 걸려 이번 범위 밖이다.

**결정 4 — 상태 전이 5개를 이번 범위에 포함한다.**
화면의 "승인"·"보류" 버튼이 미연결 상태다. 백엔드에 이미 있는 엔드포인트를
프론트 api 파일로 붙인다 — `registration-approval`·`hold`·`rejection`·
`synonym-merge`·`bulk-decision`.

### 체크리스트 — 후보어 재개분

**백엔드(결정 1·2)**
- [x] `CandidateTermType` enum 신설(`SYNONYM`·`HOMOGRAPH`·`VARIANT`), `CandidateTerm`에
      nullable 필드 추가
- [x] Flyway `V550__add_candidate_term_type.sql` (draftdictionary 대역 500–599)
- [x] `CandidateTermResponse`·`CandidateTermResult`에 `type`·`createdBy` 추가,
      `AddCandidateTermRequest`·`EditCandidateTermRequest`에 `type` 추가
- [x] `AddCandidateTermCommand`/`EditCandidateTermCommand`에 `type` 추가(기존 호출부는
      위임 생성자로 그대로 컴파일된다), `CandidateTermWriter`가 전달
- [x] 테스트 — `add_returnsTypeAndCreatedBy`·`add_typeIsNullWhenNotChosen`·
      `edit_changesTypeOnlyWhenGiven` (`CandidateTermServiceTest`)
- [x] `docs/API.md` 후보어 절의 요청·응답 예시와 설명 갱신
- [x] `./gradlew spotlessApply && ./gradlew check` 통과(2026-09-14, `BUILD SUCCESSFUL`)

**프론트**
- [x] `api/fetchCandidates.ts` — 2단계(초안 조회 → 후보어 조회) + 등록자 이름 배치 조회.
      공통 응답 타입·매핑은 `api/candidateTermApi.ts`로 분리
- [x] `api/createCandidateTerm.ts` — `POST /api/draft-dictionaries/{id}/candidate-terms`.
      입력이 `workspaceId`를 받도록 바뀜(초안 id를 스스로 찾는다), `ownerId`는 제거
- [x] `api/updateCandidateTerm.ts` — `PATCH /api/candidate-terms/{id}`. 표준어 선택은
      별도 필드가 아니라 `form` 교체다
- [x] 상태 전이 api 신설(결정 4) — `api/decideCandidateTerm.ts`(승인·보류·거절·동의어 편입) +
      `api/bulkDecideCandidateTerms.ts`. 훅 `useDecideCandidateTerm`·
      `useBulkDecideCandidateTerms` 추가. **일괄 판정은 부분 실패가 정상 응답이다**
      (`succeeded`/`failed`로 갈려 200) — 목록 UI에 붙일 때 failed 처리 필요
- [x] `model/types.ts` — `CandidateTermStatus`를 백엔드와 1:1로 교정
      (`APPROVED`→`REGISTRATION_APPROVED`, `KEPT` 추가), `CandidateTermOrigin`·
      `CandidateTermType` 신설, `CandidateTerm`에 origin/type/variantForms/handledBy 등 추가
- [x] `model/fixtures.ts` — `CandidateTermListItem`에서 `quotes`·`splitNote` 제거
      (`contextSnippets`를 상속해 쓴다), `type`을 enum으로 교체. 참조가 사라진
      `CANDIDATE_TERM_FIXTURES`·`DRAFT_DICTIONARY_ID`·`DRAFT_DICTIONARY_FIXTURE`·
      `CandidateQuote` 제거
- [x] `DictionaryDraftPage.tsx` — 근거 문장에서 출처·split·splitNote 제거(결정 3),
      분류는 한글 라벨 맵으로 표시하고 값이 없으면 "미분류", "작성 완료로 표시"·"보류"
      버튼을 등재 승인/보류 판정에 연결(승인은 정의가 비어 있으면 비활성)
- [x] `npx tsc -b` 통과, `npx oxlint src` 통과(경고 4건은 전부 기존 것)
