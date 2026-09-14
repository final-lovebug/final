# **QA 시나리오**

이 문서는 `docs/REQUIREMENTS.md`에서 **진행 상태가 `완료`인 요구사항**이 실제로 동작하는지 손으로 확인하는 표다. 사람이 위에서 아래로 훑어도 되고, **Claude Chrome 플러그인이 브라우저를 몰아 그대로 실행해도 되도록** 절차를 확정적으로 적는다.

`docs/TEST.md`(자동 테스트)와 역할이 갈린다. **단위·서비스·리포지토리 로직은 여기서 반복하지 않는다** — 자동 테스트가 이미 덮는다. 이 표가 맡는 것은 테스트가 덜 닿는 구간이다. 화면에 실제로 무엇이 보이는지, 인증 왕복이 브라우저에서 끊기지 않는지, 권한이 없는 사람에게 정말로 막히는지.

> **실행 상태 — 아직 실행하지 않는다.**
> 이 표는 **프런트가 백엔드에 실 연동되기 전에 미리 작성**했다. 현재 프런트는 `features/{domain}/api` 계층이 목데이터를 돌려주는 1차 구축 상태이며(`frontend/docs/SPEC.md`), 화면과 라우트는 이미 확정돼 있어 **연동 후에도 아래 절차가 그대로 유효하다.**
> 다만 **검증면이 `화면`인 행은 실 연동이 끝나기 전까지 실행하지 않는다** — 픽스처를 상대로 통과시켜도 아무것도 증명하지 못한다. 예외는 **회원·로그인 그룹**으로, 인증 왕복은 이미 실 연동돼 있어 지금 바로 실행할 수 있다.

---

## **1. QA 대상 범위**

대상은 `docs/REQUIREMENTS.md`의 **진행 상태 = `완료`인 요구사항 57건**뿐이다. `진행중`·`대기`·`폐기`는 넣지 않는다 — 아직 만들지 않은 것을 QA하면 실패가 정보가 되지 않는다.

| 그룹 | 대상 요구사항 |
| --- | --- |
| 회원 및 로그인 | `REQ-USR-001`·`002`·`005`·`007`, `NFR-USR-001`~`006` |
| 워크스페이스 | `REQ-WS-001`~`007`, `NFR-WS-001` |
| 문서 관리 | `REQ-DOC-001`·`002`·`003`·`004`·`006`·`008`·`009`·`010`, `NFR-DOC-001` |
| 용어 추출 | `REQ-EXT-008` |
| 사전집 리뷰·승인 | `REQ-REV-002`·`003`·`004`·`005`·`008` |
| 사전집 관리 | `REQ-DIC-001`·`002`·`003`·`004`·`005`·`006`·`009`, `NFR-DIC-002` |
| 신규 문서 대조 | `NFR-CHK-001` |
| 알림 | `REQ-NTF-004`·`008`, `NFR-NTF-002`·`003` |
| 결과 처리 | `REQ-UPD-001`·`004`, `NFR-UPD-001` |
| 비동기 파이프라인 | `REQ-MSG-001`, `NFR-MSG-003` |
| 인프라·공통 | `NFR-INF-001`·`002`·`003`·`005`, `NFR-CMN-004`·`007` |

### 검증면 세 가지

| 값 | 뜻 |
| --- | --- |
| `화면` | 브라우저에서 화면을 조작해 확인한다. Chrome 플러그인이 그대로 실행할 수 있다 |
| `API` | 대응하는 화면이 아직(또는 앞으로도) 없어 Swagger UI에서 확인한다. `http://localhost:8080/swagger-ui/index.html` |
| `코드·설정` | 화면에도 API에도 드러나지 않는다. 설정 파일·DB 제약·테스트 실행으로 확인한다 |

**`API`로 분류한 이유가 「화면이 없어서」인 항목이 적지 않다.** 워크스페이스 생성·이름 수정·삭제, 멤버 초대·역할 변경·제외, 문서 삭제·수정, 일괄 승인, 알림 목록은 백엔드만 완료됐고 화면이 없다. 화면이 생기면 「7. QA 대상 갱신 규칙」 2·3번에 따라 `화면`으로 옮긴다.

---

## **2. 사전 준비**

### 기동

```
# 백엔드 — 반드시 local 프로파일을 준다
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'   # http://localhost:8080

# 프런트
cd frontend && npm run dev                                               # http://localhost:5173
```

`bootRun`이 `compose.yaml`의 MySQL·Redis·Grafana LGTM을 자동으로 띄운다. `docker compose up`을 따로 실행하지 않는다.

**프로파일을 주지 않으면 로컬 로그인이 깨진다.** 기본값이 없어 refresh token 쿠키의 `Secure` 속성이 남고, `http://localhost`에서는 브라우저가 그 쿠키를 저장하지 않아 세션 복구와 재발급이 전부 실패한다.

필요한 환경변수(**이름만** 적는다 — 값은 이 문서에 절대 쓰지 않는다): `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`. 나머지(`JWT_SECRET`, `MEMBER_ENCRYPTION_KEY`, `OAUTH_FRONTEND_REDIRECT_URI`, `CORS_ALLOWED_ORIGINS`)는 로컬 기본값이 있어 설정하지 않아도 된다. 기본값이 프런트 `:5173`에 맞춰져 있다.

### QA 계정

시드 데이터가 없다. 계정도 데이터도 직접 만든다.

| 계정 | 역할 | 준비 방법 |
| --- | --- | --- |
| **A** | 워크스페이스 OWNER | `POST /api/members` → `POST /api/auth/dev/login {"memberId": A}` |
| **B** | 리뷰어(REGULAR로 참여) | 같은 방식 |
| **C** | 비참여자(권한 시나리오용) | 같은 방식. 어떤 워크스페이스에도 넣지 않는다 |

`POST /api/auth/dev/login`은 **`local` 프로파일에서만 빈이 등록되는 뒷문**이다. Google 왕복 없이 access token과 refresh 쿠키를 준다. 화면에는 노출되지 않으므로 Swagger에서 호출한 뒤 「Authorize」에 토큰을 넣어 쓴다.

**로그인 자체를 검증하는 행(`QA-USR-001-*`, `QA-USR-002-*`)은 이 뒷문을 쓰지 않는다.** 실제 Google 동의 화면이 뜨므로 **사람이 실행한다** — Chrome 플러그인에 맡기지 않는다.

---

## **3. 기준 시나리오 `S-0` ~ `S-9`**

표의 `절차` 칸이 매번 처음부터 상태를 쌓지 않도록, 공통 시퀀스를 여기서 한 번 정의하고 각 행이 `S-5 이후` 식으로 참조한다. 흐름은 `backend/src/test/java/com/ubidict/backend/scenario/UbiquitousLanguageLifecycleTest`와 같고, 그것을 화면 조작으로 옮긴 것이다.

