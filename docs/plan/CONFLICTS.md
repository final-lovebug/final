# 결정·충돌 인벤토리

6개 도메인(`workspace`·`document`·`dictionary`·`draftdocument`·`draftdictionary`·`reviewrequest`) 계획 문서가 **공유하는 결정 원장**이다. 도메인 하나에 갇히지 않는 결정과 충돌을 여기 모으고, 각 계획 문서의 2절은 **ID로만 참조**한다.

같은 항목을 두 문서가 서로 다르게 적는 것을 막는 것이 이 문서의 목적이다. 새 결정이 필요하면 여기에 먼저 적고 그 뒤에 계획 문서를 고친다.

**읽는 순서** — 2절(큰 흐름) → 3절(이번 세션 확정) → 4절(뒤집힌 결정)까지 읽으면 현재 설계의 기준이 잡힌다. 6·7절은 구현 태스크로 넘어가는 목록이다.

---

## 1. ID 체계

| 접두사 | 뜻 | 범위 | 비고 |
| --- | --- | --- | --- |
| `D-1`~`D-18` | 초안·리뷰 3개 문서가 2026-09-10에 확정한 결정 | 전역 공유 | `DRAFT_DOCUMENT_PLAN.md`·`DRAFT_DICTIONARY_PLAN.md`·`REVIEW_REQUEST_PLAN.md` 2-1절 |
| `G-1`~`G-15` | **사용자가 확정한 큰 흐름** | 전역 | 이 문서 2절 |
| `D-19`~`D-32` | 큰 흐름을 받아 확정한 나머지 결정 | 전역 | 이 문서 3절 |
| `D-33`~`D-37` | **잔여 태스크 병렬화 세션(2026-09-12)이 확정한 결정** | 전역 | 이 문서 3-1절 |
| `R-1`~`R-24` | **큰 흐름이 뒤집은 기존 결정** | 전역 | 이 문서 4절. 문서 수정이 구현보다 앞선다 |
| `F-1`~`F-6` | 뒤집힘이 만든 새 과제 | 전역 | 이 문서 5절 |
| `X-*` | 문서 ↔ 문서 충돌 | 전역 | 이 문서 6절 |
| `Y-*` | 문서 ↔ 코드 충돌 | 전역 | 이 문서 7절 |
| `O-*` | 기존 계획 문서가 남긴 열린 질문 | 전역 | 이 문서 8절 |

**태스크 ID 접두사** — `WS-*`(Workspace) `DOC-*`(Document) `DIC-*`(Dictionary) `DD-*`(DraftDocument) `DI-*`(DraftDictionary) `RR-*`(ReviewRequest) `T-*`(통합·공통).

> **`DI-`와 `DIC-`를 혼동하지 않는다.** `DI-`는 사전 **초안**(DraftDictionary), `DIC-`는 **사전집**(Dictionary)이다.

**새 결정 ID는 `D-38`부터** 붙인다. `D-1`~`D-18`을 재사용하지 않는다.

### `D-1`~`D-18` 색인 — 이 문서 밖에 정의된 결정

초안·리뷰 3개 문서의 2-1절이 정의한 것이다. **아래는 이 문서와 신규 3개 계획 문서가 실제로 인용하는 것만** 추린 색인이다.

| ID | 요지 | 정의 위치 | 상태 |
| --- | --- | --- | --- |
| `D-1` | `반대`(OPPOSED) verdict와 `반려` 상태를 두지 않는다. 집계는 **회원별 최신 판정 1건**만 보고, 재교정으로 회차가 올라가도 이전 승인은 유지한다(GitHub PR 모델) | `REVIEW_REQUEST_PLAN.md` 2-1 | 유효 |
| `D-10` | 「진행 중」은 `status != REVISED` AND `deleted_at is null`이다. **DB 유니크로 걸지 않고 애플리케이션에서 검증한다** — 소프트 삭제를 쓰는데 MySQL 8.4에 부분 유니크 인덱스가 없다 | `DRAFT_DOCUMENT_PLAN.md`·`DRAFT_DICTIONARY_PLAN.md` 2-1 | 유효. **`D-22`가 이 정의를 그대로 쓴다** |
| `D-12`·`D-13` | 정족수는 **실시간 조회**다. `ReviewRequest`에 스냅샷 필드를 두지 않고 발행 시점의 `Workspace.ruleSet` 값으로 계산한다 | `REVIEW_REQUEST_PLAN.md` 2-1 | 유효. **`G-3`·`G-4`가 유지를 확정했다** |
| `D-15` | `DocumentQueryPort.isOutdated` 시그니처만 두고 스텁은 `false`를 반환한다. 추출이 MVP1 밖이라 정책이 유예된다 | `DRAFT_DICTIONARY_PLAN.md` 2-1 | **뒤집힘 — `R-9`** |
| `D-18` | 도메인 레벨 패키지 구조만 지키고 하위 디렉터리는 `presentation/dto/`·`service/model/`, ErrorCode는 `{domain}/exception/` | 3개 문서 2-1 | 유효 |

나머지(`D-2`~`D-9`·`D-11`·`D-14`·`D-16`·`D-17`)는 해당 도메인 안에서만 쓰이므로 각 문서의 2-1절에서 본다.

---

## 2. 큰 흐름이 확정한 것 (`G-1`~`G-15`)

### 2-1. 사전 흐름

```
사전집이 없는 경우
  ① 문서 선택 → 용어 추출 → 사전 초안 생성        (ADMIN 이상)
  ② 사전 초안 교정                                (ADMIN 이상)
  ③ 리뷰 요청 + 개정안 생성
  ④ 리뷰어 승인 → ADMIN이 반영 → 새 사전집 발행

사전집이 있는 경우
  ①' 문서 선택 → 용어 추출 + 이전 버전 사전집과 통합 → 새 사전 초안
  ② 이후 동일
```

| ID | 확정 내용 | 상태 |
| --- | --- | --- |
| **G-1** | **사전 초안은 「이전 사전집 + 추출 용어」의 통합 결과다.** 신규 후보어만 모은 집합이 아니라 **차기 버전의 전체 용어 목록**을 든다. 사전집이 없는 첫 회차는 추출 결과만으로 구성된다 | 확정 |
| **G-2** | 사전 초안 **생성·교정 주체는 ADMIN 이상**(OWNER 포함) | 확정 |
| **G-3** | 발행은 **ADMIN 수동**이다. 정족수를 채우면 `APPROVED`가 되고 ADMIN 이상이 별도로 반영한다 — 기존 `D-12`·`D-13`과 `NFR-UPD-001`(Human-in-the-Loop) 유지 | 확정 |
| **G-4** | 승인 집계는 **룰셋 정족수** 기준이다. 리뷰어 지정은 알림·필터 용도이고 지정되지 않은 참여자도 정족수에 산입된다 — 기존 `Reviewer` 결정 유지 | 확정 |

**G-1이 가장 큰 변경이다.** 현재 `DictionaryService.revise(ReviseDictionaryCommand)`가 **용어 목록 전체를 받아 새 버전을 통째로 만든다** — `TermAppender.appendAll` javadoc이 "이전 버전에서 복사하지 않고 넘어온 목록만 그 버전의 내용이 된다"고 못박고 있다. 통합 모델이 이 구조와 정확히 맞물리고 `R-12`의 역방향 참조 결함도 함께 사라진다.

**G-4는 `WS-4`(룰셋 수정)를 크리티컬 패스로 만든다.** `RuleSet`은 `initial()`만 있고 변경 메서드가 없어 **정족수가 영구히 0**이다(`Y-14`). 룰셋을 바꿀 수단이 없으면 "리뷰어들이 승인하면"이 성립하지 않는다.

### 2-2. 문서 흐름

| ID | 확정 내용 | 상태 |
| --- | --- | --- |
| **G-5** | 업로드 대상은 `txt`·`md`지만 **파일을 서버로 보내지 않는다.** 브라우저가 파일을 읽어 **요청 본문의 텍스트로 담아 보내고** 서버는 **RDBMS에 직접 저장**한다. S3·NoSQL·multipart를 쓰지 않는다 | 확정 |
| **G-6** | 업로드 시 기준 사전집 버전은 **`null`**(미지정) | 확정 |
| **G-7** | **문서 갱신** = 대조 → 초안 → 리뷰어 지정 + 리뷰 요청 → 개정안 → 승인 → 새 버전. 새 버전의 기준 사전집 버전은 **발행 시점 최신 사전집 버전**을 따른다 | 확정 |
| **G-8** | 문서 갱신은 **사전집이 없으면 사용할 수 없다** | 확정 |
| **G-9** | **직접 편집은 리뷰 경로를 타지 않는다.** 대조·초안·개정안 없이 **즉시 새 리비전으로 발행**된다 | 확정 |
| **G-10** | **`DocumentVersion.edited` 신규**(boolean, 기본 `false`). 직접 편집으로 발행된 버전은 `true`, 대조·교정을 거친 반영본은 `false` | 확정 |
| **G-11** | 편집으로 발행된 버전의 `dictionaryVersionNo`는 **이전 버전 값을 승계**한다 | 확정 |
| **G-12** | **용어 추출 허용 조건** = `활성 사전집 versionNo == 문서 최신 확정 버전의 dictionaryVersionNo` **AND** `edited == false`. 사전집이 없으면 기준 버전이 `null`인 문서도 대상이다 | 확정 |
| **G-13** | **AI 추출을 MVP1에 포함**한다. 단 6개 계획 문서는 **계약만** 정의한다 — 추출 요청·상태 조회 엔드포인트, 상태 축(대기·실행중·성공·실패), 포트 시그니처, 대상 문서 필터 | 확정 |

