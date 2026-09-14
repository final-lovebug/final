# CLAUDE.md

이 문서는 Frontend 프로젝트 작업 시 따라야 하는 규칙이다.

---

## 기술 스택

> **버전은 이 문서에 적지 않는다.** 정확한 버전은 `frontend/package.json`이 기준이다.
> 왜 이 조합인지·결정 이력은 `frontend/docs/SPEC.md`를 본다.

- **프레임워크** — Vite + React (순수 SPA, Next.js 아님)
- **언어** — TypeScript
- **패키지 매니저** — npm
- **스타일링** — Tailwind CSS
- **상태관리** — TanStack Query(서버 상태) + Zustand(클라이언트 상태)
- **라우팅** — react-router-dom (`createBrowserRouter`)
- **아키텍처** — 도메인별 `features/{domain}` + `shared`, 페이지는 `pages/`
- **린트** — `oxlint` (ESLint 아님). 포매터(Prettier 등)는 도입하지 않음

### Tailwind 주의사항

- 설치된 버전은 v4다. **`tailwind.config.js`가 없다** — 커스텀 토큰(색상/반경/폰트/그림자)은
  `src/index.css`의 `@theme` 블록에 있다. Tailwind v3 기준 예제(`tailwind.config.js`에
  `theme.extend` 채우는 방식)를 그대로 베끼지 않는다.
- 토큰 이름과 실제 값의 기준은 `src/index.css`, 매핑 근거는 `frontend/docs/ARCHITECTURE.md`
  "상태 관리"·"디자인 시스템 컴포넌트 매핑" 절 참고.

### 기존 파일 수정 시 주의

- **기존 fixtures/타입 파일에 내용을 추가할 때는 Write로 새로 쓰지 말고 Edit을 쓴다.**
  Phase 6 작업 중 `features/workspace/model/fixtures.ts`에 새 내용을 Write로 써 넣다가
  이미 있던 `WORKSPACE_FIXTURES`를 지울 뻔한 사고가 있었다(빌드 에러로 바로 발견해 복구함).
  파일 전체를 새로 만드는 게 아니라면 항상 먼저 읽고 필요한 부분만 Edit으로 추가한다.
- 목업 데이터(`features/{domain}/model/*.ts`)는 여러 화면이 같은 항목을 참조하므로,
  구조를 바꾸기 전에 그 데이터를 쓰는 다른 화면이 있는지 먼저 확인한다.

---

## 개발 명령어

모든 명령어는 `frontend/` 디렉터리에서 실행한다.

```bash
npm install
npm run dev        # http://localhost:5173
npm run build
npm run preview
```

상세 실행 방법(환경변수, 목데이터 동작 범위 등)은 `frontend/docs/guide.md` 참고.

---

## 백엔드 연동 상태 (2026-09-10 기준)

이번 1차 프론트엔드 구축(fe-task.md Phase 1~6)은 **백엔드 실 연동 없이 전부 목데이터로
만들어졌다** (`frontend/docs/SPEC.md` "백엔드 연동 범위" 참고). 그 사이 백엔드에는 실제로
아래 도메인이 구현됐다 — 실 연동을 시작한다면 이 셋부터가 가장 준비돼 있다.

| 도메인 | 엔드포인트 | 상태 |
| --- | --- | --- |
| Auth | `POST /api/auth/oauth/google/exchange`, `POST /api/auth/refresh`, `POST /api/auth/logout` | 구현 완료 |
| Member | `GET/PATCH/DELETE /api/members/me`, `POST /api/members`(테스트용) | 구현 완료 |
| Workspace | `POST/GET /api/workspaces`, `GET/PATCH/DELETE /api/workspaces/{id}` | 구현 완료. **단, 인증 연동 전이라 `memberId`를 쿼리 파라미터로 받는 임시 방식**(백엔드가 실제 인증 연동을 마치면 제거될 예정) |

문서(document)·용어 추출·사전집(dictionary)·리뷰(review)·알림(notification) 도메인은
아직 API 엔드포인트 자체가 없다. 정확한 요청/응답 필드는 `docs/API.md`를 기준으로 한다
(2026-09-10 기준 실제 구현과 일치 확인됨).