| ID | 단계 | 화면 / 경로 |
| --- | --- | --- |
| `S-0` | 백엔드·프런트 기동, 계정 A·B·C 준비 | — |
| `S-1` | A로 로그인 | `/login` |
| `S-2` | 워크스페이스 생성 (A가 OWNER가 된다) | `POST /api/workspaces` — 화면 없음 |
| `S-3` | 초대 발급 → B가 수락해 REGULAR로 참여 | `POST /api/workspaces/{id}/invitations` → `POST /api/invitations/{token}/accept` — 화면 없음 |
| `S-4` | 리뷰어 정족수를 1로 설정 | `/workspaces/{id}/settings/ruleset` → 「저장」 |
| `S-5` | 문서 생성 (v1이 함께 발행된다) | `/workspaces/{id}/documents/upload` → 제목·본문 입력 → 「업로드」 |
| `S-6` | 용어 추출 요청 → 작업이 끝날 때까지 대기 | `/workspaces/{id}/documents/{documentId}/extract` |
| `S-7` | 후보 단어 수동 추가 → 등재 승인 → 검토 완료 | `/workspaces/{id}/dictionary/draft` |
| `S-8` | 사전 리뷰 요청 → B가 승인 → 반영 → 사전집 v1 활성 | `/workspaces/{id}/dictionary/revisions/{revisionId}` |
| `S-9` | 문서 대조 요청 → 검토 완료 → 리뷰 → 승인 → 반영 → 문서 v2 | `/workspaces/{id}/documents/drafts`, `.../documents/{id}/review` |

> **`S-6`과 `S-9`의 순서를 바꾸지 않는다.** 초안은 워크스페이스당 상호 배타다. 문서 초안이 진행 중이면 사전 추출이 `DRAFT_DICTIONARY_ALREADY_EXISTS`로, 사전 초안이 진행 중이면 문서 대조가 `DRAFT_DOCUMENT_ALREADY_EXISTS`로 막힌다. 순서를 뒤집으면 정상 시나리오가 「실패」로 나오는데 그것은 버그가 아니다.

> **`S-7`에서 후보 단어를 손으로 넣는 이유** — 추출기가 스텁이라 결과가 항상 빈 목록이다(「6. 실행 전제와 알려진 제약」). 손으로 넣지 않으면 `S-8` 이후가 전부 막힌다.

---

## **4. QA 표**

`결과` 칸은 비워 둔 템플릿이다. 실제 판정은 「8. 실행 이력」에 회차별로 쌓는다.

### 4-1. 회원 및 로그인

| QA ID | 요구사항 | 시나리오 | 검증면 | 절차 | 기대 결과 | 결과 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| QA-USR-001-1 | REQ-USR-001 | 신규 Google 계정은 닉네임을 입력해야 회원이 된다. | 화면 | `/login` → 「Google로 계속하기」 → 아직 가입하지 않은 Google 계정으로 동의 | `/onboarding/nickname`으로 이동한다. 이 시점에는 아직 회원이 만들어지지 않았다 | | **사람이 실행.** 실제 Google 계정 필요 |
| QA-USR-001-2 | REQ-USR-001 | 닉네임을 입력해 제출하면 그때 회원이 만들어진다. | 화면 | QA-USR-001-1에 이어 닉네임 입력 → 「시작하기」 | `/workspaces`로 이동하고 상단에 입력한 닉네임이 보인다 | | Google 프로필 이름을 쓰지 않는다 |
| QA-USR-001-3 | REQ-USR-001 | 닉네임이 비면 등록이 막힌다. | 화면 | 온보딩 화면에서 닉네임을 비운 채 「시작하기」 | 제출되지 않거나 입력 오류가 표시된다 | | |
| QA-USR-001-4 | REQ-USR-001 | 온보딩 도중 새로고침하면 처음부터 다시 해야 한다. | 화면 | 온보딩 화면에서 브라우저 새로고침 | `/login`으로 돌아간다 | | 등록 토큰을 라우터 state로만 들고 있어 의도된 동작이다 |
| QA-USR-002-1 | REQ-USR-002 | 가입된 계정으로 로그인하면 워크스페이스 목록으로 간다. | 화면 | `/login` → 「Google로 계속하기」 → 이미 가입된 계정으로 동의 | 온보딩을 건너뛰고 바로 `/workspaces`로 간다 | | **사람이 실행** |
| QA-USR-002-2 | REQ-USR-002 | Google 동의를 취소하면 로그인 화면으로 돌아온다. | 화면 | Google 동의 화면에서 취소 | `/login`으로 돌아오고 로그인되지 않는다 | | |
| QA-USR-005-1 | REQ-USR-005 | 로그아웃하면 로그인 화면으로 돌아간다. | 화면 | `S-1` 이후 상단 「로그아웃」 | `/login`으로 이동한다 | | |
| QA-USR-005-2 | REQ-USR-005 | 로그아웃한 뒤에는 새로고침해도 자동 로그인되지 않는다. | 화면 | QA-USR-005-1에 이어 `/workspaces`를 주소창에 직접 입력 | `/login`으로 튕긴다 | | refresh 쿠키가 폐기됐는지 보는 행 |
| QA-USR-005-3 | REQ-USR-005 | 이미 로그아웃한 상태에서 로그아웃해도 오류가 나지 않는다. | API | 쿠키 없이 `POST /api/auth/logout` | 204 | | 멱등 |
| QA-USR-007-1 | REQ-USR-007 | 회원 탈퇴는 확인 창을 거쳐야 진행된다. | 화면 | `S-1` 이후 상단 「회원 탈퇴」 → 확인 창에서 취소 | 탈퇴되지 않고 화면이 그대로다 | | |
| QA-USR-007-2 | REQ-USR-007 | 탈퇴하면 로그아웃되고 로그인 화면으로 간다. | 화면 | 「회원 탈퇴」 → 확인 | `/login`으로 이동한다 | | |
| QA-USR-007-3 | REQ-USR-007 | 탈퇴한 계정으로는 다시 로그인할 수 없다. | API | 탈퇴한 회원 id로 `POST /api/auth/dev/login` | 로그인 불가 오류. 상태가 `WITHDRAWN`이다 | | 소프트 삭제라 행은 남는다 |
| QA-USR-NFR001-1 | NFR-USR-001 | 로그인하지 않고 보호된 화면에 들어가면 로그인으로 튕긴다. | 화면 | 브라우저 시크릿 창에서 `/workspaces`를 직접 입력 | `/login`으로 리다이렉트된다 | | |
| QA-USR-NFR001-2 | NFR-USR-001 | 요청자는 토큰에서만 식별된다. | API | A 토큰으로 임의의 워크스페이스 조회 시 쿼리에 `memberId=<B의 id>`를 덧붙인다 | 파라미터가 무시되고 A 기준으로 응답한다 | | `memberId` 파라미터는 `T-INT-3`에서 전부 제거됐다 |
| QA-USR-NFR002-1 | NFR-USR-002 | 로그인 응답에 access token과 역할이 함께 온다. | API | `POST /api/auth/dev/login` | 본문에 `accessToken`·`role`, 헤더에 refresh 쿠키 | | |
| QA-USR-NFR003-1 | NFR-USR-003 | 재발급하면 직전 refresh token은 유예 시간 뒤 거절된다. | API | `POST /api/auth/refresh` 2회 연속 → 첫 번째 토큰으로 10초 뒤 다시 재발급 | 마지막 호출이 401 `AUTH_TOKEN_INVALID` | | current/grace 2키 구조 |
| QA-USR-NFR004-1 | NFR-USR-004 | access token이 만료돼도 보던 화면에 그대로 머문다. | 화면 | `S-5` 화면에서 access token 만료를 기다린 뒤 목록을 다시 불러오는 조작 | 로그인 화면으로 튕기지 않고 데이터가 그대로 갱신된다 | | 401 → 자동 재발급 → 원요청 재시도 |
| QA-USR-NFR004-2 | NFR-USR-004 | 새로고침해도 로그인 상태가 복구된다. | 화면 | 로그인한 상태에서 브라우저 새로고침 | 로그인 화면을 거치지 않고 같은 화면이 다시 뜬다 | | access token은 메모리에만 있고 refresh 쿠키로 복구한다 |
| QA-USR-NFR005-1 | NFR-USR-005 | 로그아웃하면 서버의 refresh token이 폐기된다. | API | 로그아웃 뒤 로그아웃 전에 받아 둔 refresh 쿠키로 `POST /api/auth/refresh` | 401 | | |
| QA-USR-NFR006-1 | NFR-USR-006 | 비참여자는 워크스페이스 자원에 닿을 수 없다. | API | C 토큰으로 `S-2`의 `GET /api/workspaces/{id}` | **404** `WORKSPACE_NOT_FOUND` — 403이 아니다 | | 403이면 존재가 드러난다 |
| QA-USR-NFR006-2 | NFR-USR-006 | 참여자라도 서열이 모자라면 막힌다. | API | B(REGULAR) 토큰으로 `DELETE /api/workspaces/{id}` | 403 `WORKSPACE_OWNER_REQUIRED` | | 404가 아니라 403인 것이 핵심 |

