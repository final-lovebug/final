# 6개 도메인 실행 순서와 세션 운영 규약

여섯 도메인 계획 문서를 **여러 세션·여러 사람이 나눠 구현할 때** 지키는 규약이다.

- `WORKSPACE_PLAN.md` · `DOCUMENT_PLAN.md` · `DICTIONARY_PLAN.md` — 기구현 도메인(as-built + 재설계)
- `DRAFT_DOCUMENT_PLAN.md` · `DRAFT_DICTIONARY_PLAN.md` · `REVIEW_REQUEST_PLAN.md` — 미구현 도메인

각 문서의 12절이 도메인 안의 태스크 순서를 정하고, 이 문서는 **도메인을 가로지르는 순서와 공유 파일의 주인**을 정한다. 계획 문서가 **병렬 작업을 전제로 쓰였기 때문에** 이 규약이 필요하다. 한 세션이 자기 문서만 보고 진행하면 다른 세션과 같은 파일을 건드리거나, 아직 없는 산출물을 전제하게 된다.

**`CONFLICTS.md`를 함께 읽는다.** 큰 흐름 확정(`G-*`)·이번 세션 확정(`D-19`~`D-32`)·뒤집힌 결정(`R-*`)이 거기에 있다. **초안·리뷰 3개 문서의 서술과 `R-*`가 어긋나면 `R-*`를 따른다** — 그 문서들은 보존 대상이라 갱신되지 않는다.

---

## 0. 태스크 ID 접두사

| 접두사 | 도메인 | 계획 문서 |
| --- | --- | --- |
| `WS-` | Workspace | `WORKSPACE_PLAN.md` |
| `DOC-` | Document | `DOCUMENT_PLAN.md` |
| `DIC-` | **Dictionary(사전집)** | `DICTIONARY_PLAN.md` |
| `DD-` | DraftDocument(문서 초안) | `DRAFT_DOCUMENT_PLAN.md` |
| `DI-` | **DraftDictionary(사전 초안)** | `DRAFT_DICTIONARY_PLAN.md` |
| `RR-` | ReviewRequest | `REVIEW_REQUEST_PLAN.md` |
| `T-` | 통합·공통 | 이 문서 6절 |

> **`DI-`와 `DIC-`를 혼동하지 않는다.** `DI-`는 사전 **초안**(DraftDictionary), `DIC-`는 **사전집**(Dictionary)이다. 세션 지시문에 태스크 ID를 적을 때 한 글자 차이로 다른 도메인을 구현하게 된다.

**결정 ID**는 `D-1`~`D-18`(초안·리뷰 3개 문서 전역 공유), `G-1`~`G-15`(큰 흐름), `D-19`~`D-32`(이번 세션)를 쓴다. **새 결정은 `D-33`부터**다.

---

## 1. 태스크 의존 그래프

여섯 문서 12절의 의존 칸을 한데 모은 것이다. **`**`가 붙은 것이 도메인을 가로지르는 의존이다.**

### 선행 태스크

| 태스크 | 의존 |
| --- | --- |
| **T-DOC-1** 문서 선행 수정 | — (**모든 구현의 선행**) |
| **T-CMN-1** `PageResponse`·`PageResult` | `T-DOC-1` |

### 기구현 도메인

| 태스크 | 의존 |
| --- | --- |
| **WS-4** 룰셋 수정 + 상한 검증 + 제공 어댑터 | — |
| **WS-1** 참여자 목록·권한 변경 + 감사 로그 | — |
| WS-2 내보내기 + 소유권 이전 | WS-1 |
| WS-3 초대 | WS-1 |
| WS-5 삭제·이탈 이벤트 | WS-2 |
| WS-6 패키지 규약 + ID 값 객체 제거 | WS-3 |
| **DOC-1** 사전집 활성 버전 조회 배선 | — |
| DOC-2 본문 직접 편집 + `edited` + `V210` | DOC-1 |
| DOC-3 `outdated` 제거·`aligned` 통합 | DOC-1, DOC-2 |
| DOC-4 제공 포트(추출 대상·문서 스냅샷) | DOC-3 |
| DOC-5 편집 차단 검증 | DOC-2, **DD-1**, **DI-1** |
| DOC-6 교정 반영 발행 어댑터 | DOC-2, **RR-4b** |
| DOC-7 목록·버전 이력 페이징 | **T-CMN-1** |
| DOC-8 ID 값 객체 제거 | — |
| DOC-9 감사 로그 | DOC-2 |
| DOC-10 삭제·편집 이벤트 | DOC-2 |
| DIC-1 `API.md` 절 신설 + 임시 API 태그 | **T-DOC-1** |
| **DIC-2** 감사 상위 클래스 정합 + `V310` | — |
| DIC-3 발행 위임 어댑터 | DIC-2, **RR-4b**(포트 정의) |
| DIC-4 표준어·정의 스냅샷 조회 어댑터 | DIC-2 |
| DIC-5 활성 버전 조회 어댑터 | DIC-2 |
| DIC-6 조회·검색·정렬 + 페이징 | **T-CMN-1**, DIC-2 |
| DIC-7 임시 API 제거 | **RR-4b**, DIC-3 |
| DIC-8 이벤트 + 감사 로그 | DIC-3 |
| DIC-9 ID 값 객체 제거 | — |

