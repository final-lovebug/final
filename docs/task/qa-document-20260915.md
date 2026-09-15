# 문서 도메인 QA 리포트 (2026-09-15)

브랜치 `feat/WLSH-178-document-fix` 기준. 문서(Document)·문서 초안(DraftDocument) 영역 전체를 대상으로 돌린 결과다.

## 결론

**247건 중 246건 통과, 1건 실패.** 실패 1건은 이 브랜치의 변경과 무관한 **기존 실패**이며, 원인과 조치안은 아래 3절에 있다.

---

## 1. 방법과 한계

애초 계획은 브라우저 확장으로 화면을 직접 클릭하는 QA였으나 **확장을 쓸 수 없어 방식을 바꿨다.** 대신 아래 셋을 돌렸다.

| 계층 | 수단 |
| --- | --- |
| 백엔드 | JUnit — `document.*`, `draftdocument.*`, `scenario.*` (Testcontainers로 MySQL·Redis·LocalStack 기동) |
| AI 워커 | pytest + ruff (`ubidicExtractor/ubidict-py`) |
| 프론트엔드 | `tsc -b`, `oxlint` |
| 실행 중인 스택 | API 인증 경계 probe, 워커 목 대조 dry-run, DB 시드 확인 |

**덮지 못한 것을 먼저 적는다.**

- **화면 조작 QA를 하지 못했다.** 버튼 클릭·화면 전환·하이라이트 렌더링 같은 실제 UI 회귀는 이 리포트가 보장하지 않는다. 프론트는 타입·린트 수준까지만 검증됐다.
- **프론트엔드에 테스트 러너가 없다**(`package.json`에 vitest·jest 없음). 이번 브랜치에서 고친 사전집 버전 표시·줄바꿈·라우트 이름 변경은 **자동 회귀 테스트가 붙지 않은 상태**다.
- **인증이 필요한 API를 실제 호출로 검증하지 못했다.** 실행 중인 백엔드가 `dev` 프로파일만 활성이라 `local` 프로파일의 개발용 로그인(`/api/auth/dev/login`)이 없고, 토큰을 얻으려면 Google 로그인을 실제로 통과해야 한다.

---

## 2. 결과 요약

| 대상 | 통과 | 실패 | 비고 |
| --- | --- | --- | --- |
| 백엔드 document·draftdocument·scenario | 246 | **1** | 클래스 48개 / 테스트 247건 |
| AI 워커 pytest | 35 | 0 | |
| AI 워커 ruff | 통과 | — | |
| 프론트 `tsc -b` | 통과 | — | |
| 프론트 `oxlint` | 경고 1 | — | 기존 건(`OAuthCallbackPage.tsx:37`), 이번 변경과 무관 |

### 백엔드 상세

| 묶음 | 클래스 | 테스트 |
| --- | ---: | ---: |
| `document.domain` | 5 | 39 |
| `document.implement` | 3 | 10 |
| `document.infra` | 6 | 28 |
| `document.presentation` | 2 | 17 |
| `document.service` | 1 | 33 |
| `draftdocument.domain` | 3 | 21 |
| `draftdocument.implement` | 7 | 13 |
| `draftdocument.infra` | 5 | 17 |
| `draftdocument.presentation` | 4 | 25 |
| `draftdocument.service` | 11 | 40 |
| `scenario` | 1 | 4 |
| **합계** | **48** | **247** |

> 스킵된 테스트는 없다.

---

## 3. 실패 1건

### `UbiquitousLanguageLifecycleTest.firstDictionaryIsBornFromDocuments`

> 문서를 올려 용어를 추출하고 리뷰를 통과시키면 첫 사전집이 태어난다.

```
Expecting actual:
  ["결제", "결제수단", "주문"]
to contain exactly (and in same order):
  ["결제수단"]
but some elements were not expected:
  ["결제", "주문"]
```

`UbiquitousLanguageLifecycleTest.java:165`

**원인 — 인프로세스 추출 대역이 고정 후보어를 돌려주게 바뀌었는데 기대값이 따라가지 않았다.**

`InProcessExtractionWorker`가 `결제`·`주문` 두 건을 돌려준다(`:52`, `:60`). 테스트는 손으로 등록한 `결제수단` 하나만 실릴 것을 기대한다. 흥미롭게도 **테스트 주석이 이 상황을 미리 적어 두었다**:

> 인프로세스 대역은 추출 결과를 빈 목록으로 돌려주므로(D-74) 손으로 등록한 하나만 남는다 — **대역이 후보어를 돌려주게 되면 그것들도 함께 실리며 이 기대값이 늘어난다.**

즉 대역의 동작이 바뀐 시점(`c9b4f96 feat: align LLM worker mock contract (WLSH-176)`)에 기대값을 함께 올렸어야 했다.