### 4-2. 워크스페이스

| QA ID | 요구사항 | 시나리오 | 검증면 | 절차 | 기대 결과 | 결과 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| QA-WS-001-1 | REQ-WS-001 | 워크스페이스를 만들면 생성자가 OWNER가 된다. | API | A 토큰으로 `POST /api/workspaces` → 이어서 `GET /api/workspaces/{id}/participants` | 201. 참여자가 A 한 명이고 권한이 `OWNER` | | 생성 화면이 아직 없다 |
| QA-WS-001-2 | REQ-WS-001 | 이름이 비면 생성이 거절된다. | API | 이름을 빈 문자열로 `POST /api/workspaces` | 400 `COMMON_INVALID_REQUEST` | | |
| QA-WS-002-1 | REQ-WS-002 | 내가 속한 워크스페이스만 목록에 보인다. | 화면 | `S-3` 이후 C로 로그인해 `/workspaces` | `S-2`에서 만든 워크스페이스가 보이지 않는다 | | |
| QA-WS-002-2 | REQ-WS-002 | 카드를 누르면 그 워크스페이스로 들어간다. | 화면 | `S-2` 이후 A로 `/workspaces` → 카드 클릭 | `/workspaces/{id}/documents`로 이동한다 | | |
| QA-WS-003-1 | REQ-WS-003 | 초대 링크를 수락하면 참여자가 된다. | API | `POST /api/workspaces/{id}/invitations`로 토큰 발급 → B 토큰으로 `POST /api/invitations/{token}/accept` | 201. 참여자 목록에 B가 REGULAR로 추가된다 | | `S-3`과 같은 절차 |
| QA-WS-003-2 | REQ-WS-003 | 같은 초대 토큰을 두 번 수락할 수 없다. | API | QA-WS-003-1의 토큰으로 다시 `accept` | 이미 사용된 초대라는 오류 | | |
| QA-WS-003-3 | REQ-WS-003 | REGULAR는 초대를 발급할 수 없다. | API | B 토큰으로 `POST /api/workspaces/{id}/invitations` | 403 `WORKSPACE_ADMIN_REQUIRED` | | |
| QA-WS-004-1 | REQ-WS-004 | OWNER가 참여자를 ADMIN으로 올릴 수 있다. | API | A 토큰으로 `PATCH /api/workspaces/{id}/participants/{participantId}/permission`에 B의 `participantId`와 `ADMIN` | 204. 목록에서 B의 권한이 `ADMIN` | | |
| QA-WS-004-2 | REQ-WS-004 | OWNER가 아니면 권한을 바꿀 수 없다. | API | B(ADMIN) 토큰으로 다른 참여자 권한 변경 시도 | 403 `WORKSPACE_OWNER_REQUIRED` | | 권한 변경은 OWNER 전용 |
| QA-WS-004-3 | REQ-WS-004 | 소유권을 넘기면 이전 OWNER는 ADMIN으로 내려간다. | API | A 토큰으로 `PATCH .../participants/{B}/ownership` → 참여자 목록 조회 | B가 `OWNER`, A가 `ADMIN` | | 워크스페이스에 OWNER는 항상 하나다 |
| QA-WS-005-1 | REQ-WS-005 | 참여자 목록에 이름·이메일·역할·가입일이 보인다. | 화면 | `S-3` 이후 `/workspaces/{id}/settings/members` | A와 B가 네 항목과 함께 표시된다 | | |
| QA-WS-005-2 | REQ-WS-005 | REGULAR는 참여자를 제외할 수 없다. | API | B(REGULAR) 토큰으로 `DELETE /api/workspaces/{id}/participants/{A}` | 403 `WORKSPACE_ADMIN_REQUIRED` | | 제외 화면이 아직 없다 |
| QA-WS-005-3 | REQ-WS-005 | OWNER는 제외할 수 없다. | API | ADMIN 토큰으로 `DELETE /api/workspaces/{id}/participants/{participantId}`에 OWNER의 `participantId` | 400대 `WORKSPACE_OWNER_CANNOT_BE_REMOVED` | | 소유권을 먼저 넘겨야 한다 |
| QA-WS-006-1 | REQ-WS-006 | 워크스페이스 이름을 바꾸면 화면에 반영된다. | API | A 토큰으로 `PATCH /api/workspaces/{id}` → 이어서 `/workspaces` 화면 새로고침 | 목록 카드의 이름이 바뀐다 | | 이름 수정 화면이 아직 없다 |
| QA-WS-006-2 | REQ-WS-006 | REGULAR는 이름을 바꿀 수 없다. | API | B 토큰으로 `PATCH /api/workspaces/{id}` | 403 `WORKSPACE_ADMIN_REQUIRED` | | |
| QA-WS-007-1 | REQ-WS-007 | OWNER만 워크스페이스를 삭제할 수 있다. | API | ADMIN 토큰으로 `DELETE /api/workspaces/{id}` | 403 `WORKSPACE_OWNER_REQUIRED` | | |
| QA-WS-007-2 | REQ-WS-007 | 삭제된 워크스페이스는 참여자에게도 보이지 않는다. | 화면 | OWNER로 삭제한 뒤 B로 로그인해 `/workspaces` | 목록에 없고, 주소를 직접 쳐도 들어가지지 않는다 | | 참여자 행은 남지만 접근이 막힌다 |
| QA-WS-NFR001-1 | NFR-WS-001 | 비참여자가 주소를 직접 쳐도 워크스페이스에 들어갈 수 없다. | 화면 | C로 로그인한 뒤 주소창에 `S-2`의 `/workspaces/{id}/documents` 입력 | 찾을 수 없다는 화면. 존재 여부가 드러나지 않는다 | | `NFR-USR-006`과 함께 본다 |
| QA-WS-NFR001-2 | NFR-WS-001 | 다른 워크스페이스의 문서 id로는 조회되지 않는다. | API | 워크스페이스 X의 경로에 워크스페이스 Y의 `documentId`를 끼워 `GET` | 404 | | 워크스페이스가 둘 필요하다 |