### 미구현 도메인

| 태스크 | 의존 |
| --- | --- |
| **RR-1** 루트 CRUD + 취소 | — |
| **DD-1** 루트 CRUD | — |
| **DI-1** 루트 CRUD | **DIC-4**(통합 입력 — 스텁이 빈 목록을 주면 「사전집 없는 첫 회차」만 표현된다) |
| RR-2a 리뷰어 지정 | RR-1 |
| RR-2b 문서 개정안 | RR-1, **DD-1** |
| RR-2c 사전 개정안 | RR-1, **DI-1** |
| RR-2d 목록 조회 | RR-1, **T-CMN-1** |
| DD-2 제안어 CRUD + 목록 | DD-1, **DIC-4**(`suggestionTerm` 출처), **T-CMN-1** |
| DI-2 후보어 CRUD + 목록 | DI-1, **T-CMN-1** |
| RR-3a 리뷰 제출 | RR-2a |
| RR-3b 코멘트 | RR-3a, **DD-2**(`TextRange`) |
| **RR-3c** 상태 전이 + 발행 판정 | RR-3a, **WS-4**(룰셋 정족수) |
| DD-3 교정 흐름 | DD-2 |
| DI-3 판정 + 리뷰 요청 | DI-2 |
| RR-4a 재교정 | RR-3c |
| **RR-4b** 발행 | RR-3c |
| RR-4c 인가 + 낙관적 락 | RR-4a, RR-4b |
| RR-4d 이벤트 | RR-4a, RR-4b |
| DD-4 정책 + 이벤트 | DD-3, **RR-1**, **DOC-4** |
| DI-4 정책 + 인가 + 이벤트 | DI-3, **RR-2c**, **DOC-4** |
| T-INT-1 Flyway out-of-order 정리 | 6개 도메인 Phase 1 |
| T-INT-2 크로스 도메인 어댑터 `real` 전환 | 6개 도메인 Phase 3 |
| T-INT-3 `SecurityConfig` + 인증 주체 + 프로파일 분리 | 인증 도메인(별건) |

> **`DI-4`의 의존을 `RR-2c`로 정확히 적는다.** `DRAFT_DICTIONARY_PLAN.md` 12절은 `RR-2`로 적었으나 그것은 Phase 표기이고 태스크가 아니다. 「사전집에 초안 또는 개정안이 존재하면 추가 초안 생성 불가」 정책이 `RevisionDictionary.dictionaryId`로 진행 중인 개정안을 찾으므로 **사전 개정안을 만드는 `RR-2c`**가 실제 선행이다.

**`T-INT-4`는 신설하지 않는다** — `D-26`(유래 리비전을 두지 않는다)으로 필요가 사라졌다.

> **발행 포트의 의존은 한 방향이다.** `RR-4b`가 `DocumentVersionPublishPort`·`DictionaryVersionPublishPort`를 **정의하고 스텁까지 만들어** 자기 태스크를 완결한다(`docs/ARCHITECTURE.md` 「제공 도메인이 아직 없으면 스텁을 함께 만든다」). 그 뒤 제공 도메인이 real 어댑터로 갈아끼운다 — `DIC-3`·`DOC-6`이 `RR-4b`를 기다리고, **`RR-4b`는 둘을 기다리지 않는다.** 반대로 읽으면 순환이 생긴다.

### 크리티컬 패스가 둘로 늘었다

기존에는 ReviewRequest 하나였으나 **`WS-4`와 `DIC-3`이 새 병목이다.**

