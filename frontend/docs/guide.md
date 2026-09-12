# **Frontend 실행 가이드**

## **현재 상태 안내**

✅ `frontend/task/fe-task.md`의 **Phase 1~5가 완료됐다** (2026-09-10). `/login`에서 목업
로그인 버튼을 누르면 `/workspaces`로 이동해 목데이터로 만든 워크스페이스 목록을 실제로
불러온다(TanStack Query + Zustand 인증 가드까지 연결됨). 다만 그 이후 화면(문서/사전집/리뷰/
설정)은 아직 `PagePlaceholder` 스텁이다 — 화면별 목데이터 연결과 실제 UI는 Phase 6에서 채운다.

기술 스택 자체(왜 Vite/React/Tailwind인지 등)는 `frontend/docs/SPEC.md`를 참고한다. 이 문서는
"어떻게 켜고 끄는지"만 다룬다.

## **사전 요구사항**

- Node.js — 저장소의 `frontend/.nvmrc`에 명시된 버전 (nvm 사용 시 `nvm use`로 자동 전환)
- npm (Node.js에 기본 포함)

## **설치**

```bash
cd frontend
npm install
```

## **개발 서버 실행**

```bash
npm run dev
```

- 기본적으로 Vite 개발 서버가 `http://localhost:5173`에서 뜬다 (포트 충돌 시 자동으로 다른 포트 사용).
- 별도로 백엔드 서버를 띄우지 않아도 실행된다. `/login` → 목업 로그인 → `/workspaces`까지는
  실제 데이터 흐름(TanStack Query 목업 API)이 동작하고, 그 이후 화면은 아직 스텁이다.

## **빌드**

```bash
npm run build
```

- `dist/` 폴더에 정적 파일이 생성된다. `docs/REQUIREMENTS.md` `NFR-INF-006` 기준 S3+CloudFront 같은
  정적 호스팅에 그대로 배포하는 것을 전제로 한다.

## **빌드 결과 미리보기**

```bash
npm run preview
```

- 로컬에서 `dist/` 빌드 결과를 실제 배포와 유사한 방식으로 확인할 때 사용한다.

## **지금은 목데이터로 동작한다**

`frontend/docs/SPEC.md`의 "백엔드 연동 범위"에 정리했듯, 이번 단계는 **백엔드 실 연동 없이** 화면
골격과 동작만 완성한다.

- 모든 화면 데이터는 `ui/data.js`를 이전한 목데이터에서 온다. 로그인도 실제 Google OAuth 없이 목업으로
  처리된다.
- 실제 연동이 필요해지면 `src/features/{domain}/api/`의 목업 구현체만 실제 API 호출(`fetch`/http
  client)로 교체하면 된다. 함수 시그니처(입출력 타입)를 그대로 유지하면 `hooks`·`components`는
  수정할 필요가 없다 — 이 경계는 `frontend/docs/ARCHITECTURE.md`의 레이어 규칙(컴포넌트는 `api`를
  직접 호출하지 않는다)이 보장한다.
- 연동 시 참고할 문서: 인증/에러 코드 규약은 `docs/API.md`(Auth API만 정의돼 있고 나머지 도메인
  엔드포인트는 미정), 도메인 모델은 `docs/DOMAIN.md`(단, `Label` 엔티티와 `Document`의 라벨·outdated
  속성은 아직 "미확정"으로 표시돼 있어 이 부분은 모델이 먼저 정해져야 한다).

## **환경 변수 (실제 백엔드 연동 시에만 필요)**

지금 단계(목데이터 기반)에서는 환경 변수 설정이 필요 없다. 이후 실제 연동 시:

```bash
# frontend/.env.local (커밋 금지 — 루트 CLAUDE.md 준수)
VITE_API_BASE_URL=http://localhost:8080
```

- 로컬 백엔드는 `backend/CLAUDE.md` 기준 `8080` 포트에서 `./gradlew bootRun`으로 실행한다.
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## **관련 문서**

- 기술 스택·결정 이력: `frontend/docs/SPEC.md`
- 폴더 구조·레이어 규칙: `frontend/docs/ARCHITECTURE.md`
- 작업 절차·진행 상태: `frontend/task/fe-task.md`