### 4-3. 문서 관리

| QA ID | 요구사항 | 시나리오 | 검증면 | 절차 | 기대 결과 | 결과 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| QA-DOC-001-1 | REQ-DOC-001 | 제목과 본문을 넣고 올리면 문서가 v1으로 생긴다. | 화면 | `S-2` 이후 `.../documents/upload` → 제목·본문 입력 → 「업로드」 | 문서 목록으로 이동하고 새 문서의 버전이 1이다 | | `S-5`와 같은 절차 |
| QA-DOC-001-2 | REQ-DOC-001 | 본문이 비면 올릴 수 없다. | 화면 | 업로드 화면에서 본문을 비운 채 「업로드」 | 제출되지 않거나 입력 오류가 뜬다 | | |
| QA-DOC-001-3 | NFR-DOC-001 | 본문이 10,000자를 넘으면 거절된다. | 화면 | 업로드 화면 본문에 10,001자를 붙여넣고 「업로드」 | 길이 제한 안내가 뜨고 저장되지 않는다 | | 서버도 400으로 막는다 |
| QA-DOC-001-4 | NFR-DOC-001 | 제목이 200자를 넘으면 거절된다. | API | 제목 201자로 `POST /api/workspaces/{id}/documents` | 400 `COMMON_INVALID_REQUEST` | | 파일이 아니라 JSON 본문을 받는다 |
| QA-DOC-001-5 | REQ-DOC-001 | 비참여자는 문서를 올릴 수 없다. | API | C 토큰으로 `POST /api/workspaces/{id}/documents` | 404 `WORKSPACE_NOT_FOUND` | | `NFR-WS-001` 동시 검증 |
| QA-DOC-002-1 | REQ-DOC-002 | 목록에 제목·작성자·수정일·버전·라벨이 보인다. | 화면 | `S-5` 이후 `.../documents` | 다섯 항목이 모두 표시된다 | | |
| QA-DOC-002-2 | REQ-DOC-002 | 라벨로 목록을 거를 수 있다. | 화면 | 라벨이 다른 문서를 둘 만든 뒤 목록에서 한 라벨을 고른다 | 그 라벨이 붙은 문서만 남는다 | | |
| QA-DOC-002-3 | REQ-DOC-002 | 목록 응답에는 본문이 실리지 않는다. | API | `GET /api/workspaces/{id}/documents` | 각 항목에 본문 필드가 없다 | | 목록이 무거워지지 않게 한 결정 |
| QA-DOC-003-1 | REQ-DOC-003 | 문서를 열면 최신 확정 버전의 본문이 보인다. | 화면 | `S-5` 이후 목록에서 문서 클릭 | 업로드한 본문이 그대로 보인다 | | |
| QA-DOC-003-2 | REQ-DOC-003 | 없는 문서 id로 들어가면 찾을 수 없다고 나온다. | 화면 | 주소창의 `documentId`를 존재하지 않는 값으로 바꾼다 | 「문서를 찾을 수 없습니다」 | | |
| QA-DOC-004-1 | REQ-DOC-004 | ADMIN 이상이 삭제하면 목록에서 사라진다. | API | A 토큰으로 `DELETE /api/workspaces/{id}/documents/{documentId}` → 목록 화면 새로고침 | 204. 목록에 더 이상 없다 | | 삭제 화면이 아직 없다 |
| QA-DOC-004-2 | REQ-DOC-004 | REGULAR는 문서를 삭제할 수 없다. | API | B 토큰으로 같은 `DELETE` | 403 `WORKSPACE_ADMIN_REQUIRED` | | |
| QA-DOC-006-1 | REQ-DOC-006 | 사전집이 없으면 문서에 뒤처짐 배지가 붙지 않는다. | 화면 | `S-5` 직후(사전집 발행 전) 목록 확인 | 「뒤처짐」·「재검사 필요」 배지가 없다 | | |
| QA-DOC-006-2 | REQ-DOC-006 | 사전집이 발행되면 기존 문서에 재검사 배지가 붙는다. | 화면 | `S-8` 이후 문서 목록 확인 | 해당 문서에 「재검사 필요」 배지가 보인다 | | |
| QA-DOC-006-3 | REQ-DOC-006 | 대조 작업 상태를 조회하면 네 상태 중 하나가 온다. | API | `S-9`의 대조 요청 직후 `GET /api/draft-documents/checks/{checkJobId}` | 대기·실행중·성공·실패 중 하나 | | |
| QA-DOC-008-1 | REQ-DOC-008 | 제목과 라벨을 수정하면 목록에 반영된다. | API | `PATCH /api/workspaces/{id}/documents/{documentId}` → 목록 화면 새로고침 | 바뀐 제목과 라벨이 보인다. 라벨은 통째로 교체된다 | | 수정 화면이 아직 없다 |
| QA-DOC-008-2 | REQ-DOC-008 | 본문을 직접 편집하면 즉시 새 버전이 발행된다. | API | `PATCH .../documents/{documentId}/content` → `GET .../versions` | 버전이 하나 늘고 새 버전이 `edited`로 표시된다 | | 이 버전은 추출 대상에서 빠진다 |
| QA-DOC-009-1 | REQ-DOC-009 | 새 라벨 이름을 넣으면 워크스페이스 라벨로 생긴다. | 화면 | `.../settings/labels` → 「새 라벨 이름」에 입력 후 추가 | 목록에 새 라벨이 나타난다 | | |
| QA-DOC-009-2 | REQ-DOC-009 | 같은 이름의 라벨은 두 번 생기지 않는다. | 화면 | 방금 만든 라벨과 같은 이름을 다시 추가 | 중복 안내가 뜨거나 항목이 늘지 않는다 | | DB에 `(workspace_id, name)` 유니크 |
| QA-DOC-009-3 | REQ-DOC-009 | 문서당 라벨은 5개를 넘을 수 없다. | API | 라벨 6개를 실어 `PATCH .../documents/{documentId}` | 400대 오류로 거절된다 | | |
| QA-DOC-010-1 | REQ-DOC-010 | 버전 이력에서 과거 버전을 고르면 그때 본문이 보인다. | 화면 | `S-9` 이후 문서 상세 → 「버전 이력」 → v1 선택 | v2가 아니라 v1의 본문이 보인다 | | 반영 경로가 있어야 v2가 생긴다 |
| QA-DOC-010-2 | REQ-DOC-010 | 없는 버전 번호는 조회되지 않는다. | API | `GET .../versions/999` | 404 | | |