- **`WS-4` → `RR-3c` → `RR-4b`** — `G-4`(승인 집계는 룰셋 정족수)로 룰셋 수정이 발행 판정의 선행이 됐다. `RuleSet`에 변경 메서드가 없어 **정족수가 영구히 0**이므로(`Y-14`), `WS-4` 없이는 발행 조건의 「1 이상」 경로를 한 번도 타지 못한다.
- **`DIC-2` → `DIC-4` → `DI-1`·`DD-2`** — `G-1`(통합 모델)로 사전 초안이 이전 사전집을 입력으로 받게 되면서 `DIC-4`가 `DI-1`의 선행이 됐다. `DD-2`도 `suggestionTerm` 값의 출처로 같은 포트를 쓴다(`O-3`).
- **`RR-1`이 `DD-4`를, `RR-2`가 `DI-4`를 막는다** — 기존 그래프 그대로다.

---

## 2. 순차 실행 순서 (권장)

세션을 하나씩 돌린다면 아래 순서를 따른다. 이 순서면 3절 공유 파일 경합이 **자연히 사라진다** — 같은 시점에 두 세션이 같은 파일을 건드리지 않는다.

```
 0. T-DOC-1                          ← 문서 기준을 먼저 맞춘다. 모든 구현의 선행
 1. T-CMN-1                          ← PageResponse·PageResult를 여기서 만든다
 2. WS-4                             ← 룰셋 수정. RR-3c·RR-4b가 이것을 기다린다
 3. DIC-2  →  DIC-1                  ← 감사 엔티티 정합, API.md 절 신설
 4. DOC-1  →  DOC-2  →  DOC-3        ← aligned 판정을 실제로 동작하게 만든다 (한 PR)
 5. DIC-4  →  DIC-5  →  DOC-4        ← 도메인 간 조회 창구
 6. RR-1                             ← 리뷰 요청 루트. DD-4·DI-4를 푼다
 7. DD-1  →  DI-1
 8. RR-2b  →  RR-2c                  ← DD-1·DI-1의 초안 식별자가 필요하다
 9. DD-2                             ← common/domain/TextRange를 여기서 만든다
10. RR-2a  →  RR-2d  →  DI-2
11. RR-3a  →  RR-3b  →  RR-3c        ← WS-4가 2번에서 끝나 있어야 발행 판정을 검증할 수 있다
12. DD-3  →  DI-3
13. RR-4a  →  RR-4b  →  DIC-3  →  DOC-6     ← RR-4b가 포트·스텁을 만들고, 제공 도메인이 real 어댑터로 갈아끼운다
14. RR-4c  →  RR-4d
15. DD-4  →  DI-4  →  DOC-5
16. WS-1  →  WS-2  →  WS-3  →  WS-5  →  WS-6
17. DIC-6  →  DIC-7  →  DIC-8  →  DIC-9
18. DOC-7  →  DOC-8  →  DOC-9  →  DOC-10
19. T-INT-1  →  T-INT-2  →  T-INT-3
```

**0번을 건너뛰지 않는다.** `R-1`~`R-24`가 `DOMAIN.md`·`REQUIREMENTS.md`·`ARCHITECTURE.md`·`UBIQUITOUS_LANGUAGE.md`·`TEST.md`·`API.md`를 고쳐야 한다. 문서를 그대로 두고 구현하면 **세션이 낡은 기준을 읽고 뒤집힌 결정을 되살린다** — 루트 `CLAUDE.md`의 "결정이 바뀌면 코드보다 문서를 먼저 갱신한다"가 이 순서의 근거다.

**1번을 앞당기지 않는다** — `PageResponse`·`PageResult`가 여기서 처음 만들어지므로 `DOC-7`·`DIC-6`·`DD-2`·`DI-2`·`RR-2d`가 그 뒤에 온다. **원래 `DD-2` 소유였던 것을 선행 태스크로 옮긴 이유**는 기구현 3개의 페이징(`Y-05`)이 `DD-2`보다 먼저 필요해졌기 때문이다.

**2번을 미루지 않는다.** `WS-4`가 11번(`RR-3c`)보다 늦으면 정족수 0인 채로 발행 판정을 만들게 되고, 룰셋이 들어올 때 「1 이상」 경로가 처음 실행된다.

**4번은 한 PR로 묶는다**(`DOC-1`~`DOC-3`). `aligned`를 넣으려면 활성 사전집 버전을 읽을 수 있어야 하고 조건에 `edited`가 들어가므로, 나누어 머지하면 중간 상태에서 응답 필드가 두 번 바뀐다. **커밋은 태스크별로 나눈다.**

