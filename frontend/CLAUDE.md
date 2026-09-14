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

## 백엔드 연동 상태 (2026-09-14 기준)

**목데이터는 전부 걷어냈다.** 1차 구축(fe-task.md Phase 1~6)은 `fixtures.ts` + `delay()`로만
만들어졌지만, 트랙 A(`T-INT-9`~`T-INT-16`)·`T-INT-17`·디자인 정합 작업을 거쳐 **모든 화면이
실 API를 쓴다.** `features/*/model/fixtures.ts`에 남은 것은 목데이터가 아니라 **뷰 타입**
(여러 엔드포인트를 화면이 쓰는 모양으로 합친 것)과 고정 정책 상수뿐이다.

### 대응 백엔드가 없어 화면이 「읽기 전용」인 곳

억지로 목업을 만들지 않고, 지금 실제로 어떻게 동작하는지를 보여주기로 한 자리들이다.

| 화면 | 없는 것 | 화면이 하는 일 |
| --- | --- | --- |
| 설정 › 알림 | 알림 설정 조회·저장 API 전부(`D-54`가 MVP1에서 채널 개념을 제거) | 고정 정책(전 유형 인앱 발송)을 읽기 전용으로 표시 |
| 설정 › 승인 규칙 | 「승인 무효화」·「작성자도 리뷰어」 토글에 해당하는 `RuleSet` 필드 | 잠근 채 "변경 불가" 사유 표기. 정족수 둘만 실제로 저장 |
| 설정 › 라벨 | 라벨 생성·이름변경·삭제 API(문서 생성/수정의 `labels`로만 생긴다) | 목록 + 문서 수(문서 목록에서 집계) + 문서 업로드로 안내 |
| 사전집 용어 「정의」 칸 | 목록 응답에 `definition`이 없다(`D-41`, 단건 조회도 없음) | "—" 표시 |
| 문서 「최종 수정자」 칸 | 문서 응답에 최종 수정자 필드가 없다 | "—" 표시 |
| 개정 이력 타임라인 | `revisionlog` 도메인에 presentation 패키지가 없다(REST 미노출) | 버전 목록 + 버전별 용어로 **직접 만든다**(diff 포함) |
| 문서 버전 비교 | 백엔드가 본문 diff를 만들지 않는다(`D-61`) | 버전 단건 조회로 본문을 받아 화면에서 계산(`model/textDiff.ts`) |

### 로컬 백엔드로 연동을 테스트할 때

- 백엔드는 `./gradlew bootRun --args='--spring.profiles.active=local'`로 **8080 포트**에서
  실행한다(`backend/CLAUDE.md`). `local` 프로파일 없이 띄우면 refresh 토큰 쿠키의 `Secure`
  플래그가 `true`로 고정돼 `http://localhost`에서 쿠키가 동작하지 않는다.
- **CORS는 백엔드에 이미 있다** — `app.cors.allowed-origins` 기본값이
  `http://localhost:5173`(Vite dev 서버)이다. 프론트 주소를 바꾸면 그 환경변수도 함께 바꾼다.
  refresh 토큰이 쿠키 기반이라 `*`는 쓸 수 없다.
- 프론트가 부를 백엔드 주소는 `VITE_API_BASE_URL`이며, 없으면 `http://localhost:8080`이다
  (`.env.local.example` 참고).
- `OAUTH_FRONTEND_REDIRECT_URI`는 Google 로그인 성공 후 서버가 되돌려보낼 프론트 주소다 —
  이 저장소의 콜백 라우트는 `/oauth/callback`이다.
- 추출·대조는 **외부 FastAPI 워커**가 실행한다(`D-66`). 워커가 없는 환경
  (`app.ai.dispatch.mode=in-process`, 로컬 기본값)에서는 대역이 **빈 결과로 작업을 끝낸다** —
  "성공했는데 후보/제안 0건"이 정상이며 두 화면이 그 문구를 따로 갖고 있다.

---

## 참고 문서

- 기술 스택·결정 이력: `frontend/docs/SPEC.md`
- 폴더 구조·레이어 규칙: `frontend/docs/ARCHITECTURE.md`
- 실행 방법: `frontend/docs/guide.md`
- 작업 절차·진행 상태: `frontend/task/fe-task.md` (개인 작업 추적용, git 추적 제외)
- 백엔드 API 규격: `docs/API.md`
- 백엔드 도메인 모델: `docs/DOMAIN.md`
- 백엔드 개발 규칙: `backend/CLAUDE.md`