### 4-4. 용어 추출

| QA ID | 요구사항 | 시나리오 | 검증면 | 절차 | 기대 결과 | 결과 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| QA-EXT-008-1 | REQ-EXT-008 | 추출을 요청하면 기다리지 않고 접수된다. | 화면 | `S-5` 이후 `.../documents/{documentId}/extract`에서 추출 실행 | 화면이 멈추지 않고 진행 상태 표시로 넘어간다 | | 서버는 `202` + `jobId` |
| QA-EXT-008-2 | REQ-EXT-008 | 작업 상태를 조회하면 결국 성공으로 끝난다. | API | `GET /api/draft-dictionaries/extractions/{extractionJobId}`를 상태가 바뀔 때까지 반복 | 실행중을 거쳐 성공이 된다 | | 몇 초 안에 끝난다 |
| QA-EXT-008-3 | REQ-EXT-008 | 추출 결과가 비어도 작업은 성공이다. | API | QA-EXT-008-2의 응답과 초안의 후보 단어 수 확인 | 작업은 성공이고 후보 단어는 0건 | | **스텁이라 정상이다.** 「6」 참조 |
| QA-EXT-008-4 | REQ-EXT-008 | REGULAR는 추출을 요청할 수 없다. | API | B 토큰으로 `POST /api/draft-dictionaries/extractions` | 403 `WORKSPACE_ADMIN_REQUIRED` | | |

### 4-5. 사전집 리뷰·승인

| QA ID | 요구사항 | 시나리오 | 검증면 | 절차 | 기대 결과 | 결과 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| QA-REV-002-1 | REQ-REV-002 | 후보 단어를 고르면 근거 문장과 출현 문서가 함께 보인다. | 화면 | `S-7`에서 후보 단어를 추가한 뒤 그 항목 선택 | 「근거 문장」과 출현/문서 수가 표시된다 | | 추가할 때 근거 문장을 넣어야 보인다 |
| QA-REV-002-2 | REQ-REV-002 | 상세 응답에 출처 문서 id와 스니펫이 함께 온다. | API | `GET /api/candidate-terms/{candidateTermId}` | `occurredDocumentIds`·`contextSnippets`가 채워져 있다 | | |
| QA-REV-003-1 | REQ-REV-003 | 표준어 후보를 고쳐 저장하면 목록에 반영된다. | 화면 | `S-7`의 초안 화면에서 「표준어 후보」를 수정 후 저장 | 목록의 표기가 바뀐다 | | |
| QA-REV-003-2 | REQ-REV-003 | 표기를 공백으로만 채우면 저장이 거절된다. | API | `PATCH /api/candidate-terms/{id}`에 공백 문자열 표기 | 400대 `DRAFT_DICTIONARY_INVALID_FORM` | | |
| QA-REV-003-3 | REQ-REV-003 | 기존 대표어에 동의어로 합칠 수 있다. | API | `POST /api/candidate-terms/{id}/synonym-merge`에 대상 용어 id | 상태가 동의어 병합으로 바뀐다 | | 대상 id가 없으면 거절된다 |
| QA-REV-004-1 | REQ-REV-004 | 정의를 편집해 저장할 수 있다. | 화면 | 초안 화면에서 「정의」를 고쳐 저장 | 고친 정의가 유지된다 | | |
| QA-REV-004-2 | REQ-REV-004 | 정의가 비면 등재 승인이 거절된다. | API | 정의를 비운 후보에 `POST /api/candidate-terms/{id}/registration-approval` | 400대 `DRAFT_DICTIONARY_CANDIDATE_DEFINITION_REQUIRED` | | 정의 없는 용어는 발행 단계에서도 막힌다 |
| QA-REV-005-1 | REQ-REV-005 | 후보를 등재 승인하면 상태가 바뀐다. | 화면 | `S-7`에서 후보를 승인 | 해당 항목의 상태 표시가 승인으로 바뀐다 | | |
| QA-REV-005-2 | REQ-REV-005 | 기각하려면 사유가 있어야 한다. | API | 사유 없이 `POST /api/candidate-terms/{id}/rejection` | 400대 `DRAFT_DICTIONARY_REJECT_REASON_REQUIRED` | | |
| QA-REV-005-3 | REQ-REV-005 | 판정하지 않은 후보가 남으면 검토를 끝낼 수 없다. | API | 대기 상태 후보를 남긴 채 `POST /api/draft-dictionaries/{id}/examine-completion` | 거절된다 | | `S-7`을 끝내려면 전부 판정해야 한다 |
| QA-REV-008-1 | REQ-REV-008 | 여러 후보를 한 번에 같은 판정으로 처리할 수 있다. | API | 후보 3건을 만든 뒤 `POST /api/draft-dictionaries/{id}/candidate-terms/bulk-decision` | 3건의 상태가 모두 바뀐다 | | 일괄 처리 화면이 아직 없다 |
| QA-REV-008-2 | REQ-REV-008 | 일괄 기각도 사유를 요구한다. | API | 사유 없이 기각으로 `bulk-decision` 호출 | 거절된다 | | 개별 기각과 같은 규칙 |

### 4-6. 사전집 관리