**9번을 앞당기지 않는다** — `common/domain/TextRange`가 여기서 처음 만들어지므로 `RR-3b`가 그 뒤에 온다.

**15번의 `DOC-5`가 초안 도메인 뒤에 있는 이유** — real 어댑터를 만들려면 `DD-1`·`DI-1`이 develop에 있어야 한다. 그 전에는 스텁이 `false`를 반환해 편집이 항상 허용된다(as-built와 같은 동작).

**16번 이후는 병목이 아니다.** 참여자 관리·검색·페이징·로그·이벤트는 다른 도메인을 막지 않으므로 여유 있는 세션에 배치한다. 단 **`WS-4`는 2번에 있고 나머지 `WS-*`만 16번**이다.

`T-INT-1`(`spring.flyway.out-of-order=true`)은 **6개 도메인 Phase 1이 모두 머지된 뒤** 한다. 대역이 100·200·300·400·500·600으로 나뉘어 있고 `workspace`가 `V2__`로 대역 밖이므로(`Y-10`), 머지 순서가 번호 순서와 어긋나면 Flyway가 거부한다.

---

## 3. 공유 파일 주인

**"선착순"으로 두지 않고 주인을 못박는다.** 세션은 서로를 볼 수 없으므로 선착순은 곧 중복 생성이다.

| 파일 | 주인 | 규약 |
| --- | --- | --- |
| `docs/DOMAIN.md`, `docs/REQUIREMENTS.md`, `docs/ARCHITECTURE.md`, `docs/UBIQUITOUS_LANGUAGE.md`, `docs/TEST.md` | **`T-DOC-1`만** | `R-1`~`R-24`·`D-19`~`D-32`가 요구하는 수정을 **그 태스크 하나가 한 커밋으로** 한다. `CONFLICTS.md` 9절이 파일별 목록을 갖는다. 다른 태스크는 이 파일들을 읽기만 한다 |
| `common/presentation/PageResponse`, `common/service/PageResult` | **`T-CMN-1`** | 규격은 `docs/API.md` «페이징·정렬 규격»을 그대로 따른다. 만든 즉시 develop에 올린다. `DOC-7`·`DIC-6`·`DD-2`·`DI-2`·`RR-2d`는 이것을 **쓴다** |
| `common/domain/TextRange` | **DraftDocument (`DD-2`)** | `@Embeddable` record. 필드 `startOffset`/`endOffset`, 컬럼 `start_offset`/`end_offset`, 생성자에서 `startOffset <= endOffset`·음수 아님 검증. 만든 즉시 develop에 올린다. `RR-3b`는 **직접 만들지 않고 이것을 쓴다** |
| `workspace/implement/WorkspaceAccessValidator` | **Workspace** | **5개 도메인이 직접 주입한다**(`D-19`). 시그니처를 바꾸면 전부 깨진다 — 변경은 통합 태스크로 넘기고 PR에 영향 범위를 적는다 |
| `workspace/domain/Permission` | **Workspace** | 다른 도메인이 `Permission.ADMIN`을 인자로 넘긴다. 상수를 지우거나 이름을 바꾸지 않는다 |
| `docs/API.md` | **도메인별 자기 절만** | 각 도메인은 파일 **끝에 자기 `# **{도메인} API**` 절을 추가**한다. Workspace 절의 골격(도입 문단 → 요약 표 → 엔드포인트 절 → 에러 표)을 따른다. **공통 규칙·페이징·버저닝·에러 응답 형식 절은 건드리지 않는다** — 그 절들은 `T-DOC-1`이 고친다 |
| Flyway 마이그레이션 | 대역으로 분리 | member 1–99 / **workspace 100–199** / **document 200–299** / **dictionary 300–399** / DraftDocument 400–499 / DraftDictionary 500–599 / ReviewRequest 600–699 / 공통·사후 정리 900–999. 도메인 내부는 10 단위로 증가시킨다 |

**기존 마이그레이션 파일은 고치지 않는다.** `V1`(member)·`V2`(workspace)·`V200`·`V201`(document)·`V300`(dictionary)은 이미 머지됐고, 파일명이나 내용을 바꾸면 Flyway 체크섬이 어긋난다. `V2`가 대역 밖인 것은 `T-INT-1`이 `out-of-order`로 덮는다.

### 아무도 수정하지 않는 파일

`common/**`(위 3건 제외), `common/domain/BaseEntity`, `backend/src/test/java/.../support/**`, `application.properties`, `build.gradle`, 그리고 **다른 도메인의 패키지 전체**.