**`edited`가 만드는 순환**

```
직접 편집 → edited=true → 추출 대상 제외
        → 사용자가 「갱신」 실행 → 대조·교정 → 리뷰 → 반영
        → 새 버전은 edited=false + 최신 사전집 버전 → 추출 대상 복귀
```

**G-11의 부수 효과 — `outdated`를 제거한다.** 승계하면 편집본의 `outdated`가 `false`로 남아(직전 버전이 최신 기준이었다면) "낡지 않았는데 추출 대상이 아닌" 상태가 생긴다. 두 축을 병기하면 사용자가 이유를 알 수 없으므로 **`G-12` 조건을 계산한 파생 필드 `aligned` 하나로 통합한다**(`D-31`·`R-18`).

조건이 참이면 「사전집 기준에 맞춰져 있고 그 뒤로 사람이 손대지 않았다」 = **추출 대상이자 갱신 불필요**다. 두 의미가 같은 조건이라 하나로 합쳐도 축이 갈리지 않는다. 사전집이 없으면 양쪽이 `null`이라 참이 되고, 갱신은 `G-8`로 애초에 불가하므로 일관된다.

### 2-3. 제약조건

| ID | 확정 내용 | 상태 |
| --- | --- | --- |
| **G-14** | **워크스페이스 단위 상호 배타.** 문서 초안/개정안이 존재하면 사전집 초안을 만들 수 없고, 사전집 초안/개정안이 존재하면 문서 초안을 만들 수 없다. 같은 대상에 추가 초안/개정안도 금지 | 확정 |
| **G-15** | 사전 초안이 진행 중이면 그 초안의 **원천 문서(`sourceDocumentIds`)를 직접 편집할 수 없다** | 확정 |

**G-14가 왜 필요한지** — 문서 갱신은 사전집 v_N으로 대조하는데 사전 개정이 동시에 v_N+1을 만들면, 문서 개정안이 발행될 때(`G-7`) "최신 사전집 버전"이 이미 v_N+1이어서 **대조하지 않은 버전을 기준으로 표시**하게 된다. 이 제약이 그 구멍을 막는다.

**G-15가 왜 필요한지** — 추출은 `edited == false`인 문서만 대상으로 하는데(`G-12`), 초안 진행 중에 원천 문서가 편집되면 초안이 **이미 존재하지 않는 본문**을 근거로 삼는다.

---

## 3. 이번 세션에서 확정한 나머지 결정 (`D-19`~`D-32`)

| ID | 확정 내용 | 해소 |
| --- | --- | --- |
| **D-19** | **접근 검증은 `WorkspaceAccessValidator` 직접 주입을 규약의 명문 예외로 둔다.** `docs/ARCHITECTURE.md`의 «크로스 도메인 조회 — 포트와 어댑터»에 예외 조항을 추가한다. 근거 셋 — ① 「비참여자에게 404」와 `Permission` 서열 판정이 한 곳에 남아야 한다(`WorkspaceAccessValidator` javadoc이 그 이유를 적어두었다) ② `validateAtLeast`는 예외를 던지고 값을 돌려주지 않아 `boolean` 포트에 그대로 쓸 수 없다(`REVIEW_REQUEST_PLAN.md` 9절이 이미 이 불일치를 자각하고 있다) ③ 기구현 2개와 신규 4개가 같은 방식이 된다 | `Y-01`·`Y-23` |
| **D-20** | **`CandidateTerm` 하나에 원천을 구분한다** — `origin`(`EXTRACTED`/`EXISTING`) + `sourceTermId`(EXISTING일 때 원본 `Term`) 추가, 상태에 `KEPT`(유지) 추가. `occurrenceCount`·`contextSnippets`·`occurredDocumentIds`는 EXISTING 항목에서 비운다. 교정 화면이 단일 목록, 발행이 한 테이블 조회로 끝난다 | `F-1` |
| **D-21** | **상태 전이 조건을 통합 모델에 맞춘다.** `EXAMINED`의 「미처리(`PENDING`) 0건」은 그대로 유효하다 — EXISTING 항목은 기본 `KEPT`라 `PENDING`이 아니다. `REVIEW_REQUESTED`의 「등재승인 또는 동의어편입 1건 이상」은 **「이전 버전과 달라진 항목이 1건 이상」**으로 바꾼다 — 기존 용어를 전부 유지하고 신규가 0건이면 변경 없는 개정안이 된다 | `F-1` |
| **D-22** | **상호 배타 검증은 초안만 상호 조회한다.** 초안 `status != REVISED`가 곧 「진행 중」이고(`D-10`) `REVIEW_REQUESTED` 상태가 개정안 진행을 함의하므로 **한 번의 조회로 초안과 개정안을 동시에 덮는다.** `ReviewRequest`를 경유하지 않는다 | `F-2`. `R-14` 취소 |
| **D-23** | **벡터·임베딩을 MVP1에서 배제한다.** `REQ-EXT-002`를 MVP2로 내리고 추출은 **문서 단위 LLM 일괄 호출**로 한다. `REQ-EXT-004`·`REQ-EXT-005`는 클러스터 없이 LLM 단독으로, `REQ-EXT-009`(정규화 + 편집거리)는 벡터 무관이라 그대로 둔다. **DB 전환이 없다** — MySQL 8.4 유지 | `X-07`. `R-20`·`R-22` 발생 |
| **D-24** | **메시징: Kafka 제거 확정. 로컬·테스트는 인메모리 어댑터, AWS 배포는 SQS.** 추출·대조 작업은 **DB 작업 테이블**(대기·실행중·성공·실패)로 관리하고 상태 조회는 폴링이다(`REQ-MSG-001`이 이미 폴링 우선). 어댑터 선택은 `app.messaging.mode` 프로퍼티, `EventPublisher` 포트는 그대로 쓴다 | `X-18`. 루트 `CLAUDE.md` 「메시징 배포 대상 미확정」 **해소**. `R-21` 발생 |
| **D-25** | **ID 값 객체 8개와 그 테스트 8개를 제거하고 `Long`으로 통일한다.** `docs/DOMAIN.md` 속성 표의 `WorkspaceId`·`DocumentId` 등은 「개념 표기」 각주를 단다. 초안·리뷰 3개 문서가 이미 `Long`을 쓰므로 문서 변경이 가장 적다 | `Y-09`·`Y-19` |
| **D-26** | **`originRevisionId`를 두지 않는다.** `RevisionDocument`·`RevisionDictionary`의 `resultVersionNo`가 같은 관계의 반대 방향이라 정보가 중복이고, 크로스 도메인 논리 FK가 하나 줄어든다. MVP1에 「버전에서 리뷰로 이동」 요구사항이 없다 | `Y-18`. **`T-INT-4` 불필요** |
| **D-27** | **`POST /dictionary/versions`를 임시 API로 태그해 유지하고 `DIC-7`에서 제거한다.** `REVIEW_REQUEST_PLAN.md` 7절의 `INTERNALIZE` 태그 체계를 재사용하고 `docs/API.md`에 「리뷰 도메인 완성 시 제거」를 명시한다. 지금 잠그면 사전집을 만들 방법이 없어 `DOC-1`과 `aligned` 판정을 검증할 수 없다 | `Y-03` |
| **D-28** | **`REQ-DIC-003`의 동의어·비권장어 표시를 내린다.** 대조가 저장된 표기 목록을 훑는 방식이 아니라 LLM이 맥락을 파악하는 방식이라는 제품 설계와 일관된다. 최근 변경 이력은 버전마다 `Term`이 복제되므로 `REQ-DIC-007`(버전 간 비교)이 대신한다 | `X-02`. `R-24` 발생 |
| **D-29** | **역인덱스(`REQ-IDX-001`~`003`, `NFR-IDX-001`)를 MVP1에서 제외한다.** 6개 도메인 어디에도 넣지 않고 MVP2로 내린다 | `X-06`. `R-23` 발생 |
| **D-30** | **문서 초안이 진행 중인 문서도 직접 편집을 막는다.** 편집하면 초안의 `baseVersionNo`가 낡고, 그 초안이 발행되면 사용자가 직접 고친 내용을 덮어써 날린다. `G-15`와 한 자리에서 검증한다 | `F-6` |
| **D-31** | **`outdated`를 대체할 파생 필드 이름은 `aligned`다.** `docs/UBIQUITOUS_LANGUAGE.md` 4절의 「미갱신/outdated」를 지우고 **「정렬됨/aligned — 사전집 기준에 맞춰져 있고 그 뒤로 사람이 손대지 않은 상태」**를 등재한다 | `R-18` |
| **D-32** | **`docs/TEST.md`의 `@Transactional` 롤백 권장을 정정한다.** 기구현 코드가 이미 `DbCleaner` 방식이고 `IntegrationTestSupport` javadoc이 이유를 적어두었다 | `X-16` |