| QA ID | 요구사항 | 시나리오 | 검증면 | 절차 | 기대 결과 | 결과 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| QA-DIC-001-1 | REQ-DIC-001 | 리뷰 승인 뒤 반영하면 사전집 v1이 활성화된다. | 화면 | `S-8`을 끝까지 진행한 뒤 `.../dictionary` | `S-7`에서 승인한 용어가 사전집에 보인다 | | 이 흐름이 제품의 중심 경로다 |
| QA-DIC-001-2 | REQ-DIC-001 | 정족수를 채우지 못하면 반영할 수 없다. | API | `S-4`에서 정족수를 2로 두고 승인 1건만 받은 뒤 `POST /api/review-requests/{id}/revision` | 400대 `REVIEW_REQUEST_NOT_ELIGIBLE_FOR_REVISE` | | 계정이 셋 필요하다 |
| QA-DIC-002-1 | REQ-DIC-002 | 사전집 화면에 표준어·영문명·정의·최종 수정이 보인다. | 화면 | `S-8` 이후 `.../dictionary` | 네 열이 모두 표시된다 | | |
| QA-DIC-002-2 | REQ-DIC-002 | 키워드로 검색하면 해당 용어만 남는다. | 화면 | 용어를 둘 이상 등재한 뒤 검색창에 한 용어의 일부를 입력 | 그 용어만 남고 총 건수가 함께 줄어든다 | | |
| QA-DIC-002-3 | REQ-DIC-002 | 허용되지 않은 정렬 키는 거절된다. | API | `GET /api/workspaces/{id}/dictionary?sort=definition,asc` | 400 `COMMON_INVALID_REQUEST` | | 화이트리스트는 `preferredForm`·`createdAt` |
| QA-DIC-002-4 | REQ-DIC-002 | 페이지 크기 상한을 넘기면 거절된다. | API | `...?size=101` | 400 `COMMON_INVALID_REQUEST` | | 상한 100 |
| QA-DIC-003-1 | REQ-DIC-003 | 용어의 대표어와 정의가 함께 표시된다. | 화면 | `.../dictionary`에서 용어 한 건 확인 | 표준어와 정의가 같이 보인다 | | 별도 상세 화면은 두지 않았다 |
| QA-DIC-004-1 | REQ-DIC-004 | 초안에서 후보 단어를 직접 추가할 수 있다. | 화면 | `.../dictionary/draft` → 「새 후보 단어」에 입력 후 추가 | 목록에 추가된다 | | AI 추출과 무관한 수동 등록 경로 |
| QA-DIC-004-2 | REQ-DIC-004 | 초안에서 이전 사전집 용어도 고칠 수 있다. | 화면 | 사전집 v1 발행 후 두 번째 추출을 돌려 초안에 들어온 기존 용어를 수정 | 수정한 내용이 유지된다 | | 확정 사전집을 직접 고치는 경로는 없다 |
| QA-DIC-004-3 | REQ-DIC-004 | 확정된 사전집의 용어를 직접 고치는 API는 없다. | API | Swagger의 Dictionary 절 확인 | `GET` 세 개뿐이고 수정·삭제가 없다 | | 교정은 초안에서만 한다 |
| QA-DIC-005-1 | REQ-DIC-005 | 새 버전이 발행되면 이전 버전은 보관으로 내려간다. | API | 두 번째 반영을 끝낸 뒤 `GET .../dictionary/versions` | v1이 보관, v2가 활성이다 | | |
| QA-DIC-005-2 | REQ-DIC-005 | 사전집을 직접 발행하는 엔드포인트는 없다. | API | Swagger의 Dictionary 절 확인 | 발행용 `POST`가 없다 | | 임시 엔드포인트는 `DIC-7`에서 제거됐다 |
| QA-DIC-006-1 | REQ-DIC-006 | 버전 이력이 시간순으로 보인다. | 화면 | `.../dictionary/history` | 발행된 버전이 순서대로 보인다 | | |
| QA-DIC-006-2 | REQ-DIC-006 | 특정 버전을 고르면 그때 용어 목록이 보인다. | API | `GET .../dictionary/versions/1` | v1 시점의 용어가 온다 | | 버전마다 용어가 복제된다 |
| QA-DIC-009-1 | REQ-DIC-009 | 사전집이 없는 워크스페이스는 빈 상태로 보인다. | 화면 | `S-2` 직후(발행 전) `.../dictionary` | 오류가 아니라 비어 있는 화면이 뜬다 | | |
| QA-DIC-NFR002-1 | NFR-DIC-002 | 같은 표준어를 한 사전집에 두 번 등재할 수 없다. | API | 같은 표기의 후보 둘을 승인한 뒤 반영 | 발행이 거절되거나 유니크 제약에 걸린다 | | `uk_term_dictionary_preferred_form` |

### 4-7. 신규 문서 대조

| QA ID | 요구사항 | 시나리오 | 검증면 | 절차 | 기대 결과 | 결과 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| QA-CHK-NFR001-1 | NFR-CHK-001 | 대조를 요청하면 기다리지 않고 접수된다. | 화면 | `S-8` 이후 문서 상세에서 대조(재검사) 실행 | 화면이 멈추지 않고 진행 상태로 넘어간다 | | 서버는 `202` + `jobId` |
| QA-CHK-NFR001-2 | NFR-CHK-001 | 접수 직후 상태는 아직 끝나지 않은 상태다. | API | 요청 직후 곧바로 `GET /api/draft-documents/checks/{checkJobId}` | 대기 또는 실행중 | | 커밋 후 비동기로 실행된다 |
| QA-CHK-NFR001-3 | NFR-CHK-001 | 사전집이 없으면 대조를 요청할 수 없다. | API | `S-5`까지만 한 상태에서 `POST /api/draft-documents/checks` | 400대 `DRAFT_DOCUMENT_DICTIONARY_NOT_FOUND` | | 대조는 활성 사전집이 있어야 성립한다 |
| QA-CHK-NFR001-4 | NFR-CHK-001 | 사전 초안이 진행 중이면 대조가 막힌다. | API | `S-6`으로 추출 초안을 열어 둔 채 `POST /api/draft-documents/checks` | 400대 `DRAFT_DOCUMENT_ALREADY_EXISTS` | | 상호 배타 — 「3」의 경고 참조 |

### 4-8. 알림

