# **Frontend Tech Spec**

## **목표**

이 문서는 프론트엔드가 **무엇으로 만들어지는지(기술 스택)**와 **왜 그렇게 정했는지(결정 이력)**를 기록하는
단일 기준 문서다. 역할을 이렇게 나눈다.

| 문서 | 담당 |
| --- | --- |
| `frontend/docs/SPEC.md` (이 문서) | 기술 스택, 범위, 결정 이력 |
| `frontend/docs/ARCHITECTURE.md` | 폴더 구조, 레이어 규칙, 네이밍 컨벤션 |
| `frontend/docs/guide.md` | 설치·실행·빌드 방법 |
| `frontend/task/fe-task.md` | 실행 순서(Setup Order)와 진행 상태 체크리스트 |

## **기술 스택**

| 영역 | 선택 | 설치된 버전 | 비고 |
| --- | --- | --- | --- |
| 프레임워크 | Vite + React (순수 SPA) | Vite 8.2.2 / React 19.2.8 | 최초 Next.js로 합의했다가 2026-09-10 SPA로 변경 |
| 언어 | TypeScript | 6.0.2 | |
| 패키지 매니저 | npm | 11.14.1 | |
| 라우팅 | `react-router-dom` | 7.18.3 | |
| 스타일링 | Tailwind CSS | 4.3.3 | `@tailwindcss/vite` 플러그인 방식(CSS `@import` 기반, `tailwind.config.js` 불필요).
  `ui/style.css` 디자인 토큰을 Tailwind theme로 이식하는 작업은 Phase 2 |
| 상태관리 — 서버 상태 | TanStack Query (`@tanstack/react-query`) | 5.102.8 | |
| 상태관리 — 클라이언트 상태 | Zustand | 5.0.15 | |
| 린트 | `oxlint` | 1.79.0 | Vite `react-ts` 템플릿 기본값. 포매터가 아니라 린터라 "포매터 미도입" 결정과 무관 |
| 테스트 | **도입하지 않음** | — | 루트 `CLAUDE.md`의 "새 로직에는 테스트를 함께 작성한다" 원칙과 상충 — 필요해지면 재검토 |
| 포매터 | **도입하지 않음** (Prettier 등) | — | `oxlint` 기본 설정만 사용 |
| Node 버전 고정 | `.nvmrc` + `package.json` `engines` | Node 24.14.1(`>=24`) | 부트스트랩 시점 Active LTS 버전으로 고정 |

버전은 2026-09-10 Phase 1 부트스트랩 실행 시점의 npm 레지스트리 최신 안정판 기준이며,
정확한 값은 항상 `frontend/package.json`을 기준으로 한다(이 표는 스냅샷).

## **상태관리 근거**

2025~2026 실무에서는 서버 상태(API 데이터)와 클라이언트 UI 상태를 분리해 서로 다른 도구로
관리하는 것이 표준적 접근으로 확인됐다 — Zustand가 신규 프로젝트 다운로드 기준 Redux Toolkit을
추월했고, TanStack Query와 조합하는 패턴이 다수 레퍼런스에서 공통적으로 확인됨.

- [Zustand and TanStack Query: The Dynamic Duo](https://medium.com/@contato.blense/zustand-and-tanstack-query-the-dynamic-duo-that-simplified-my-react-state-management-486c3073f81c)
- [React State Management in 2026](https://vucense.com/dev-corner/react-state-management-2026/)

폴더 배치와 사용 규칙(서버 데이터를 Zustand에 복제 저장하지 않는다 등)은
`frontend/docs/ARCHITECTURE.md`의 "상태 관리" 절을 따른다.

## **범위**

`ui/` 프로토타입(index.html/style.css/main.js/data.js) 기준 **12개 화면 전체 포팅**.
(`docs/REQUIREMENTS.md` `NFR-CMN-005`, 2026-09-10 "핵심 3화면 우선" 제약 해제 후 범위 확대)

## **백엔드 연동 범위 (중요)**

- 이번 1차 프론트엔드 구축 작업에서는 **백엔드 실 연동을 진행하지 않는다.**
- 모든 화면은 `ui/data.js`를 이전한 **목데이터로 골격과 동작을 구현**한다. 로그인(Google OAuth)도
  이번 단계에서는 목업으로 처리한다.
- `features/{domain}/api` 계층은 실제 백엔드 호출과 동일한 함수 시그니처(반환 타입 = Phase 4에서
  정의한 도메인 모델)를 가진 **목업 구현체**로 채운다. 이렇게 하면 이후 담당 개발자가 목업 구현체만
  실제 API 호출로 교체하면 되고, `hooks`/`components`는 손댈 필요가 없다 — 이 경계는
  `frontend/docs/ARCHITECTURE.md`의 의존성 규칙(컴포넌트는 `api`를 직접 호출하지 않는다)이 이미
  보장한다.
- **향후 실 연동 시 담당 개발자가 참고할 것**: 인증 흐름·에러 코드 등은 `docs/API.md`(Auth API만
  정의됨), 도메인 엔티티는 `docs/DOMAIN.md`. 단, `docs/DOMAIN.md`는 `Document` 라벨 속성과
  `Label` 엔티티, `Document`의 outdated 판별 속성을 아직 "미확정"으로 표시하고 있으므로, 문서
  라벨 필터·설정 > 라벨 관리 화면은 이 모델이 정해진 뒤에야 실 데이터로 연동 가능하다.
  그 외 워크스페이스·문서·용어 추출·사전집·리뷰·알림 도메인의 API 엔드포인트 자체가
  `docs/API.md`에 아직 정의돼 있지 않다는 점도 함께 인지해야 한다.

## **결정 이력**

| 날짜 | 결정 |
| --- | --- |
| 2026-09-10 | 최초 합의: Next.js(App Router) / TypeScript / npm / 12화면 전체 포팅 |
| 2026-09-10 | 프레임워크를 Next.js → Vite+React 순수 SPA로 변경 |
| 2026-09-10 | Tailwind CSS 채택, 테스트 라이브러리 미도입, TanStack Query+Zustand 채택 |
| 2026-09-10 | `react-router-dom` 채택, Node 버전 `.nvmrc`+`engines`, Prettier 등 포매터 미도입 |
| 2026-09-10 | `docs/REQUIREMENTS.md` `NFR-CMN-005`·`NFR-INF-006` 루트 문서에 직접 반영 완료 |
| 2026-09-10 | 백엔드 실 연동은 이번 작업 범위에서 제외, 목데이터 기반 골격/동작만 구현하기로 결정 |
| 2026-09-10 | Phase 1(프로젝트 부트스트랩) 완료 — 위 표의 버전으로 실제 설치·빌드 검증까지 완료 |

## **관련 문서**

- 작업 절차·진행 상태: `frontend/task/fe-task.md`
- 폴더 구조·레이어 규칙: `frontend/docs/ARCHITECTURE.md`
- 실행 방법: `frontend/docs/guide.md`
- 루트 요구사항: `docs/REQUIREMENTS.md` (`NFR-CMN-005`, `NFR-INF-006`)
- API 설계 초안(Auth만 정의됨): `docs/API.md`
- 도메인 모델(Label 등 일부 미확정): `docs/DOMAIN.md`
