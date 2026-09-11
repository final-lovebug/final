// Vite 환경변수 래퍼(frontend/docs/ARCHITECTURE.md "환경변수 래퍼, 상수, 라우트 경로 상수").
// `.env.local`(git 커밋 금지)에 VITE_API_BASE_URL을 지정하면 그 값을 쓰고, 없으면 로컬
// 백엔드 기본 포트(8080, backend/CLAUDE.md 참고)로 접속한다.
export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