### 로컬 백엔드로 실제 연동을 테스트할 때 알아야 할 것

- 백엔드는 `./gradlew bootRun --args='--spring.profiles.active=local'`로 **8080 포트**에서
  실행한다 (`backend/CLAUDE.md` 참고). `local` 프로파일 없이 띄우면 refresh 토큰 쿠키의
  `Secure` 플래그가 `true`로 고정돼 `http://localhost`에서 쿠키가 동작하지 않는다.
- **백엔드에 CORS 설정이 아직 없다.** 프론트 dev 서버(예: `localhost:5173`)에서
  `http://localhost:8080/api/**`를 직접 호출하면 브라우저가 막는다. 실 연동 전에 다음 중
  하나가 필요하다.
  - 백엔드에 `CorsConfigurationSource` 추가 — refresh 토큰이 쿠키 기반이라
    `Access-Control-Allow-Origin: *`는 못 쓰고, 프론트 origin을 명시하면서
    `Access-Control-Allow-Credentials: true`도 같이 켜야 한다.
  - 또는 프론트 dev 서버에 프록시 설정 (`vite.config.ts`의 `server.proxy`로 `/api` →
    `http://localhost:8080`), 이 경우 브라우저 입장에서는 같은 origin이라 CORS 자체가 필요 없다.
- 백엔드 env `OAUTH_FRONTEND_REDIRECT_URI`(기본값 `http://localhost:3000/oauth/callback`)는
  CORS 허용 목록이 아니라 **"Google 로그인 성공 후 리다이렉트할 프론트 주소"**다. 실제 프론트
  개발 서버 주소/콜백 라우트가 정해지면 백엔드 팀에 알려 이 값을 맞춰야 한다 (지금 이 저장소
  라우터에는 `/oauth/callback` 라우트가 아직 없다 — 아래 체크리스트 참고).
- 로컬 전용 `POST /api/auth/dev/login` `{ memberId }` (local 프로파일에서만 존재)로 Google
  OAuth 없이 바로 토큰을 받을 수 있다 — 프론트 개발 중 로그인 흐름을 매번 타지 않아도 된다.

### 실 연동을 시작할 때 처음 할 일 (체크리스트)

1. 백엔드 CORS 또는 프론트 dev 프록시 중 하나를 정하고 적용
2. `OAUTH_FRONTEND_REDIRECT_URI`와 맞는 콜백 라우트(예: `/oauth/callback`)를 라우터에 추가 —
   쿼리의 `code`를 받아 `POST /api/auth/oauth/google/exchange` 호출
3. `src/shared/stores/authStore.ts`의 `loginAsMock()`을 실제 토큰 교환 로직으로 교체,
   accessToken은 지금처럼 메모리(Zustand)에만 보관(localStorage 금지 — XSS 노출 방지)
4. `src/shared/api/httpClient.ts`(아직 없음)를 신설해 `Authorization` 헤더 부착과
   `AUTH_TOKEN_EXPIRED` 시 `/api/auth/refresh` 자동 재시도를 공통 처리
5. `features/workspace/api`, `features/member/api`(아직 없음, 이번엔 설정 화면에서 멤버 목록만
   목업으로 조회)의 목업 함수들을 실제 `fetch` 호출로 교체 — 반환 타입을 그대로 유지하면
   `hooks`/`components`는 손댈 필요가 없다(`frontend/docs/ARCHITECTURE.md` 의존성 규칙)
6. `Workspace.myPermission`(OWNER/ADMIN/REGULAR)이 실제로 내려오기 시작하므로, 지금 목업에
   없던 권한 기반 UI 분기(예: ADMIN 이상만 용어 추출·워크스페이스 설정 가능)를 추가

---

## 참고 문서

- 기술 스택·결정 이력: `frontend/docs/SPEC.md`
- 폴더 구조·레이어 규칙: `frontend/docs/ARCHITECTURE.md`
- 실행 방법: `frontend/docs/guide.md`
- 작업 절차·진행 상태: `frontend/task/fe-task.md` (개인 작업 추적용, git 추적 제외)
- 백엔드 API 규격: `docs/API.md`
- 백엔드 도메인 모델: `docs/DOMAIN.md`
- 백엔드 개발 규칙: `backend/CLAUDE.md`