**이 브랜치의 변경과 무관하다.** 작업 트리에 백엔드 변경이 없고(`git diff HEAD -- backend/` 비어 있음), 이번 브랜치가 건드린 것은 프론트엔드·워커·compose·문서다.

**조치안** — 기대값을 `["결제", "결제수단", "주문"]`으로 올린다. 정렬 순서가 보장되는지 확인이 필요하면 `containsExactlyInAnyOrder`를 쓴다. **테스트를 지우거나 단언을 느슨하게 만들지 않는다** — 대역이 후보어를 싣는다는 사실 자체가 검증 대상이다. 담당자 배정이 필요하다.

---

## 4. 실행 중인 스택 점검

### 인증 경계

| 경로 | 응답 | 판정 |
| --- | ---: | --- |
| `GET /api/workspaces/1/documents` | 401 | 정상 |
| `GET /api/draft-documents?status=EXAMINING` | 401 | 정상 |
| `GET /api/draft-documents/checks/1` | 401 | 정상 |
| `GET /api/review-requests?workspaceId=1&type=DOCUMENT` | 401 | 정상 |
| `POST /api/internal/llm/checks/999/result` | 400 | **설계대로** — 인증 필터는 통과하고 `requestId` 대조가 막는다(`D-70`) |

> 마지막 줄은 취약점이 아니라 알려진 설계다. 다만 `docs/AI_CONTRACT.md` 4-2가 경고하듯 **배포 시 보안 그룹·인그레스로 이 경로를 워커 출발지로 제한하지 않으면 `NFR-AI-002`는 미충족**이다.

### 목 대조 왕복

`app.ai.mode=mock`에서 워커가 본문을 읽어 앵커를 만든다. 시드 문서(id 9)로 확인했다.

| originTerm | suggestionTerm | anchor | 본문 구간 일치 |
| --- | --- | --- | --- |
| 유저 | 이용자 | 0–2 | ✔ |
| 고객 | 회원 | 30–32 | ✔ |
| 페이먼트 | 결제 | 18–22 | ✔ |

세 건 모두 `body.substring(anchor) == originTerm`을 만족해 `CheckSuggestionValidator`를 통과한다.

### 컨테이너

`mysql`·`redis`·`localstack`·`grafana-lgtm`·`frontend`·`llm-worker` 모두 healthy. 백엔드는 컨테이너가 아니라 호스트에서 `bootRun`으로 8080을 점유하고 있다(루트 `compose.yaml`의 `backend` 서비스는 주석 상태).

---

## 5. 후속 과제

| # | 내용 | 근거 |
| --- | --- | --- |
| 1 | `firstDictionaryIsBornFromDocuments` 기대값 갱신 | 3절 |
| 2 | 프론트엔드 테스트 러너 도입 검토 | 1절 — 이번 브랜치 수정 3건에 회귀 테스트가 없다 |
| 3 | 화면 조작 QA 별도 수행 | 1절 — 자동화가 덮지 못한 영역 |
| 4 | `/api/internal/**` 인그레스 제한 | 4절 |

---

## 6. 추록 — `develop` 병합 이후 (같은 날 저녁)

위 결과는 **`develop` 병합 전** 기준이다. 병합하면서 전제가 셋 바뀌었다.

| 항목 | 리포트 작성 시점 | 지금 |
| --- | --- | --- |
| 테스트 DB | Testcontainers MySQL | **H2**(`MODE=MySQL`) + collation 검증만 MySQL 컨테이너 |
| 실패 1건(`firstDictionaryIsBornFromDocuments`) | 미해소 | **해소** — `df6c16a`가 기대값을 `["결제", "결제수단", "주문"]`으로 올렸다 |
| 백엔드 전체 | 측정 안 함 | **982건 통과 / 실패 0 / 스킵 20** |

병합이 새로 드러낸 것도 있다. develop이 테스트 DB를 H2로 바꾸면서 **라벨 대소문자 정합(`D-94`) 검증 7건이 깨졌다** — H2의 MySQL 모드는 문법만 흉내 낼 뿐 `utf8mb4_0900_ai_ci`를 따라 하지 않는다. collation이 판정 주체인 테스트만 실제 MySQL 컨테이너로 되돌려 해결했다(`MySqlContainerConfiguration`, `99cf4dc`).

**스킵 20건**은 develop이 `@Disabled`로 둔 것들이다 — SQS 왕복(`DraftDocumentCheckWorkerRoundTripTest` 등 11건)과 Redis 저장소 테스트 9건. 대조·추출의 큐 왕복 검증이 비활성이라는 뜻이므로 5절의 후속 과제에 함께 둔다.

1절의 한계(화면 조작 QA 미수행, 프론트엔드 테스트 러너 부재)는 그대로다.
