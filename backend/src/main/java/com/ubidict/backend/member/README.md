# Member 도메인 — 로컬 로그인 테스트 가이드 (임시)

> **이 문서는 프론트엔드가 완성돼 실제 회원가입/로그인 화면으로 테스트할 수 있게 되면 통째로
> 삭제합니다.** 그 전까지 백엔드만으로(또는 팀원이 처음 로그인을 시도할 때) 로그인 플로우를
> 확인하기 위한 임시 가이드입니다.

## 0) 다른 도메인(Workspace 등)만 테스트하고 싶다면 — Google 로그인 없이 바로 토큰 받기

**Member/로그인 자체가 아니라 자기 도메인 API만 테스트하려는 거라면, 아래 Google 로그인 절차를
거칠 필요가 없습니다.** `local` 프로필에서만 열리는 `POST /api/auth/dev/login`으로 이미 존재하는
회원 id를 넣으면 바로 토큰이 나옵니다(Google Test User 등록, OAuth 왕복 전부 불필요).

```bash
# 1. 테스트용 회원이 없다면 먼저 하나 만든다 (관리자/테스트용 엔드포인트)
curl -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{"email":"tester@example.com","displayName":"테스터","provider":"GOOGLE","providerId":"tester-1"}'
# 응답의 memberId를 기억해둔다 (예: 1)

# 2. 그 memberId로 바로 로그인해 토큰을 받는다
curl -i -X POST http://localhost:8080/api/auth/dev/login \
  -H "Content-Type: application/json" \
  -d '{"memberId": 1}'
```

응답 형식은 실제 로그인(`/api/auth/oauth/google/exchange`)과 완전히 동일합니다 —
`accessToken`/`role`은 본문으로, refresh token은 `Set-Cookie`로 내려옵니다. 이후 그대로
`Authorization: Bearer {accessToken}`으로 자기 도메인 API를 호출하면 됩니다. Swagger UI를 쓴다면
받은 accessToken을 우측 상단 **Authorize** 버튼에 붙여넣으면 이후 "Try it out" 호출마다 자동으로
헤더가 실립니다.

> `local` 프로필에서만 존재하는 엔드포인트라 배포 환경엔 아예 없습니다. 회원의 권한(role)은
> DB에 저장된 값을 그대로 쓰고 이 엔드포인트가 바꿔주지 않습니다 — 관리자 권한이 필요한 API를
> 테스트하려면 로컬 DB에서 직접 해당 회원의 role을 `ADMIN`으로 바꿔야 합니다(운영과 동일한 정책,
> `docs/DOMAIN.md`).

## 1) 실제 Google 로그인 플로우 자체를 테스트해야 한다면

Member/로그인 도메인을 직접 개발 중이라 Google OAuth 콜백·교환 코드 등 실제 흐름을 확인해야
한다면 아래를 따르세요.

### 왜 필요한가

Google OAuth 동의 화면이 아직 **Testing** 상태입니다. 이 상태에서는 **"Test users"로 등록된
Google 계정만** 로그인할 수 있고, 그 외 계정은 아래처럼 막힙니다.

```
액세스 차단됨: 승인 오류
{계정 이메일}
The OAuth client was not found. / 이 앱은 확인되지 않았습니다 등
401 오류: invalid_client (또는 access_denied)
```

즉 팀원이 로그인을 테스트하려면, **프로젝트 담당자가 그 팀원의 Google 계정을 먼저 Test user로
등록**해줘야 합니다.

### 1-1) 팀원을 Test user로 등록하는 방법 (Google Cloud Console 접근 권한 있는 담당자만 가능)

1. [Google Cloud Console](https://console.cloud.google.com) 접속 후 이 프로젝트 선택
2. 좌측 메뉴 **APIs & Services > OAuth consent screen** 이동
3. 화면 하단(또는 "Audience"/"Test users" 탭)의 **Test users** 섹션에서 **ADD USERS** 클릭
4. 추가할 팀원의 **Google 계정 이메일**을 입력하고 저장 (한 번에 여러 명 입력 가능, 최대 100명)
5. 등록된 팀원은 별도 승인 절차 없이 바로 로그인 테스트가 가능합니다 (앱을 Publish할 필요 없음)

> Test users는 Google 계정 소유자 본인이 스스로 추가할 수 없습니다 — 반드시 프로젝트
> 담당자에게 본인 Gmail 주소를 알려주고 등록을 요청하세요.

### 1-2) 로컬 환경변수 설정

팀이 공유하는 `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` 값을 담당자에게 전달받아 설정합니다
(저장소에는 커밋되지 않으므로 직접 문의해야 합니다). 자세한 변수 목록은 `backend/CLAUDE.md`의
환경변수 표를 참고하세요.

- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

### 1-3) 실행

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

`--spring.profiles.active=local`을 빼먹으면 refresh token 쿠키의 `Secure` 플래그가 켜져서
`http://localhost`에서 브라우저가 쿠키를 돌려받지 못합니다.

IntelliJ로 실행한다면 Run Configuration의 **Environment variables**에 위 두 값을 넣고 **그 Run
설정을 직접 실행**하세요 — 터미널에서 별도로 `./gradlew bootRun`을 실행하면 IntelliJ에 등록한
환경변수가 적용되지 않습니다(서로 다른 프로세스라서).

### 1-4) 로그인 플로우 확인

1. 브라우저로 `http://localhost:8080/oauth2/authorization/google` 접속
2. Google 로그인 → 동의 화면 통과 (Test user로 등록된 계정이어야 함)
3. 프론트가 아직 없어서 `http://localhost:3000/oauth/callback?code=...`에서 "연결할 수 없음" 에러
   페이지가 뜹니다 — **정상입니다.** 주소창의 `code=` 값을 **30초 안에** 복사합니다(1회용, TTL 30초).
4. 복사한 code로 교환 API를 호출해 토큰 발급을 확인합니다.

   ```bash
   curl -i -X POST http://localhost:8080/api/auth/oauth/google/exchange \
     -H "Content-Type: application/json" \
     -d '{"code":"<위에서 복사한 code>"}'
   ```

   성공하면 `200 OK`와 함께 `accessToken`/`role`이 응답 본문으로, refresh token이 `Set-Cookie`로
   내려옵니다.
5. 받은 `accessToken`으로 인증이 필요한 API를 호출해 확인합니다.

   ```bash
   curl -H "Authorization: Bearer <accessToken>" http://localhost:8080/api/members/me
   ```

재발급(`POST /api/auth/refresh`)·로그아웃(`POST /api/auth/logout`)을 포함한 전체 API 스펙은
`docs/API.md`의 Auth API / Member API 절을 참고하세요.