필요하면 통합 태스크(`T-*`)로 넘기고 PR에 이유를 적는다.

> **어댑터를 만들 때도 다른 도메인의 파일을 고치지 않는다.** 어댑터는 `{소비도메인}/infra/adapter/`에 두고 제공 도메인의 `infra`(Repository)만 참조한다 — `docs/ARCHITECTURE.md`의 «크로스 도메인 조회 — 포트와 어댑터».
>
> **단 접근 검증은 예외다**(`D-19`). `WorkspaceAccessValidator`를 직접 주입하며, 이것은 `workspace` 패키지를 **참조**하는 것이고 **수정**하는 것이 아니다.

### `docs/plan/`의 기존 3개 문서는 아무도 고치지 않는다

`DRAFT_DOCUMENT_PLAN.md`·`DRAFT_DICTIONARY_PLAN.md`·`REVIEW_REQUEST_PLAN.md`는 **보존 대상**이다. `R-8`~`R-14`·`R-19`·`Y-21`이 그 문서들을 향하지만, `CONFLICTS.md`가 ID로 지목하고 각 세션이 그것을 함께 읽는다.

**세션이 이 문서들의 서술과 `R-*`가 어긋나는 것을 발견하면 `R-*`를 따르고 `CONFLICTS.md`에 근거를 확인한다.** 문서를 고치지 않는다.

---

## 4. 동시 실행 제약

worktree는 워킹 디렉터리만 분리한다. **`.git`·Docker·호스트 포트는 공유된다.**

| 제약 | 이유 |
| --- | --- |
| **`bootRun`은 한 번에 한 worktree만** | `spring-boot-docker-compose`가 worktree마다 별도 Compose 프로젝트(디렉터리명 기준)를 띄운다. MySQL·MongoDB·Redis·LGTM이 배로 뜨고 호스트 포트 8080·3000이 겹쳐 `port is already allocated`로 죽는다 |
| **`./gradlew test`를 여러 worktree에서 동시에 돌리지 않는다** | `RepositoryTestSupport`가 MySQL 컨테이너를, `IntegrationTestSupport`가 `TestcontainersConfiguration`(MySQL+Mongo+Redis+LGTM)을 띄운다. worktree 수만큼 Gradle 데몬도 늘어난다. LGTM과 빌드 JVM이 겹치면 Docker Desktop 기본 메모리에서 OOM으로 죽는다 |
| develop 동기화 | **`T-DOC-1` 머지 직후 즉시**(문서 기준이 바뀐다), `T-CMN-1` 머지 직후 즉시(`PageResponse`가 생긴다), `WS-4` 머지 직후(정족수를 쓸 수 있게 된다), 공유 자산이 올라온 직후, 다른 도메인 Phase 1이 머지된 직후 |

**병렬로 진행하려면** 3절 주인 규약을 지키고 위 두 제약을 사람이 조율해야 한다. 조율이 어렵다면 2절 순차 순서를 쓴다.

**병렬로 안전한 조합** — 서로 다른 도메인의 Phase 1이면서 공유 자산을 만들지 않는 것들이다.

```
WS-4  ·  DIC-2  ·  DOC-8  ·  DIC-9        ← 파일이 겹치지 않는다
WS-1  ·  DOC-9  ·  DIC-8                  ← 각 도메인의 로그·이벤트
```

**병렬로 위험한 조합**

```
DOC-1 · DOC-2 · DOC-3    ← 같은 응답 DTO를 연달아 고친다. 한 PR로 묶는다
DD-2 · RR-3b             ← TextRange 소유권. DD-2가 먼저다
DOC-7 · DIC-6 · DI-2     ← PageResponse 소유권. T-CMN-1이 먼저다
WS-6 · 다른 모든 WS-*    ← 디렉터리 이동이 앞선 태스크의 파일을 전부 건드린다
```

---

## 5. 커밋 단위와 브랜치

### 커밋 단위는 **태스크 하나**다

Phase가 아니라 태스크(`WS-4`, `DOC-2`, `RR-1`)를 단위로 삼는다. DraftDocument·DraftDictionary는 Phase와 태스크가 1:1이라 차이가 없지만, **ReviewRequest는 Phase 2가 2a·2b·2c·2d, Phase 4가 4a·4b·4c·4d로 갈리고 Document는 한 Phase에 여러 태스크가 있다.** Phase 단위로 묶으면 한 커밋에 포트·스텁·이벤트 record·마이그레이션이 뒤섞인다.

