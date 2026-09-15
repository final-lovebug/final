# T-INT-16 — 프론트 환경설정 정리

상태: 완료(2026-09-14) | 담당자: (세션 진행)
근거: `docs/plan/INTEGRATION_PLAN.md` 0절(프론트 상태 — dev 프록시/env 없음)
의존: 없음.

현재 `frontend/.env.local`이 없어 `VITE_API_BASE_URL`이 기본값(`http://localhost:8080`)
으로 떨어진다. 다른 태스크(T-INT-9~14)를 시작하는 사람이 매번 직접 알아내지 않도록
가이드를 남긴다.

## 체크리스트

- [x] `frontend/.env.local.example` 신설
- [x] 로컬 실행 가이드 추가 — 기존 `frontend/docs/guide.md`(`frontend/README.md`가
      가리키는 실행 가이드 문서, 신규 파일이 아니라 여기 있었다)의 "환경 변수" 절에
      `.env.local.example` 복사 안내, `--spring.profiles.active=local` 필수 이유,
      dev 프록시 없이 CORS+credentials로 직접 통신한다는 점을 보강
- [x] dev 프록시 여부 — **현재 방식(직행) 유지로 결정**(프록시 도입 이유 없음, 문서에
      명시)
- [x] `.gitignore`의 `*.local` 패턴이 `.env.local`을 이미 막고, `.env.local.example`은
      `.example`로 끝나 커밋 대상에 포함됨을 확인
- [ ] 커밋 브랜치 `chore/WLSH-{티켓}-fe-env-config`, PR 생성 — **사용자 지시 시 진행**