| QA ID | 요구사항 | 시나리오 | 검증면 | 절차 | 기대 결과 | 결과 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| QA-NTF-004-1 | REQ-NTF-004 | 알림 목록이 최신순으로 온다. | API | `S-8`의 리뷰 요청 뒤 B 토큰으로 `GET /api/workspaces/{id}/notifications` | 최근 알림이 먼저 온다 | | 알림 패널 화면이 아직 없다 |
| QA-NTF-004-2 | REQ-NTF-004 | 미읽음만 골라 볼 수 있다. | API | `...?unreadOnly=true` | 읽지 않은 것만 온다 | | |
| QA-NTF-004-3 | REQ-NTF-004 | 개별 읽음은 여러 번 눌러도 결과가 같다. | API | `PATCH .../notifications/{notificationId}/read`를 두 번 | 두 번 다 성공하고 상태가 같다 | | 멱등 |
| QA-NTF-004-4 | REQ-NTF-004 | 모두 읽음 뒤 미읽음 수가 0이 된다. | API | `PATCH .../notifications/read-all` → `GET .../unread-count` | `0` | | |
| QA-NTF-004-5 | REQ-NTF-004 | 남의 알림은 보이지 않는다. | API | A 토큰으로 목록 조회 | B에게 간 리뷰 요청 알림이 A 목록에 없다 | | 수신자별로 갈린다 |
| QA-NTF-008-1 | REQ-NTF-008 | 리뷰 요청을 만들면 지정된 리뷰어에게 알림이 간다. | API | `S-8`의 리뷰 요청 직후 B 토큰으로 `GET .../unread-count` | 1 이상으로 늘어난다 | | 비동기라 잠시 뒤 조회한다 |
| QA-NTF-008-2 | REQ-NTF-008 | 반영이 끝나면 워크스페이스 참여자 전원에게 알림이 간다. | API | `S-8`의 반영 직후 A와 B 양쪽 알림 목록 조회 | 둘 다 반영완료 알림을 받는다 | | 유형마다 수신자가 다르다 |

### 4-9. 결과 처리·사전집 갱신

| QA ID | 요구사항 | 시나리오 | 검증면 | 절차 | 기대 결과 | 결과 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| QA-UPD-001-1 | REQ-UPD-001 | 제안을 적용하면 미리보기 본문에 대체어가 들어간다. | 화면 | `S-9`의 검토 화면에서 제안을 「적용」 → 미리보기 확인 | 원래 표기가 대표어로 바뀌어 보인다 | | **스텁이라 제안이 0건이다.** 손으로 제안을 추가한 뒤 실행한다 |
| QA-UPD-001-2 | REQ-UPD-001 | 처리하지 않은 제안이 남으면 검토를 끝낼 수 없다. | 화면 | 제안을 남긴 채 「검토 완료 · 리뷰 요청」 | 미처리 제안이 있다는 안내가 뜬다 | | |
| QA-UPD-001-3 | REQ-UPD-001 | 사전집에 없는 표기는 대체어로 수락되지 않는다. | API | 활성 사전집에 없는 표기를 제안어로 넣고 `POST /api/suggestion-terms/{id}/acceptance` | 400대 `DRAFT_DOCUMENT_INVALID_SUGGESTION_TERM` | | |
| QA-UPD-004-1 | REQ-UPD-004 | 정족수를 채우면 반영할 수 있다. | 화면 | `S-8`에서 B가 승인한 뒤 A가 반영 | 새 버전이 발행된다 | | `S-4`의 정족수 1 전제 |
| QA-UPD-004-2 | REQ-UPD-004 | 변경요청이 남아 있으면 반영할 수 없다. | API | B가 변경요청으로 판정한 뒤 `POST /api/review-requests/{id}/revision` | 400대로 거절된다 | | |
| QA-UPD-004-3 | REQ-UPD-004 | 재교정하면 회차가 올라간다. | API | 변경요청 뒤 `POST /api/review-requests/{id}/reexaminations` → `GET`으로 확인 | 회차가 1 늘어난다 | | 같은 회차를 두 번 만들 수 없다 |
| QA-UPD-004-4 | REQ-UPD-004 | ADMIN 미만은 반영할 수 없다. | API | B(REGULAR) 토큰으로 `POST /api/review-requests/{id}/revision` | 403 `REVIEW_REQUEST_ACCESS_DENIED` | | |
| QA-UPD-004-5 | REQ-UPD-004 | 같은 리뷰 요청을 두 번 반영할 수 없다. | API | 반영이 끝난 요청에 다시 `POST .../revision` | 400대 `REVIEW_REQUEST_ALREADY_REVISED` | | |
| QA-UPD-NFR001-1 | NFR-UPD-001 | 사람 승인 없이 사전집이 바뀌는 경로가 없다. | API | `S-6`·`S-7`만 하고 리뷰·반영을 건너뛴 뒤 `GET .../dictionary` | 사전집이 여전히 비어 있다 | | 추출만으로는 아무것도 확정되지 않는다 |

### 4-10. 비동기 파이프라인

| QA ID | 요구사항 | 시나리오 | 검증면 | 절차 | 기대 결과 | 결과 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| QA-MSG-001-1 | REQ-MSG-001 | 추출 작업 id로 상태를 조회할 수 있다. | API | `GET /api/draft-dictionaries/extractions/{extractionJobId}` | 상태와 결과 초안 id가 온다 | | 폴링 방식이다 |
| QA-MSG-001-2 | REQ-MSG-001 | 대조 작업 id로 상태를 조회할 수 있다. | API | `GET /api/draft-documents/checks/{checkJobId}` | 상태와 결과 초안 id가 온다 | | 도메인마다 작업 테이블이 따로다 |
| QA-MSG-001-3 | REQ-MSG-001 | 남의 워크스페이스 작업은 조회할 수 없다. | API | C 토큰으로 위 두 조회 | 404 | | |

---

## **5. 코드·설정으로 확인하는 항목**

화면에도 API에도 드러나지 않는 항목이다. 브라우저로 검증할 수 없으므로 Chrome 플러그인에 맡기지 않고 사람이 확인한다.

