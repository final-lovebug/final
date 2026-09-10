# **Frontend Architecture Convention**

## **목표**

프론트엔드(Vite + React SPA)는 루트 `docs/ARCHITECTURE.md`가 백엔드에 적용하는
**도메인 중심의 레이어드 아키텍처** 방향성을 그대로 따른다.

**핵심 목표는 페이지 컴포넌트를 읽었을 때 화면이 어떤 데이터를 어떤 순서로 조합해
보여주는지 알 수 있게 만드는 것**이다. 신규 합류자가 페이지 컴포넌트만 읽어도 화면
구성 흐름을 이해할 수 있어야 하고, 통신 기술(fetch/axios)과 서버 응답 형태는 도메인
모델 뒤로 숨긴다.

## **참고 자료**

이 구조를 실제로 반영하기 전에 "Vite+React 프로젝트를 실무에서 이런 식으로 구성하는지"를
먼저 확인했다. 아래 두 레퍼런스를 조합한 형태이며, 어느 한쪽 기준으로 봐도 낯선 구조가
아니다.

- [bulletproof-react](https://github.com/alan2207/bulletproof-react/blob/master/docs/project-structure.md) —
  React 아키텍처 레퍼런스로 가장 많이 인용되는 저장소. `features/{feature}/{api, components,
  hooks, stores, types, utils}` 구성과 최상위 공용 코드, **"shared → features → app" 단방향
  흐름**, **feature 간 직접 참조 금지** 규칙의 출처다.
- [Feature-Sliced Design (FSD)](https://feature-sliced.design/docs/reference/layers) —
  슬라이스 내부 세그먼트 표준 이름을 `ui`, `model`, `api`, `lib`, `config`로 규정한다.
  이 문서의 `model`/`api` 명칭은 임의 작명이 아니라 FSD 표준 세그먼트 이름을 따른 것이다.

즉 이 문서의 폴더 이름은 **FSD의 세그먼트 이름(`model`, `api`) + bulletproof-react의
폴더 배치(`features`, `shared`, 단방향 의존)**를 조합했고, 여기에 루트 `docs/ARCHITECTURE.md`가
쓰는 레이어 용어(presentation/service/infra/domain)를 대응시켜 백엔드와 같은 어휘로
설명할 수 있게 했다.

상태관리(서버 상태 vs 클라이언트 상태 분리) 역시 실무 확인을 거쳤다.

- [Zustand and TanStack Query: The Dynamic Duo](https://medium.com/@contato.blense/zustand-and-tanstack-query-the-dynamic-duo-that-simplified-my-react-state-management-486c3073f81c) —
  Zustand는 클라이언트 상태(UI, 인증 세션), TanStack Query는 서버 상태(API 데이터·캐시·동기화)를
  맡는 역할 분리가 정착됐다는 근거.
- [React State Management in 2026](https://vucense.com/dev-corner/react-state-management-2026/) —
  신규 프로젝트 다운로드 기준 Zustand가 Redux Toolkit을 추월했고, TanStack Query와 조합하는
  패턴이 표준적 접근으로 굳어졌다는 근거.

## **기본 폴더 구조**

도메인(feature)을 최상위 기준으로 나누고, 도메인 내부에서 레이어를 나눈다. 여러 도메인이
공유하는 것은 `shared`로 뺀다.

```text
src/
├── app/                # 앱 부트스트랩: main.tsx, App.tsx, 라우터 설정, QueryClientProvider, AppLayout
├── pages/              # 라우트에 매핑되는 화면(페이지) 컴포넌트 — ui/main.js render*Screen 대응
│   ├── LoginPage.tsx
│   ├── WorkspacesPage.tsx
│   ├── document/
│   ├── dictionary/
│   ├── review/
│   └── settings/
├── features/           # 도메인별 기능 모듈
│   ├── auth/
│   ├── workspace/
│   ├── document/
│   ├── dictionary/
│   ├── review/
│   ├── notification/
│   └── member/
│       ├── api/          # infra: TanStack Query query/mutation 함수, 서버 DTO ↔ 도메인 모델 매핑
│       ├── model/         # domain: 타입, 값 객체, 상태 전이/검증 규칙, 목데이터(fixtures)
│       ├── hooks/          # service: api를 useQuery/useMutation으로 감싸 유스케이스 단위로 노출
│       └── components/     # 해당 도메인 전용 UI 조각 (다른 도메인에서 재사용하지 않음)
├── shared/              # 여러 도메인이 공유하는 것
│   ├── ui/               # 디자인 시스템 컴포넌트 — Tailwind 유틸리티 기반 (Button, Card, Pill, Avatar, Modal, DataTable ...)
│   ├── api/               # 공용 http client, 인증 헤더/401 처리 등 통신 공통 로직
│   ├── stores/             # Zustand 전역 클라이언트 상태 (인증 세션, 알림 패널 열림 여부, 현재 워크스페이스 등)
│   ├── lib/               # 순수 유틸리티(포맷터, 날짜 등), 프레임워크 의존 없음
│   └── config/             # 환경변수 래퍼, 상수, 라우트 경로 상수
└── styles/              # globals.css — Tailwind 지시문 + ui/style.css 디자인 토큰 이식
```

`tailwind.config`(또는 CSS `@theme`)와 `postcss.config`는 `frontend/` 루트에 둔다.

| 폴더 | 역할 (백엔드 레이어 대응) |
| --- | --- |
| `pages` | 라우트 진입점. 여러 `features`의 `hooks`/`components`를 조합해 화면을 구성한다 (presentation) |
| `features/{domain}/components` | 해당 도메인 전용 프레젠테이셔널 컴포넌트 (presentation, 도메인 범위 한정) |
| `features/{domain}/hooks` | 유스케이스 단위로 `api` + `model`을 조합해 화면이 쓸 상태/액션을 제공 (service) |
| `features/{domain}/api` | TanStack Query 기반 HTTP 통신, 서버 DTO ↔ 도메인 모델 변환, 에러를 공용 규약으로 정규화 (infra) |
| `features/{domain}/model` | 도메인 타입, 값 객체, 검증/상태 전이 규칙, 목데이터 (domain) |
| `shared/stores` | Zustand 전역 클라이언트 상태 저장소 (여러 화면이 공유하는 UI/세션 상태) |
| `shared/*` | 특정 도메인에 속하지 않는 공통 도구 (backend의 `common` 패키지에 대응) |

## **의존성 방향**

```text
pages -> features/{domain}/hooks -> features/{domain}/api
                                  -> features/{domain}/model
features/{domain}/components -> features/{domain}/hooks (또는 model 타입만 참조)
(모든 계층) -> shared/*
```

규칙은 다음과 같다.

1. 상위 계층은 하위 계층만 참조한다. `shared`는 어떤 `features`도 참조하지 않는다.
2. `features/{domain}` 간 직접 참조는 하지 않는다. 다른 도메인 데이터가 필요하면
   `pages`(조합 지점)에서 여러 도메인의 `hooks`를 함께 사용하거나, 공용 개념이면 `shared`로 뺀다.
   **예외**: `model` 계층끼리는 실제 도메인 관계(한 엔티티가 다른 도메인의 값을 그대로 포함하는
   경우, 예: `RevisionDictionary.proposedTerms: CandidateTerm[]`)라면 타입만 import할 수 있다.
   여러 도메인이 공유하는 식별자 타입(`MemberId` 등)은 애초에 `shared/types/ids.ts`에 둬서
   이런 참조 자체가 필요 없게 한다. `hooks`/`api`/`components`는 이 예외 없이 여전히 다른
   도메인을 직접 참조하지 않는다.
3. `components`는 `api`를 직접 호출하지 않는다. 데이터 접근은 항상 `hooks`를 통한다 —
   컴포넌트가 통신 기술을 몰라야 목업↔실 API 전환 시 컴포넌트 코드가 바뀌지 않는다.
4. `api` 계층은 서버 응답(원본 DTO)을 그대로 상위로 올리지 않는다. `model`이 정의한
   도메인 타입으로 변환해서 반환한다 (docs/API.md 변경에 대비).
5. `model`에는 React나 통신 기술 의존을 두지 않는다. 순수 타입/함수만 둔다
   (백엔드 domain 레이어의 "Spring·Web 의존 없음" 규칙과 동일한 취지).
6. `shared/ui` 컴포넌트는 도메인 용어(문서, 사전집 등)를 모른다. props로만 데이터를 받는
   순수 프레젠테이셔널 컴포넌트로 유지한다.

## **네이밍 컨벤션 (hooks/api)**

| 역할 | 예시 |
| --- | --- |
| 목록 조회 | `useDocumentList`, `fetchDocuments` |
| 단건 조회 | `useDocumentDetail`, `fetchDocument` |
| 생성 | `useDocumentUpload`, `createDocument` |
| 수정/상태 전이 | `useReviewSubmit`, `updateReviewStatus` |
| 검증 | `validateDictionaryTerm` (model 내부 함수) |

## **상태 관리**

서버 상태(API 데이터)와 클라이언트 UI 상태를 분리해 서로 다른 도구로 관리한다.

| 구분 | 도구 | 위치 | 예시 |
| --- | --- | --- | --- |
| 서버 상태 | TanStack Query | `features/{domain}/api`(query/mutation 함수) + `features/{domain}/hooks`(`useQuery`/`useMutation`) | 문서 목록, 사전집, 리뷰 요청 등 백엔드에서 오는 모든 데이터 |
| 클라이언트 상태 | Zustand | `shared/stores`(전역 공유) 또는 `features/{domain}/stores`(도메인 내부 전용) | 인증 세션(`authStore`), 알림 패널 열림 여부, 현재 선택된 워크스페이스 |

규칙은 다음과 같다.

1. 서버에서 온 데이터를 Zustand 스토어에 복제해서 들고 있지 않는다. TanStack Query 캐시가
   서버 데이터의 유일한 출처(source of truth)다 — 캐시 이중화는 두 상태가 어긋나는 버그로 이어진다.
2. 여러 화면이 공유할 필요가 없는 상태는 Zustand로 올리지 않는다. 화면(또는 컴포넌트) 로컬
   `useState`로 충분하면 그대로 둔다.
3. Zustand 스토어는 순수 클라이언트 개념만 다룬다(로그인 여부, 패널 열림 상태 등). 서버 리소스의
   낙관적 업데이트가 필요하면 Zustand가 아니라 TanStack Query의 캐시 갱신 기능을 사용한다.

## **화면 ↔ 라우트 ↔ 도메인 매핑 (ui/main.js 기준)**

`ui/main.js`의 `state.screen` 키와 `CONTENT_RENDERERS`를 기준으로 한 초안이다. 실제 경로/그룹핑은
구현 단계에서 확정한다.

| `state.screen` (ui/main.js) | 화면 | 라우트 초안 | 담당 도메인(`features`) |
| --- | --- | --- | --- |
| (renderLogin) | 로그인 | `/login` | `auth` |
| (renderWorkspaces) | 워크스페이스 선택 | `/workspaces` | `workspace` |
| `docs` | 문서 목록 | `/workspaces/:workspaceId/documents` | `document` |
| `upload` | 문서 업로드 | `/workspaces/:workspaceId/documents/upload` | `document` |
| `docDetail` | 문서 상세 | `/workspaces/:workspaceId/documents/:documentId` | `document` |
| `docHistory` | 문서 버전 이력 | `/workspaces/:workspaceId/documents/:documentId/history` | `document` |
| `extract` | 용어 추출 | `/workspaces/:workspaceId/documents/:documentId/extract` | `dictionary` |
| `draft` | 사전집 초안 | `/workspaces/:workspaceId/dictionary/draft` | `dictionary` |
| `dictionary` | 사전집 | `/workspaces/:workspaceId/dictionary` | `dictionary` |
| `dictHistory` | 사전집 리비전 이력·비교 | `/workspaces/:workspaceId/dictionary/history` | `dictionary` |
| `reviewDoc` | 문서 검토(대조 결과) | `/workspaces/:workspaceId/documents/:documentId/review` | `review` |
| `reviewThread` | 문서 개정안 리뷰(PR형) | `/workspaces/:workspaceId/documents/:documentId/review/:reviewId` | `review` |
| `revision` | 사전집 개정안 리뷰(PR형) | `/workspaces/:workspaceId/dictionary/revisions/:revisionId` | `review` |
| `settings` | 설정 · 멤버 | `/workspaces/:workspaceId/settings/members` | `member` |
| `settings` | 설정 · 룰셋 | `/workspaces/:workspaceId/settings/ruleset` | `workspace` |
| `settings` | 설정 · 알림 | `/workspaces/:workspaceId/settings/notifications` | `notification` |
| `settings` | 설정 · 라벨 | `/workspaces/:workspaceId/settings/labels` | `document` |

`/settings`는 `SettingsLayout`(탭 네비게이션 + `Outlet`)이 감싸고, 위 4개 하위 라우트로
연결된다(Phase 3에서 확정). 경로 상수는 `src/shared/config/routes.ts` 기준.

**사이드바 퀵링크 관련 결정 (Phase 3)**: `ui/main.js`의 사이드바는 "문서" 그룹에
초안(`reviewDoc`)·개정안(`reviewThread`), "사전집" 그룹에 개정안(`revision`)까지 직접 링크로
뒀지만, 이 화면들은 위 표에서 보듯 특정 `:documentId`/`:revisionId`가 있어야 진입 가능하다.
목록 없이 사이드바에서 바로 연결할 수 없으므로, 실제 구현에서는 사이드바에 목록형 화면
(문서 목록/사전집/사전집 초안/사전집 리비전 이력)만 두고 리뷰류 화면은 그 목록에서
드릴다운으로 진입하는 구조로 바꿨다 (`src/app/Sidebar.tsx` 참고).

## **디자인 시스템 컴포넌트 매핑 (`ui/style.css` → `shared/ui`)**

| `ui/style.css` 클래스 | `shared/ui` 컴포넌트(안) |
| --- | --- |
| `.btn`, `.btn-primary`, `.btn-outline` | `Button` |
| `.card` | `Card` |
| `.pill`, `.tone-*` | `Pill` |
| `.avatar` | `Avatar` |
| `.checkbox`, `.radio-dot` | `Checkbox`, `RadioDot` |
| `.toggle` | `Toggle` |
| `table.dtable` | `DataTable` |
| sidebar/topbar/app-shell | `AppShell`, `Sidebar`, `Topbar` (→ `src/app`) |
| modal | `Modal` |
| pr-thread(comment-thread) | `CommentThread` |
| 알림 패널 | `NotificationPanel` (→ `features/notification/components`) |
| suggestion popover | `SuggestionPopover` (→ `features/review/components`) |

## **리뷰 체크리스트**

- 페이지 컴포넌트가 화면 구성 흐름으로 읽히는가? (여러 `hooks`를 조합만 하고 통신 로직이 없는가)
- `components`가 `api`를 직접 호출하지 않는가?
- `api` 계층이 서버 응답 DTO를 그대로 상위로 올리지 않는가?
- `model`에 React·통신 기술 의존이 들어오지 않았는가?
- `features` 간 직접 참조가 생기지 않았는가?
- `shared/ui` 컴포넌트가 도메인 용어를 모른 채 props만으로 동작하는가?
- 서버에서 온 데이터를 Zustand 스토어에 중복 저장하지 않았는가? (TanStack Query 캐시가 유일한 출처인가)
- 컴포넌트 로컬 state로 충분한 것까지 Zustand 전역 상태로 올리지 않았는가?