각 커밋은 **해당 태스크의 DoD를 만족한 상태**여야 한다. 각 문서 12절 «Phase별 DoD»의 공통 항목 + 태스크 항목을 함께 본다.

**한 PR에 여러 태스크를 담는 경우가 하나 있다** — `DOC-1`~`DOC-3`이다(2절 4번). 커밋은 셋으로 나누고 PR만 하나로 묶는다.

### 브랜치명

`feat/WLSH-{티켓번호}-{도메인}` — 예: `feat/WLSH-101-document`, `feat/WLSH-102-workspace`.

`.githooks/prepare-commit-msg`가 브랜치명에서 `[A-Z]+-[0-9]+`를 뽑아 커밋 메시지에 삽입한다. **패턴이 없으면 티켓 추적이 끊긴다.** 세션을 시작할 때 티켓 번호를 함께 알려줘야 한다.

`T-*` 태스크는 도메인이 없으므로 `chore/WLSH-{티켓번호}-{태스크}` — 예: `chore/WLSH-100-doc-baseline`(`T-DOC-1`), `chore/WLSH-103-page-response`(`T-CMN-1`).

**`T-DOC-1`은 문서만 담는다.** 코드 변경을 섞으면 "문서를 먼저 갱신한다"는 규약이 의미를 잃는다.

---

## 6. 세션 시작 지시문 템플릿

계획 문서에 Phase 경계·DoD·필수 테스트 케이스가 다 들어 있으므로 지시문은 짧아도 된다. **범위를 태스크 ID로 지목**하는 것이 핵심이다.

```
docs/plan/EXECUTION_ORDER.md와 docs/plan/CONFLICTS.md,
docs/plan/DOCUMENT_PLAN.md를 읽고 DOC-1만 구현해줘.

- 12절 DoD(공통 + DOC-1)를 전부 만족시킨다
- 11절 필수 케이스를 테스트로 쓴다
- ./gradlew spotlessApply && ./gradlew check 초록 확인 후 커밋한다
- 브랜치: feat/WLSH-<티켓번호>-document
```

**`CONFLICTS.md`를 반드시 함께 읽힌다.** 뒤집힌 결정(`R-*`)이 거기에만 있고, 초안·리뷰 3개 문서는 낡은 서술을 그대로 갖고 있다.

지시문에 다시 적지 않아도 되는 것들 — 문서와 `CLAUDE.md`에 이미 있다.

- 레이어 규칙, 허용 애노테이션, 이벤트 페이로드 규약, **접근 검증 예외**(`D-19`) → `docs/ARCHITECTURE.md`
- 테스트 계층별 방식, `@Transactional` 금지, AssertJ, Fixture 형태 → `docs/TEST.md` + 각 문서 11절
- 로그 prefix, 예외 코드 형식, API 규격 → `docs/LOG.md`·`docs/EXCEPTION.md`·`docs/API.md`
- 수정 금지 파일 → 이 문서 3절
- 마이그레이션 대역 → `backend/CLAUDE.md` + 이 문서 3절
- 확정된 결정과 뒤집힌 결정 → `docs/plan/CONFLICTS.md`

---

## 7. 세션이 멈춰야 하는 경우

아래를 만나면 **임의로 결정하지 않고 보고한다**(루트 `CLAUDE.md`).

- 계획 문서에 없는 설계 결정이 필요해졌다 → **`CONFLICTS.md`에 먼저 적고** 합의한 뒤 구현한다. ID는 `D-33`부터
- 3절 «아무도 수정하지 않는 파일»을 고쳐야 한다 → 통합 태스크로 넘긴다
- `docs/DOMAIN.md`·`REQUIREMENTS.md`·`ARCHITECTURE.md`·`UBIQUITOUS_LANGUAGE.md`·`TEST.md`를 고쳐야 한다 → **`T-DOC-1`으로 넘긴다.** 이미 머지됐다면 그 변경만 담은 커밋을 따로 만든다
- 초안·리뷰 3개 계획 문서의 서술이 `CONFLICTS.md`의 `R-*`와 어긋난다 → **`R-*`를 따르고 문서는 고치지 않는다.** 어긋남이 `CONFLICTS.md`에 없으면 보고한다
- 의존 태스크가 아직 develop에 없다 → 1절 그래프를 확인하고 순서를 조정한다
- 테스트가 실패한다 → **테스트를 삭제하거나 조건을 완화하지 말고** 원인을 보고한다
- 새 라이브러리·의존성이 필요하다 → 사전에 제안하고 승인받는다