| QA ID | 요구사항 | 시나리오 | 검증면 | 절차 | 기대 결과 | 결과 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| QA-INF-001-1 | NFR-INF-001 | `bootRun` 한 번으로 로컬 인프라가 함께 뜬다. | 코드·설정 | 컨테이너를 모두 내린 뒤 `./gradlew bootRun --args='--spring.profiles.active=local'` | MySQL·Redis·Grafana LGTM 컨테이너가 자동으로 기동한다 | | `docker compose up`을 직접 실행하지 않는다 |
| QA-INF-002-1 | NFR-INF-002 | 프로파일이 넷으로 갈려 있다. | 코드·설정 | `backend/src/main/resources/`의 `application-*.yml` 확인 | `local`·`dev`·`prod`·`test`가 모두 있다 | | |
| QA-INF-002-2 | NFR-INF-002 | 시크릿이 저장소에 없다. | 코드·설정 | `application*.yml`에서 시크릿 항목이 `${ENV}` 참조인지 확인 | 실제 값이 파일에 박혀 있지 않다 | | 값을 이 문서에도 적지 않는다 |
| QA-INF-003-1 | NFR-INF-003 | Flyway가 스키마를 만든다. | 코드·설정 | 빈 DB로 기동한 뒤 `flyway_schema_history` 조회 | 마이그레이션이 순서대로 적용돼 있다 | | |
| QA-INF-003-2 | NFR-INF-003 | 운영 프로파일이 스키마를 자동 생성하지 않는다. | 코드·설정 | `application-prod.yml`의 `ddl-auto` 확인 | `create`·`create-drop`·`update`가 아니다 | | 루트 `CLAUDE.md`의 금지 항목 |
| QA-INF-005-1 | NFR-INF-005 | 경계가 전부 설정값으로 갈린다. | 코드·설정 | `app.crossdomain.*`·`app.messaging.mode`·`app.ai.*` 프로퍼티 확인 | 코드 수정 없이 프로파일로 교체 가능하다 | | |
| QA-CMN-004-1 | NFR-CMN-004 | Swagger UI가 뜨고 인증 버튼이 있다. | 코드·설정 | `http://localhost:8080/swagger-ui/index.html` 접속 | 전 도메인 API가 보이고 「Authorize」로 Bearer 토큰을 넣을 수 있다 | | 이 표의 `API` 행이 전부 여기에 기댄다 |
| QA-CMN-007-1 | NFR-CMN-007 | 자동 테스트가 전부 통과한다. | 코드·설정 | `cd backend && ./gradlew test` | 실패 0건 | | 시나리오 테스트가 포함된다 |
| QA-MSG-003-1 | NFR-MSG-003 | 같은 알림이 두 번 만들어지지 않는다. | 코드·설정 | `V700__create_notification.sql`의 유니크 인덱스 확인 후, 같은 이벤트를 두 번 흘려 본다 | `uq_notification_recipient_dedupe`에 걸려 두 번째가 생기지 않는다 | | `NFR-NTF-002`와 같은 장치다 |
| QA-NTF-NFR002-1 | NFR-NTF-002 | 이벤트를 다시 받아도 알림이 다시 발송되지 않는다. | 코드·설정 | 위와 같은 절차 | 알림 수가 늘지 않는다 | | SQS는 at-least-once라 재수신이 정상이다 |
| QA-NTF-NFR003-1 | NFR-NTF-003 | 알림 생성이 실패해도 본 작업이 되돌아가지 않는다. | 코드·설정 | 알림 핸들러에서 예외가 나도록 만든 뒤 리뷰 요청을 생성 | 리뷰 요청은 커밋되고 알림만 ERROR 로그로 남는다 | | `AFTER_COMMIT` + `@Async` |

---

## **6. 실행 전제와 알려진 제약**

이 표를 돌리기 전에 반드시 읽는다. **여기 적힌 것들은 버그가 아니라 현재 상태다.** 모르고 실행하면 정상 동작을 실패로 기록하게 된다.

- **프런트 실 연동이 선행 조건이다.** 검증면이 `화면`인 행은 연동 전까지 실행하지 않는다. 회원·로그인 그룹(4-1)만 예외로 지금 실행할 수 있다.
- **AI가 스텁이다.** `app.ai.extractor.mode`·`app.ai.checker.mode`가 `stub` 기본값이라 추출기와 대조기가 **항상 빈 목록**을 돌려준다. 작업은 성공으로 끝나지만 후보 단어와 제안어가 0건인 것이 정상이다. `S-7`에서 후보 단어를, `QA-UPD-001-*`에서 제안어를 손으로 넣어야 흐름이 이어진다.
- **초안은 워크스페이스당 상호 배타다.** 「3」의 경고 참조. `S-6`과 `S-9`의 순서를 지킨다.
- **알림 패널 화면이 없다.** `REQ-NTF-004`·`008`은 백엔드만 완료라 전부 `API`로 검증한다. 상단 종 아이콘은 아직 동작하지 않는 장식이다.
- **화면이 없어 `API`로 검증하는 기능들** — 워크스페이스 생성·이름 수정·삭제, 멤버 초대·역할 변경·제외, 문서 삭제·수정, 후보어 일괄 판정, 알림. 화면이 생기면 「7」의 2·3번에 따라 옮긴다.
- **에러 응답에 `traceId`가 없다.** `NFR-CMN-003`이 요구하지만 아직 미구현이다(`docs/plan/CONFLICTS.md` `X-08`). 응답에 없다고 실패로 적지 않는다.
- **시드 데이터가 없다.** 모든 QA는 계정과 데이터를 직접 쌓아 올린 뒤 시작한다.

---

## **7. QA 대상 갱신 규칙**

**QA 표는 기능을 따라간다. 기능이 늘었는데 표가 그대로면 그 기능은 아무도 확인하지 않는다.**

1. **`docs/REQUIREMENTS.md`의 진행 상태를 `완료`로 올리는 변경은, 같은 PR에서 이 문서에 QA 행을 함께 추가한다.** QA 행이 없는 기능은 완료로 보고하지 않는다. 최소한 정상 경로 1행과 권한·검증 실패 1행을 넣는다.
2. **새 화면이나 새 엔드포인트가 생기면** 해당 요구사항의 행에 절차를 더하거나 새 행을 만든다. QA ID의 연번을 이어 붙인다.
3. **프런트 실 연동이 끝나면** 그 기능의 `검증면`을 `API` → `화면`으로 옮기고 절차를 화면 조작으로 바꾼다. 「6」의 「화면이 없어 `API`로 검증하는 기능들」과 「프런트 실 연동이 선행 조건이다」에서도 해당 항목을 지운다.
4. **AI 스텁이 실제 어댑터로 바뀌면**(`app.ai.*.mode=real`) 「6」의 스텁 설명과 `QA-EXT-008-3`·`QA-UPD-001-1`의 기대 결과를 함께 고친다. 「빈 목록이 정상」이 더는 참이 아니게 된다.
5. **요구사항이 `폐기`되면 행을 지우지 않는다.** `검증면`을 `—`로 바꾸고 `비고`에 `폐기(M/D)`를 적는다. 왜 더 이상 확인하지 않는지가 남아야 나중에 되묻지 않는다.
6. **행을 삭제하는 경우는 요구사항 자체가 사라졌을 때뿐이다.** 통과하기 어렵다는 이유로 지우거나 조건을 무르게 고치지 않는다 — 실패하면 원인을 보고한다(루트 `CLAUDE.md`의 테스트 규칙과 같다).
7. 요구사항 ID·에러 코드·엔드포인트를 적을 때는 `docs/REQUIREMENTS.md`와 `docs/API.md`에서 그대로 가져온다. 기억으로 적지 않는다.

---

## **8. 실행 이력**

본문 표의 `결과` 칸은 비워 두는 템플릿이다. 실제 판정은 회차별로 여기에 쌓는다.

| 일시 | 실행자 | 대상 범위 | Pass | Fail | 비고 |
| --- | --- | --- | --- | --- | --- |
| | | | | | |