---

## 3-1. 잔여 태스크 병렬화 세션이 확정한 결정 (`D-33`~`D-37`)

2026-09-12. 구현된 코드와 6개 계획 문서를 대조해 **완료 22 · 미착수 26 · 차단 2**를 확인하고, 잔여를 도메인 단위 병렬로 풀기 위해 확정한 것들이다. 차단의 정체는 **도메인 간 의존이 로직이 아니라 「포트 정의」에 걸려 있다는 것**이었다 — `DIC-3`이 기다린 것은 `RR-4b`의 구현이 아니라 `DictionaryVersionPublishPort` 인터페이스 파일 하나다.

| ID | 확정 내용 | 해소 |
| --- | --- | --- |
| **D-33** | **어댑터 배치를 포트 종류로 가른다.** **조회 포트의 어댑터는 소비 도메인**(`{소비}/infra/adapter/`)에 두고 제공 도메인의 `infra`(Repository)만 참조한다 — `ARCHITECTURE.md` «크로스 도메인 **조회**»의 명문 그대로다. **발행 위임 포트의 어댑터는 제공 도메인**(`{제공}/infra/adapter/`)에 둔다 — 발행은 제공 도메인의 도메인 로직이라 어댑터가 그 도메인의 `implement`·`service`를 써야 하고, 소비 도메인에 두면 규칙 2(역방향 금지)를 어긴다. `DICTIONARY_PLAN.md` 9절의 「어댑터는 `dictionary/infra/adapter/`에 둔다」는 **조회에 대해 틀렸고 발행에 대해 맞다** | `X-21` |
| **D-34** | **추출·대조 작업 모델은 각 초안 도메인 안에 각자 둔다.** `draftdictionary/domain/ExtractionJob`, `draftdocument/domain/CheckJob`. 공용 job 도메인을 만들지 않는다 — 도메인 경계가 유지되고 두 도메인이 서로를 기다리지 않아 병렬이 깨지지 않는다. 상태 축은 `D-24`대로 `PENDING`·`RUNNING`·`SUCCEEDED`·`FAILED`이고 조회는 폴링이다 | `G-13`이 요구한 계약의 배치 |
| **D-35** | **추출·대조는 계약과 워커 스텁까지만 만든다**(`G-13` 「계약만 정의한다」와 일치). LLM 호출은 `TermExtractorPort`·`TermCheckerPort` 뒤로 감추고 스텁이 고정 결과를 돌려준다. **service 레이어는 이 포트를 주입받지 않는다** — 주입하면 동기 호출이 가능해지므로 포트는 이벤트 핸들러만 안다. 요청은 작업 행을 `PENDING`으로 넣고 이벤트를 발행한 뒤 `202`로 끝나고, 실행은 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`가 맡는다. 실제 모델 연동은 새 의존성·API 키·프롬프트 설계가 필요하므로 **별도 승인 뒤** 한다 | `NFR-CHK-001`·`NFR-CMN-002` |
| **D-36** | **기존 동기 생성 API를 유지하고 비동기 진입점을 추가한다.** `POST /api/draft-documents`·`POST /api/draft-dictionaries`는 `SHRINK` 태그를 단 채 남기고, `POST .../checks`·`POST .../extractions`를 새로 연다. 이미 머지된 `DD-1`·`DI-1`과 프론트 목업을 건드리지 않기 위해서다. 일원화는 리뷰 경로가 다 붙은 뒤 마무리 태스크에서 한다 | — |
| **D-37** | **`G-14` 상호 배타의 「진행 중인 문서 초안」 조회는 어댑터가 문서를 거친다.** `DraftDocument`에는 `workspaceId`가 없고 `documentId`만 있어 워크스페이스 단위로 바로 물을 수 없다(`DraftDictionary`는 `workspaceId`를 갖는다). **포트 시그니처는 `boolean hasOngoingDraft(Long workspaceId)`로 두고**, 소비 도메인의 어댑터가 `DocumentRepository`로 그 워크스페이스의 문서 식별자를 구한 뒤 `DraftDocumentRepository`로 `status != REVISED`인 초안을 찾는다(`D-10`·`D-22`). **스키마를 바꾸지 않는다** — `DraftDocument`에 `workspaceId`를 더하면 `DOMAIN.md` 속성 표와 마이그레이션, 생성 경로의 `documentId → workspaceId` 해석이 함께 따라온다. 어댑터가 제공 도메인 둘(`document`·`draftdocument`)의 `infra`를 읽지만 레이어 방향 위반은 아니다 | `G-14`·`D-22` |

> **`DD-5`·`DI-5`를 신설한다.** `G-13`이 추출을 MVP1에 넣으면서 「6개 계획 문서는 계약만 정의한다」고 했는데, **그 계약을 만들 태스크가 어느 문서 12절에도 없다.** `DRAFT_DICTIONARY_PLAN.md` 1절이 `R-8`로 뒤집히기 전 서술(「추출은 MVP1 밖, 별도 도메인」)을 그대로 갖고 있고 보존 대상이라 갱신되지 않은 탓이다. `D-34`~`D-36`이 그 구멍을 메운다.

---

## 4. 큰 흐름이 뒤집은 기존 결정 (`R-1`~`R-24`)

모두 기존 문서에 「확정」으로 적혀 있던 것이다. 루트 `CLAUDE.md`의 "결정이 바뀌면 코드보다 문서를 먼저 갱신한다"에 따라 **`T-DOC-1`(9절)이 모든 구현보다 앞선다.**

### 4-1. 본문 편집이 리뷰 경로를 벗어남 (`G-9`)

| ID | 뒤집힌 결정 | 출처 | 대체 |
| --- | --- | --- | --- |
| **R-1** | 「본문이 바뀌는 유일한 경로는 대조 → 초안 → 리뷰 → 반영이다. 업로드(v1)만 예외」 | `DOMAIN.md` 문서 정책 | **편집도 예외** |
| **R-2** | 「대조의 진입점은 둘이고 산출물은 같다」 | `DOMAIN.md` 문서 정책, `UBIQUITOUS_LANGUAGE.md` 4·5절 | **진입점은 「갱신」 하나** |
| **R-3** | 「수정본을 따로 저장하는 자리는 없다 — 대조 요청의 입력이 되어 `DraftDocument.draftBody`의 초기값이 된다」 | `DOMAIN.md` 문서 정책 | **편집본이 곧 새 버전** |
| **R-4** | `REQ-CHK-007` 문서 편집 대조(MVP1·상) — "편집해 저장하면 고친 본문으로 대조가 실행되고 DraftDocument가 만들어진다" | `REQUIREMENTS.md` | **폐기** |
| **R-5** | `REQ-DOC-008` 비고 「본문 편집은 여기 없다(REQ-CHK-007)」 | `REQUIREMENTS.md` | **본문 편집 경로 신설** |
| **R-6** | 「본문을 바꾸는 엔드포인트가 없다. 문서 편집도 그 경로를 타므로 `draftdocument` 도메인이 `POST /documents/{documentId}/drafts`로 연다」 | `API.md` Document API «알아 둘 것 셋» ② | **Document 도메인의 편집 엔드포인트 신설** |
| **R-7** | 「본문을 바꾸는 유스케이스가 없다」 / 「이 엔티티에는 본문을 바꾸는 메서드가 없다」 | `DocumentService`·`Document` javadoc | **`Document`에 버전 전진 메서드 추가**(`F-4`) |

### 4-2. 사전 초안이 통합 결과가 됨 (`G-1`)

| ID | 뒤집힌 결정 | 출처 | 대체 |
| --- | --- | --- | --- |
| **R-8** | 「`ExtractTerm`은 MVP1 범위 밖. 초안과 후보어를 **수동 생성·등록 API**로 만든다」 | `DRAFT_DICTIONARY_PLAN.md` 1절 | **추출이 MVP1**(`G-13`) |
| **R-9** | `D-15` 「`DocumentQueryPort.isOutdated` 시그니처만 두고 스텁은 `false`를 반환한다. 추출이 MVP1 밖이라 정책 자체가 유예된다」 | `DRAFT_DICTIONARY_PLAN.md` 2-1절 | **추출 대상 필터가 MVP1 필수 → real 어댑터 필요**(`G-12`) |
| **R-10** | `CandidateTerm.form` = 「문서에서 추출된 표현」. 기존 `Term`은 초안에 들어오지 않는다 | `DRAFT_DICTIONARY_PLAN.md` 3절, `DOMAIN.md` | **초안이 기존 용어까지 통합**(`D-20`) |
| **R-11** | `REQ-DIC-004` 용어 수동 추가·수정·삭제(MVP1·상) ↔ 「개별 용어를 추가·수정·삭제하는 경로가 없다」 | `REQUIREMENTS.md` / `DOMAIN.md` | **요구사항은 유효하다.** 구현 자리는 **사전 초안 교정 중**이며, `DOMAIN.md` «초안 공통» 표가 이미 `교정중` 상태에서 「항목 추가·수정·삭제」를 허용한다. **확정 사전집의 `Term`을 직접 고치는 경로는 계속 없다** |
| **R-12** | `DictionaryVersionPublishPort.publish(Long dictionaryId, int baseVersionNo, List<Long> approvedCandidateTermIds)` | `REVIEW_REQUEST_PLAN.md` 9절 | **통합된 용어 목록(`List<NewTermSnapshot>`)을 넘긴다.** 현 시그니처는 Dictionary가 후보어를 조회해야 해 `dictionary -> draftdictionary` 역방향을 유발하고, `TermAppender.appendAll` javadoc대로 Dictionary에는 이전 버전 복사 로직이 아예 없다 |

### 4-3. 그 외

| ID | 뒤집힌 결정 | 출처 | 대체 |
| --- | --- | --- | --- |
| **R-13** | `DocumentVersionPublishPort.publish(Long documentId, int baseVersionNo, String body)` | `REVIEW_REQUEST_PLAN.md` 9절 | **`dictionaryVersionNo` 인자 필요**(`G-7`). 현 시그니처로는 기준 사전집 버전을 넣을 수 없다 |
| **R-14** | 제공 포트 `hasOngoingDocumentReview(Long documentId)` / `hasOngoingDictionaryReview(Long dictionaryId)` — **대상 단위** | `REVIEW_REQUEST_PLAN.md` 9절 | **`D-22`로 취소.** 초안 조회 하나가 초안과 개정안을 동시에 덮으므로 워크스페이스 단위 확장이 불필요하다 |
| **R-15** | 「사전집에 초안 또는 개정안이 존재하면 추가 초안을 생성할 수 없다」 — 도메인 **내부** 제약 | `DOMAIN.md` «초안 사전», `D-10` | **워크스페이스 단위로 확대**(`G-14`) |
| **R-16** | `NFR-DOC-002` 원문 저장소 추상화(로컬 FS → S3), 상태 「보류」 | `REQUIREMENTS.md` | **폐기 확정**(`G-5`) |
| **R-17** | `NFR-DOC-001` 「확장자 화이트리스트, 파일 크기 상한, 인코딩(UTF-8) 검증」 | `REQUIREMENTS.md` | **서버가 파일을 받지 않아 검증할 수 없다**(`G-5`). 확장자·인코딩은 **클라이언트 책임**, 서버는 길이 검증(제목 200자·본문 10,000자, 이미 구현)만 |
| **R-18** | `outdated` | `DOMAIN.md` 문서 정책 / `UBIQUITOUS_LANGUAGE.md` 4절 / `API.md`(응답 필드 + 판정 설명 3곳) / `REQ-DOC-002`·`REQ-DOC-006` 비고 / 코드 4곳(`DocumentVersion.isOutdated`, `DocumentVersionSummary.isOutdated`, `DocumentResponse`, `DocumentSummaryResponse`) | **제거하고 `aligned`로 통합**(`D-31`). 판정 규칙을 static 한 곳에 두는 기존 패턴은 그대로 유지 |
| **R-19** | `WorkspacePolicyPort`의 3개 메서드(`requiredReviewerCount`, `isParticipant`, `hasAdminPermission`)와 어댑터 배치 문단 | `REVIEW_REQUEST_PLAN.md` 9절 | **`requiredReviewerCount`(룰셋 조회) 하나로 축소**(`D-19`). 참여 여부·권한은 `WorkspaceAccessValidator` 직접 주입으로 대체 |
| **R-20** | `NFR-EXT-001` 「용어 쌍별 개별 API 호출 금지. **클러스터 단위** 일괄 호출」 | `REQUIREMENTS.md` | **「문서 단위 일괄 호출」**(`D-23`). 클러스터링이 MVP2로 내려가 클러스터가 존재하지 않는다 |
| **R-21** | `NFR-MSG-001`~`006`(Kafka 토픽·Outbox·DLT·`max.poll.interval.ms`·파티션 키), `NFR-CHK-001` 「대조는 Kafka 이벤트로 처리」 | `REQUIREMENTS.md` | **SQS 기준으로 재작성**(`D-24`). 순서 보장은 FIFO `MessageGroupId`, DLT는 SQS DLQ, `max.poll.interval.ms`는 가시성 타임아웃 |
| **R-22** | `REQ-EXT-002`(임베딩·클러스터링, MVP1·상, "Gemini Embedding + pgvector"), `NFR-INF-001`·`NFR-INF-004`의 PostgreSQL·pgvector | `REQUIREMENTS.md` | **`REQ-EXT-002`는 MVP2로, DB는 MySQL 8.4 유지**(`D-23`) |
| **R-23** | `REQ-IDX-001`~`003`·`NFR-IDX-001` 역인덱스(MVP1) | `REQUIREMENTS.md` | **MVP2로 내린다**(`D-29`) |
| **R-24** | `REQ-DIC-003` 「대표어·정의·**동의어·비권장어**·최근 변경 이력 표시」 | `REQUIREMENTS.md` | **동의어·비권장어 표시를 내린다**(`D-28`) |

> **`docs/plan/`의 기존 3개 문서는 고치지 않는다.** `R-8`~`R-14`·`R-19`가 그 문서들을 향하지만 보존이 원칙이므로, 이 문서가 ID로 지목하고 각 도메인 세션이 자기 계획 문서에서 대체 내용을 따른다.

---

## 5. 뒤집힘이 만든 새 과제 (`F-1`~`F-6`)

| ID | 과제 | 확정 |
| --- | --- | --- |
| **F-1** | 통합 모델에서 `CandidateTerm`의 형태와 상태 전이 조건 | `D-20`·`D-21` |
| **F-2** | `G-14` 상호 배타 검증의 조회 대상 | `D-22` |
| **F-3** | `outdated` 제거와 파생 필드 통합 — 문서 4곳·코드 4곳을 함께 고쳐야 한다. 원시 값 `dictionaryVersionNo`·`edited`도 응답에 남겨 판정 근거를 볼 수 있게 한다 | `D-31` |
| **F-4** | **`Document`에 본문 전진 경로 신설** — `currentVersionNo` 증가 메서드 + `DocumentVersion.publishEdited(...)`/`publishRevised(...)`. 미사용 상태인 `PublishedVersion.next()`가 여기서 처음 호출된다. `Document`·`DocumentService` javadoc(`R-7`)도 함께 고친다 | `DOCUMENT_PLAN.md` 4절 |
| **F-5** | **업로드는 손댈 것이 없다** — multipart를 만들지 않는다. 현재 `CreateDocumentRequest(title, content, labels)` JSON 방식이 그대로 정답이다. "파일을 서버로 보내지 않는다"는 결정과 이유를 문서에 남겨 나중에 다시 꺼내지 않게 한다 | `G-5`·`R-17` |
| **F-6** | 문서 초안 진행 중인 문서의 편집 금지 | `D-30` |

---

## 6. 문서 ↔ 문서 충돌 (`X-01`~`X-21`)

| ID | 충돌 | 근거 | 결론 | 상태 |
| --- | --- | --- | --- | --- |
| **X-01** | `REQ-DIC-004` 용어 수동 추가·수정·삭제 ↔ 「개별 용어를 추가·수정·삭제하는 경로가 없다」 | `REQUIREMENTS.md` / `DOMAIN.md` «사전집» | **실은 충돌이 아니었다.** 후자는 **확정 사전집**에 대한 규정이고, 같은 `DOMAIN.md`의 «초안 공통» 표는 `교정중` 상태에서 「항목 추가·수정·삭제」를 이미 허용한다 | 해소(`R-11`) |
| **X-02** | `REQ-DIC-003` 「동의어·비권장어·최근 변경 이력 표시」 ↔ 「동의어·비권장어는 두지 않는다」 | `REQUIREMENTS.md` / `DOMAIN.md`·`UBIQUITOUS_LANGUAGE.md` 3절 | 동의어·비권장어 표시를 내린다. 변경 이력은 `REQ-DIC-007` | 해소(`D-28`) |
| **X-03** | `CandidateTerm.status`의 `동의어편입`과 `mergeTargetTermId` ↔ 동의어를 저장하지 않으므로 **편입 결과를 넣을 필드가 `Term`에 없다** | `DOMAIN.md` `CandidateTerm` 표 / `Term` 표 | 통합 모델에서 대표어로 합치고 후보어에 판정만 기록한다. `Term`에 동의어 필드가 필요 없다 | 해소(`G-1`) |
| **X-04** | `REQ-REV-006` 「기각된 후보는 재추출 시 재노출을 억제한다」 ↔ 억제 근거를 담을 자리가 없다 | `REQUIREMENTS.md` / `DOMAIN.md` | **`G-13`으로 다시 살아난 항목이다.** `DRAFT_DICTIONARY_PLAN.md` 1절이 "추출이 있어야 의미가 있으므로 MVP1에서는 기각 사유 기록까지만"이라고 미뤘는데, 추출이 MVP1에 들어와 그 근거가 사라졌다. **제안**: 초안은 `반영완료`로 종단되지만 행이 이력으로 남으므로, 다음 회차 추출이 **이전 초안의 `REJECTED` 후보어를 조회해 제외**한다 — 같은 도메인 안이라 역방향 문제가 없다. `DI-*` 태스크에서 확정 | 제안 |
| **X-05** | `REQ-UPD-002` 「거절 사유를 기록하고 동일 지점의 재알림을 억제」 ↔ 같은 구조 | `REQUIREMENTS.md` / `DOMAIN.md` | **제안**: `anchor` 기반 억제는 본문이 바뀌면 위치가 어긋나므로, 거절된 `(originTerm, suggestionTerm)` 쌍을 **문서 단위**로 억제한다. `DD-*` 태스크에서 확정 | 제안 |
| **X-06** | `REQ-IDX-001`~`003`·`NFR-IDX-001` 역인덱스 ↔ `DOMAIN.md`에 해당 엔티티가 없다 | `REQUIREMENTS.md` / `DOMAIN.md` | MVP1에서 제외하고 MVP2로 내린다 | 해소(`D-29`) |
| **X-07** | `REQ-EXT-002` pgvector, `NFR-INF-001`·`NFR-INF-004` PostgreSQL ↔ 실제 MySQL 8.4 | `REQUIREMENTS.md` / `backend/CLAUDE.md` | 벡터를 MVP1에서 배제하고 MySQL을 유지한다 | 해소(`D-23`) |
| **X-08** | `NFR-CMN-003` 「에러 포맷 code·message·**traceId**」 ↔ 2필드(`{code, message}`) | `REQUIREMENTS.md` / `EXCEPTION.md`·`API.md`·`ErrorResponse` | **제안**: `LOG.md`가 이미 「에러 응답에 trace id 포함」과 MDC 규약을 갖고 있으므로 `ErrorResponse`에 `traceId`를 더한다. `T-INT-3`(인증·공통 설정)과 함께 처리 | 제안 |
| **X-09** | `API.md` 공통 규칙은 검증 실패 코드를 `INVALID_INPUT`이라 하는데, 같은 문서 에러 표와 구현은 `COMMON_INVALID_REQUEST`다 | `API.md` «공통 규칙» / 같은 문서의 각 도메인 에러 표, `CommonErrorCode` | 구현이 정답이다. `T-DOC-1`에서 공통 규칙 문구를 `COMMON_INVALID_REQUEST`로 고친다 | 제안 |
| **X-10** | `REQ-WS-001`의 「태그·설명·공개여부」와 `REQ-WS-006`의 「설명(도메인 소개)」 ↔ `Workspace`에 `description`이 없다 | `REQUIREMENTS.md` 비고 / `DOMAIN.md` `Workspace` 표 | 양쪽 비고가 「모델 미정의 — 필요해지면 `DOMAIN.md`에 먼저 추가」로 이미 처리했다. **모델에 넣지 않고 근거만 기록**한다 | 제안 |
| **X-11** | `DOMAIN.md` «모델 반영 필요(미확정)» 3건 — 참여자 권한 변경 주체 / 참여자 삭제 방식(`leftAt`) / 알림 설정 모델 | `DOMAIN.md` «모델 반영 필요(미확정)» 블록 | 권한 변경 주체는 `WS-1`, 삭제 방식은 `WS-2`에서 확정한다. 알림 설정 모델은 Notification 도메인 몫이라 6개 범위 밖(`X-12`) | 제안 |
| **X-12** | `Notification`이 `DOMAIN.md`·`UBIQUITOUS_LANGUAGE.md`(「미정」)에 있으나 6개 도메인 범위 밖 ↔ 각 도메인이 발행하는 이벤트의 소비자가 없다 | `DOMAIN.md` / 3개 계획 문서 9절의 「Notification(미정)」 | **이벤트는 발행하고 소비자가 없어도 된다.** 6개 문서 9절은 수신자 칸에 `Notification(미정)`을 그대로 쓴다 | 제안 |
| **X-13** | `Invitation` 엔티티 표가 `DOMAIN.md`에 있으나 코드·API가 전무 | `DOMAIN.md` «Invitation (초대)» 표 / `REQ-WS-003` 「대기」 | `WORKSPACE_PLAN.md` 3절에 `상태=추가`로 싣고 `WS-3`에서 구현한다 | 제안 |
| **X-14** | 참여자 정원 5명 ↔ `RuleSet` 상한 「참여자 수 이하(0~5)」 — 검증 주체는 Workspace로 확정됐으나 구현·API가 없다 | `DOMAIN.md` «워크스페이스 · 권한»·«리뷰 규칙» / `RuleSet.java` 주석 | `WS-4`가 룰셋 수정과 정원 검증을 함께 구현한다. `G-4`의 선행 | 제안 |
| **X-15** | `NFR-DOC-002` 원문 저장소 추상화(S3)가 「보류」로 살아 있다 | `REQUIREMENTS.md` | 폐기 확정 | 해소(`G-5`·`R-16`) |
| **X-16** | `TEST.md`는 `@Transactional` 롤백을 권장하는데 3개 계획 문서 11절은 금지한다 | `TEST.md` «DB 테스트 독립 환경 설정» / 3개 계획 문서 11절, `IntegrationTestSupport` javadoc | `TEST.md`가 낡았다. `T-DOC-1`에서 정정 | 해소(`D-32`) |
| **X-17** | `REQUIREMENTS.md` 표 자체의 오류 — `NFR-USR-001`이 서로 다른 5개 행에 중복, `REQ-EXT-002`의 「구분」 칸에 `REQ-EXT-002`가 들어감, `REQ-MV2-004`는 MVP 칸이 MVP3인데 비고는 「MVP2」 | `REQUIREMENTS.md` | `T-DOC-1`에서 함께 정리 | 제안 |
| **X-18** | 「미결정-8」 — `NFR-INF-004` 「Kafka는 MSK 또는 EC2 단일 노드」가 미해소 | `REQUIREMENTS.md` 비고 | Kafka 제거 확정. 로컬 인메모리 / AWS SQS | 해소(`D-24`) |
| **X-19** | `D-*` 결정 ID가 3개 문서 전역 공유(`D-1`~`D-18`)여서 신규 문서가 같은 번호를 쓰면 충돌한다 | 3개 계획 문서 2-1절 | `D-19`부터 이어 붙인다. 새 결정은 `D-33`부터 | 해소(1절) |
| **X-20** | **`DraftDictionary.dictionaryId`가 필수(`O`)인데 첫 회차에는 가리킬 사전집이 없다.** `DOMAIN.md` «사전집 생성 주기»는 첫 사전집이 초안 → 리뷰 → 발행으로 태어난다고 규정하므로, 그 초안에는 `dictionaryId`가 존재할 수 없다 | `DOMAIN.md` `DraftDictionary` 표 / `DOMAIN.md` «사전집 생성 주기» | **`G-1`의 직접 귀결이라 nullable로 정정했다**(`T-DOC-1` 실행 중 발견). 발행은 `workspaceId`로 「이 워크스페이스의 다음 버전」을 만들며, `DictionaryService.appendNextVersion`이 이미 첫 버전과 다음 버전을 한 경로로 다룬다 | 해소 |
| **X-21** | **어댑터 배치가 두 문서에서 반대다.** `ARCHITECTURE.md` «크로스 도메인 조회 — 포트와 어댑터»는 「어댑터도 **소비 도메인**이 구현한다. `{소비도메인}/infra/adapter/`에 두고 제공 도메인의 `infra`만 참조한다」이고, `DICTIONARY_PLAN.md` 9절 «어댑터 배치»는 「어댑터는 `dictionary/infra/adapter/`에 두고 **스텁은 소비 도메인이 갖는다**」이다. **코드가 양쪽을 섞어 따랐다** — `DOC-1`의 `document/infra/adapter/DictionaryQueryAdapter`는 소비 측, `DIC-4`의 어댑터 2개와 `WS-4`의 `WorkspacePolicyAdapter`는 제공 측 | `ARCHITECTURE.md` «크로스 도메인 조회» / `DICTIONARY_PLAN.md` 9절 / `infra/adapter/` 6개 | 절 제목이 「조회」인 것이 답이다. **조회는 소비 도메인, 발행 위임은 제공 도메인**으로 가른다 | 해소(`D-33`) |

---

## 7. 문서 ↔ 코드 충돌 (`Y-01`~`Y-30`)

| ID | 충돌 | 근거 | 결론 | 담당 |
| --- | --- | --- | --- | --- |
| **Y-01** | **크로스 도메인 직접 참조** — `DocumentService`·`DictionaryService`가 `workspace.implement.WorkspaceAccessValidator`와 `workspace.domain.Permission`을 직접 import한다 | `ARCHITECTURE.md` «크로스 도메인 조회 — 포트와 어댑터»(2026-09-10) / 두 서비스의 import | **직접 주입을 규약의 명문 예외로 확정**했다. 기구현 2개와 신규 4개가 같은 방식이 된다 | 해소(`D-19`) |
| **Y-02** | `DocumentService.activeDictionaryVersionNo()`가 `TODO(REQ-DIC-001)`로 `null`을 반환해 판정이 항상 `false`다. dictionary는 이미 구현돼 있고 `DictionaryReader.readActiveOptional`이 필요한 값을 갖고 있다 | `DocumentService` / `DictionaryReader` | `G-12`가 이 배선을 필수로 만든다. `DictionaryQueryPort` + 어댑터로 해소 | `DOC-1` |
| **Y-03** | `POST /api/workspaces/{id}/dictionary/versions`(ADMIN 직접 반영) ↔ 「새 버전은 리뷰 승인의 반영으로만 생긴다」·`NFR-UPD-001` | `DOMAIN.md` «사전집» / `DictionaryController`(코드가 `TODO(REQ-REV-005)`로 자각) | 임시 API로 태그해 유지하고 `DIC-7`에서 제거 | 해소(`D-27`) |
| **Y-04** | **`docs/API.md`에 Dictionary API 절이 없다.** 컨트롤러 4개 엔드포인트가 미문서화 — `WLSH-29 docs: 사전집 도메인 모델과 API 반영` 커밋이 있는데도 누락됐다 | `API.md`(Auth → Workspace → Document로 끝남) / `DictionaryController` | `DICTIONARY_PLAN.md` 7절이 추가할 절의 초안을 담고 `T-DOC-1`이 반영한다 | `DIC-1` |
| **Y-05** | **페이징 규격 미적용** — 문서 목록·버전 이력·라벨 목록·사전집 버전 이력·워크스페이스 목록이 전부 배열이고 `PageResponse`/`PageResult` 클래스 자체가 없다 | `API.md` «페이징·정렬 규격» 「배열을 그대로 반환하지 않는다」 / 5개 컨트롤러 | 두 공유 자산을 **선행 공통 태스크 `T-CMN-1`으로 분리**해 가장 먼저 머지한다. 워크스페이스 목록은 `API.md`가 이미 명시한 예외로 배열 유지 | `T-CMN-1` |
| **Y-06** | **도메인 코드에 로그가 0건이다.** 로그를 남기는 곳은 `GlobalExceptionHandler`·`AsyncEventConfig` 둘뿐이고, `WorkspaceAccessValidator`가 403을 던지면서 감사 로그를 남기지 않는다 | `LOG.md` 전체 / `API.md` «데이터 격리» 「이때는 WARN 감사 로그를 남긴다(`NFR-REV-001`)」 | 각 도메인의 감사 로그 태스크에서 `LOG.md`의 `[클래스명.메서드명]` prefix 규약에 맞춰 넣는다 | `WS-*`·`DOC-9`·`DIC-8` |
| **Y-07** | 삭제 경로가 없는 엔티티에 `deleted_at`이 항상 null로 남는 대신 감사 상위 클래스가 둘로 갈라져 있다 | `BaseEntity` / `Dictionary`·`DocumentVersion`·`Label`·`DocumentLabel` | 후속 결정으로 모든 엔티티를 `BaseEntity`로 통일하고 `AuditableEntity`를 제거한다. 삭제 경로가 없는 엔티티의 `deleted_at`은 nullable로 유지한다 | 해소(후속 결정) |
| **Y-08** | `Term`이 공통 감사 상위 클래스 없이 `@CreationTimestamp`/`@UpdateTimestamp`를 직접 선언한다 | `Term` / `BaseEntity` | `BaseEntity` 상속으로 대체하고 `V310`에서 `term.deleted_at`을 추가한다 | `DIC-2` |
| **Y-09** | **ID 값 객체 8개가 정의만 되고 호출부가 없다**(`WorkspaceId`·`ParticipantId`·`DocumentId`·`DocumentVersionId`·`DocumentLabelId`·`LabelId`·`DictionaryId`·`TermId`). 엔티티는 raw `Long`을 쓴다. `DOMAIN.md` 속성 표는 이 타입을, 초안·리뷰 3개 문서는 `Long`을 쓴다 | 8개 record / `DOMAIN.md` 속성 표 / 3개 계획 문서 3절 | 전부 제거하고 `Long`으로 통일한다 | 해소(`D-25`) |
| **Y-10** | **Flyway 대역 위반** — `backend/CLAUDE.md`는 workspace = 100–199인데 실제는 `V2__create_workspace_and_participant.sql`이다. 400/500/600이 붙으면 머지 순서와 번호 순서가 어긋나 out-of-order가 필연이다 | `backend/CLAUDE.md` / `db/migration/` | 기존 파일은 고치지 않고 **신규만 대역을 지킨다**(workspace `V100`~, document `V210`~, dictionary `V310`~). `spring.flyway.out-of-order=true`는 `T-INT-1` | ~~`T-INT-1`~~ **폐기**(2026-09-12) — 개발 브랜치 DB를 항상 리셋하므로 이력이 비어 있고 전체가 버전 순서대로 한 번에 적용된다. 낮은 번호가 나중에 머지돼도 거부되지 않는다. **`V2`가 대역 밖인 것은 그대로 두고 신규만 대역을 지킨다** |
| **Y-11** | **이벤트 구현이 0건이다.** `DomainEvent` 마커와 `EventPublisher` 포트만 있고 어떤 도메인도 발행하지 않는다 | `ARCHITECTURE.md` «이벤트 발행 규약» / `DOMAIN.md` 「파생 데이터 정리는 삭제 이벤트를 각 도메인이 구독해 처리한다」 / `DocumentRemover` javadoc | 각 도메인의 이벤트 태스크에서 발행부를 넣는다. 구독자는 초안 도메인 | `WS-5`·`DOC-10`·`DIC-8` |
| **Y-12** | `workspace` 패키지가 하위 디렉터리 규약을 벗어난다 — `presentation/dto/`·`service/model/` 없이 루트에 DTO·command가 있고, 컨트롤러가 `toCommand()` 없이 `new CreateWorkspaceCommand(...)`를 직접 조립한다 | `D-18` 기본값 / `member`·`document`·`dictionary`는 규약 준수 | `WS-6`에서 정리한다 | `WS-6` |
| **Y-13** | `ParticipantRepository.countByWorkspaceIdAndDeletedAtIsNull`이 선언만 되고 호출부가 없다 — 정원 5명 검증용으로 만들어졌다 | `ParticipantRepository` / `DOMAIN.md` 「참여자는 최대 5명」 | `WS-4`가 룰셋 상한 검증과 함께 쓴다 | `WS-4` |
| **Y-14** | **참여자 관리·룰셋 수정 API가 전무하다.** `Participant.owner(...)` 팩토리만 있고 초대·권한 변경·내보내기·소유권 이전이 없다. `RuleSet`은 `initial()`만 있고 변경 메서드가 없어 **정족수가 영구히 0**이다 | `REQ-WS-003`「대기」·`REQ-WS-004`「진행중」·`REQ-WS-005`「대기」 / `RuleSet`·`ParticipantFixture` javadoc | `G-4`(룰셋 정족수)가 이것을 **6개 도메인 전체의 크리티컬 패스**로 만든다 | `WS-1`~`WS-4` |
| **Y-15** | `SecurityConfig`가 없다. Spring Security 의존성이 있으므로 기본 설정이 적용되면 Swagger UI를 포함한 모든 요청이 인증에 막힌다. 컨트롤러 테스트가 `@AutoConfigureMockMvc(addFilters = false)`로 우회하고 있다 | 루트 `CLAUDE.md` «미구성 항목» / 컨트롤러 테스트 | `T-INT-3`(인증 도메인 별건) | `T-INT-3` |
| **Y-16** | 프로파일 분리가 없다 — `application.properties`가 2줄이고 `application-local/dev/prod`·`application-test`가 모두 없는데 테스트는 `@ActiveProfiles("test")`를 쓴다 | `NFR-INF-002` / `src/{main,test}/resources` | `T-INT-3`에서 함께 정리 | `T-INT-3` |
| **Y-17** | `Document.currentVersionNo` 전진 경로와 `PublishedVersion.next()` 호출부가 없다. v2 이상은 테스트가 `ReflectionTestUtils`로만 만든다 | `Document` javadoc / `DocumentVersionFixture` javadoc | **`G-9`(직접 편집)와 `G-7`(반영)이 둘 다 이 경로를 필요로 한다.** `F-4`로 신설 | `DOC-2`·`DOC-6` |
| **Y-18** | **스키마 소유권 충돌** — `DOMAIN.md`는 `DocumentVersion.originRevisionId`와 `Dictionary`의 유래 리비전을 「리뷰 도메인에서 추가한다」로 남겼는데, `EXECUTION_ORDER.md` 3절은 「다른 도메인의 패키지 전체」를 수정 금지로 둔다 | `DOMAIN.md`의 `DictionaryVersion`·`DocumentVersion` 각주 / `EXECUTION_ORDER.md` 3절 | **컬럼을 두지 않기로 확정**해 충돌 자체가 소멸했다 | 해소(`D-26`) |
| **Y-19** | ID 값 객체 record 8개와 `Member.changeDisplayName`만 `IllegalArgumentException`을 던진다 — 나머지 도메인은 전부 `BusinessException`이다 | `EXCEPTION.md` «리뷰 체크리스트» 「예상 가능한 실패를 Exception이나 RuntimeException으로 직접 던지지 않았는가」 | record 제거로 8건이 사라진다. `Member`는 6개 범위 밖이라 기록만 | 해소(`D-25`) |
| **Y-20** | `member`가 구버전 패턴이다 — ErrorCode가 `domain/`에(`ARCHITECTURE.md`는 `{domain}/exception`), `implement` 테스트만 Mockito 단위, `MySqlContainerConfiguration` 중복 정의 | `member` 패키지 / `ARCHITECTURE.md` «Domain 작성 규칙» | 6개 범위 밖이지만 **신규 도메인이 선례로 오인할 위험**이 있어 기록한다. 기준 도메인은 `dictionary`·`document`다 | 기록만 |
| **Y-21** | 3개 계획 문서 13절이 「`refacotr/WLSH-86-base-entity`(BaseEntity 감사 애노테이션 교체)가 진행 중」을 리스크로 들고 `EXECUTION_ORDER.md` 4절이 그 머지 직후 동기화를 지시하는데, **이미 머지됐다**(`b15263d`) | 3개 계획 문서 / `git log` | `EXECUTION_ORDER.md` 4절에서 해당 조항만 걷어낸다. 3개 계획 문서는 보존 | `T-DOC-1` |
| **Y-22** | 의존성과 컨테이너만 있고 사용 코드가 0줄인 것 — MongoDB·Redis·JJWT·OAuth2·SpringDoc. **MongoDB는 어느 도메인이 쓸지 문서에도 코드에도 흔적이 없다** | `build.gradle` / `compose.yaml` / 소스 트리 | Redis·JJWT·OAuth2·SpringDoc은 `T-INT-3`에서 쓰인다. **MongoDB는 용처를 정하거나 걷어내야 한다** — `D-24`로 메시징이 SQS가 되어 후보 하나가 사라졌다 | 제안 |
| **Y-23** | `ARCHITECTURE.md` 절 제목이 「크로스 도메인 **조회**」이고 본문이 "포트는 조회와 발행 위임에만 쓴다"인데, **접근 검증이 조회인지 아닌지 답하지 않는다.** 기구현 2개는 직접 참조, 초안·리뷰 3개 문서는 어댑터를 요구한다 | `ARCHITECTURE.md` «크로스 도메인 조회» / 3개 계획 문서 9절 | 예외 조항을 명문화한다 | 해소(`D-19`) |
| **Y-24** | **`WS-4`가 다른 도메인의 파일을 만들었다** — `reviewrequest/infra/port/WorkspacePolicyPort`는 `REVIEW_REQUEST_PLAN.md` 6절 트리가 `RR-3c`의 산출물로 둔 것인데 `WS-4` 세션이 어댑터와 함께 만들었다. `EXECUTION_ORDER.md` 3절 «아무도 수정하지 않는 파일»의 「다른 도메인의 패키지 전체」에 걸린다 | `WorkspacePolicyPort` / `REVIEW_REQUEST_PLAN.md` 6절 / `EXECUTION_ORDER.md` 3절 | **위치와 시그니처가 `R-19`대로 정확하므로 지우지 않는다.** `RR-3c` 세션은 이 포트를 **새로 만들지 말고 그대로 쓴다.** 현재 소비자가 0곳이라 `RR-3c` 전까지는 스텁만 선택된다 | 기록 |
| **Y-25** | **`draftdocument/infra/port/DictionaryTermQueryPort`에 소비자가 0곳이다.** 어댑터는 있는데 주입받는 곳이 없어 `O-3`(`suggestionTerm` 값의 출처)이 코드에서는 아직 이어지지 않았다 | 포트·어댑터 / `SuggestionTermService`·`SuggestionTermWriter` | `DD-3`(교정 흐름)과 `DD-5`(대조 계약)가 실제로 주입해 잇는다 | `DD-3`·`DD-5` |
| **Y-26** | `AddSuggestionTermRequest`가 `common/domain/TextRange`를 **request DTO에 그대로 노출**한다. `DRAFT_DOCUMENT_PLAN.md` 6절은 `TextRangeRequest.java`를 별도 산출물(P2)로 뒀다 | `AddSuggestionTermRequest` / `DRAFT_DOCUMENT_PLAN.md` 6절 | 도메인 값 객체가 presentation까지 올라간 형태다. `DD-3`에서 `TextRangeRequest`로 분리하거나, 유지할 근거를 남긴다 | `DD-3` |
| **Y-27** | **ReviewRequest 테스트 공백** — `ReviewerController`·`RevisionController` 컨트롤러 테스트가 없고, 리포지토리 4개 중 `ReviewRequestRepository` 하나만 테스트가 있다. `ReviewerDuplicationValidatorTest`·`RevisionTypeValidatorTest`는 대상이 `implement`인데 **테스트가 `domain` 패키지에 있다** | `reviewrequest` 테스트 11개 / `TEST.md` | `review-req-phase-3` 착수 시 함께 메운다. 패키지 위치는 대상에 맞춘다 | `RR-3a` |
| **Y-28** | **죽은 프로퍼티** — `app.worker.enabled=false`가 `src/test/resources/application.yml`에만 있고 이 키를 읽는 코드가 0줄이다. `app.messaging.mode`는 반대로 **어디에도 선언돼 있지 않고** `InMemoryEventPublisher`의 `matchIfMissing = true`에만 의존한다(`D-24`가 이 키로 어댑터를 고르라고 규정했다) | `test/resources/application.yml` / `InMemoryEventPublisher` / `D-24` | `app.worker.enabled`를 지우고 `app.messaging.mode`를 `application.yml`에 명시한다 | 해소 예정(선행 PR) |
| **Y-29** | **승계 용어의 정의가 사라지고 발행이 터진다.** `DraftDictionaryWriter.create`가 `TermSnapshot(termId, preferredForm, englishName, **definition**)`을 읽고도 `CandidateTerm.createExisting(draftDictionaryId, sourceTermId, form, english, createdBy)`에 **definition을 넘길 자리가 없어 버린다.** 그 결과 ① 교정 화면에서 이전 사전집의 정의를 볼 수 없고 ② `EXISTING`/`KEPT` 후보의 `proposedDefinition`이 항상 `null`이라 `readFinalTerms`가 그대로 실어 보내면 `Term.create`가 「정의는 비어 있을 수 없다」로 거절한다(`Term.definition`은 `@Column(nullable = false)`). **`G-1` 통합 모델의 「사전집이 있는 경우」 회차가 통째로 막힌다** | `DraftDictionaryWriter` / `CandidateTerm.createExisting` / `Term.create` | `createExisting`에 `definition`을 더하고 `DraftDictionaryWriter`가 `term.definition()`을 넘긴다. **판정 시점에 정의가 빈 항목을 걸러내는 것은 `DI-3`의 몫**이다 — 사용자가 정의 없이 등재 승인하면 같은 지점에서 터진다 | 선행 PR(즉시) + `DI-3` |
| **Y-30** | **문서 편집마다 `draft_dictionary` 전체를 읽는다.** `document/infra/adapter/DraftDictionaryQueryAdapter.isSourceOfOngoingDraft`가 `findAll().stream()`으로 전체를 메모리에 올려 거른다(`DOC-5`). 스텁이 걸려 있는 동안은 실행되지 않았으나 **선행 PR이 `app.crossdomain.draft-dictionary.mode`를 `real`로 올리면서 live가 됐다** | `DraftDictionaryQueryAdapter` / `DOC-5` | `sourceDocumentIds`가 `@ElementCollection`이므로 `join`을 쓰는 `@Query`로 바꾼다. 활성화한 쪽이 선행 PR이므로 거기서 함께 고친다 | 선행 PR(즉시) |

---

## 8. 기존 계획 문서가 남긴 열린 질문 (`O-1`~`O-4`)

| ID | 질문 | 남긴 문서 | 답 |
| --- | --- | --- | --- |
| **O-1** | 승인된 후보어를 `Term`으로 만들고 새 버전을 발행하는 주체가 Dictionary인지 ReviewRequest의 `Revise`인지 — 「Dictionary 도메인 착수 시 합의」로 미뤘다 | `DRAFT_DICTIONARY_PLAN.md` 13절 | **초안이 최종 목록을 제공하고 Dictionary가 발행한다.** ADMIN의 수동 반영(`G-3`)이 `DictionaryVersionPublishPort`를 호출하고, 포트는 통합 목록을 받는다(`R-12`) | 해소(`G-1`·`G-3`) |
| **O-2** | 참여자 이탈 이벤트를 Workspace가 발행할지 미정 — 리뷰어였던 참여자가 빠질 때 | `REVIEW_REQUEST_PLAN.md` 9·13절(수신 이벤트 표에 `**미정**`) | **`WORKSPACE_PLAN.md`가 `ParticipantRemovedEvent(workspaceId, memberId, occurredAt)`를 정의하고 `WS-5`에서 발행한다** | `WS-5` |
| **O-3** | `SuggestionTerm.suggestionTerm` 값의 출처 — 「Dictionary 도메인이 생기면 `DictionaryQueryPort`로 승격 제안」 | `DRAFT_DOCUMENT_PLAN.md` 13절 | **이미 생겼다.** `DICTIONARY_PLAN.md` 9절이 표준어·정의 스냅샷 조회 포트를 제공한다(`DIC-4`) | `DIC-4` |
| **O-4** | `DocumentQueryPort.isOutdated` 스텁(`false`)의 real 전환 시점 | `DRAFT_DICTIONARY_PLAN.md` 2-1절 `D-15` | 추출 대상 필터가 MVP1 필수라 즉시 필요하다. `DOC-1` 직후 전환하고 이름·의미는 `aligned`로 바뀐다 | 해소(`G-12`·`D-31`) |

---

## 9. `T-DOC-1` — 문서 선행 수정

`R-1`~`R-24`와 `D-19`~`D-32`가 요구하는 문서 수정이다. `EXECUTION_ORDER.md` 3절이 이 파일들의 주인을 「아무도」로 두었으므로 **이 태스크가 유일한 수정 경로**이며, 그 변경만 담은 커밋을 따로 만든다.

**모든 구현 태스크보다 앞선다.** 루트 `CLAUDE.md`가 "결정이 바뀌면 코드보다 문서를 먼저 갱신한다"를 규정하기 때문이다.

> **실행 완료 (2026-09-10).** 아래 표의 8개 파일을 모두 수정했다. 실행 중 `X-20`(`DraftDictionary.dictionaryId` 필수 여부)을 발견해 함께 정정했고, `REQUIREMENTS.md`의 `NFR-USR-001` 5중복은 대표 ID 하나만 남기고 하위 항목을 `NFR-USR-002`~`005`로 나눴다 — `docs/API.md`와 컨트롤러의 `TODO(NFR-USR-001)`이 대표 ID를 가리키므로 기존 참조가 깨지지 않는다.

| 파일 | 수정 내용 |
| --- | --- |
| `docs/DOMAIN.md` | 문서 정책 3개 문장 교체(`R-1`~`R-3`) / `DocumentVersion`에 `edited` 추가·유래 리비전 행 정리(`G-10`·`D-26`) / outdated 정책 → `aligned`(`D-31`) / 추출 조건에 `edited == false` 추가(`G-12`) / 초안 정책에 워크스페이스 단위 배타·편집 금지 추가(`G-14`·`G-15`·`D-30`·**`R-15`**) / `CandidateTerm`에 `origin`·`sourceTermId` 추가·상태에 `KEPT`(`D-20`·**`R-10`**) / «모델 반영 필요» 블록에서 해소 항목 정리(`X-11`) / 속성 표 ID 타입 각주(`D-25`) |
| `docs/REQUIREMENTS.md` | `REQ-CHK-007` 폐기(`R-4`) / `REQ-DOC-008` 비고(`R-5`) / `NFR-DOC-001`·`NFR-DOC-002`(`R-16`·`R-17`) / `REQ-DOC-002`·`REQ-DOC-006` 비고의 outdated(`R-18`) / `NFR-EXT-001`(`R-20`) / `NFR-MSG-001`~`006`·`NFR-CHK-001`(`R-21`) / `REQ-EXT-002`·`NFR-INF-001`·`NFR-INF-004`(`R-22`) / `REQ-IDX-*`·`NFR-IDX-001`(`R-23`) / `REQ-DIC-003`(`R-24`) / `REQ-DIC-004` 비고에 「구현 자리는 사전 초안 교정」 명시(`R-11`) / 표 오류 정리(`X-17`) |
| `docs/UBIQUITOUS_LANGUAGE.md` | 4절 「미갱신/outdated」 삭제 + 「정렬됨/aligned」 등재(`D-31`) / 4·5절 「대조 진입점이 둘」 수정(`R-2`) / 6절 「동의어편입 대상」 설명을 통합 모델에 맞춰 정정(`X-03`) |
| `docs/ARCHITECTURE.md` | «크로스 도메인 조회 — 포트와 어댑터»에 **접근 검증 예외 조항 추가**(`D-19`) |
| `docs/TEST.md` | «DB 테스트 독립 환경 설정»의 `@Transactional` 롤백 권장 정정(`D-32`) |
| `docs/API.md` | Document API «알아 둘 것 셋» ②·③ 전면 수정(`R-6`·`R-18`) / 본문 편집 엔드포인트 추가(`G-9`) / 응답에서 `outdated` 제거·`aligned`·`edited` 추가 / **Dictionary API 절 신설**(`Y-04`) / 공통 규칙의 `INVALID_INPUT` 문구 정정(`X-09`) / `ErrorResponse`에 `traceId` 추가 여부 반영(`X-08`) |
| `docs/plan/EXECUTION_ORDER.md` | 6개 도메인 기준으로 확장 — 태스크 ID 매핑, 의존 그래프, `T-CMN-1` 신설, `T-INT-4` 미신설, `refacotr/WLSH-86` 조항 제거(`Y-21`) |
| `CLAUDE.md`(루트) | 문서 맵에 신규 4개 추가 / «미구성 항목»의 **메시징 항목 해소**(`D-24`) |
| `backend/CLAUDE.md` | 기술 스택의 메시징 줄을 「로컬·테스트 인메모리 / AWS 배포 SQS」로(`D-24`) |

### 손대지 않는 파일

`docs/plan/DRAFT_DOCUMENT_PLAN.md`, `docs/plan/DRAFT_DICTIONARY_PLAN.md`, `docs/plan/REVIEW_REQUEST_PLAN.md`.

`R-8`~`R-14`·`R-19`·`Y-21`이 이 문서들을 향하지만 **보존이 원칙**이다. 각 도메인 세션은 이 문서(`CONFLICTS.md`)를 함께 읽고, 자기 계획 문서의 서술과 여기의 `R-*`가 어긋나면 **`R-*`를 따른다.**

---

## 10. 남은 결정 대기

**없다.** 2026-09-10에 전건 확정했고, 2026-09-12에 드러난 4건도 `D-33`~`D-36`으로 확정했다.

`상태` 칸이 `제안`인 항목(`X-04`·`X-05`·`X-08`~`X-14`·`X-17`, `Y-22`)과 `기록`인 항목(`Y-24`~`Y-27`)은 **결정이 필요한 것이 아니라 담당 태스크에서 형태를 정하는 것**이다. 설계 방향은 이미 정해져 있다.

**`D-35`의 실제 LLM 연동만 예외다** — 새 의존성·API 키·프롬프트 설계가 필요하므로 루트 `CLAUDE.md`의 「새 라이브러리·의존성 추가는 사전에 제안하고 승인받는다」에 따라 그 시점에 별도로 합의한다.

새 결정이 필요해지면 루트 `CLAUDE.md`에 따라 **임의로 확정하지 않고 질문한 뒤** 이 문서에 먼저 반영한다. ID는 `D-38`부터 붙인다.
