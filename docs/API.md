**API Convention**

## **목표**

이 문서는 백엔드가 제공하는 HTTP API의 **요청/응답 규격**을 기록한다. 프론트엔드 연동, 신규 입사자 온보딩, API 변경 리뷰의 기준 문서로 사용한다.

API는 `docs/ARCHITECTURE.md`의 레이어 규칙을 따른다. 요청/응답 DTO는 presentation 레이어에서만 정의하고, service는 command/result 모델로 소통한다. 에러 응답 형식은 `docs/EXCEPTION.md`를 따른다. 도메인 모델이 JPA 엔티티를 겸하므로, 엔티티가 응답으로 새어 나가는 것을 레이어 규칙이 막아 주지 않는다. 아래 공통 규칙으로 명시적으로 금지한다.

## **공통 규칙**

- 기본 경로 접두사는 `/api`다.
- 요청/응답 본문은 모두 `application/json`이다.
- 성공 응답의 HTTP 상태 코드는 유스케이스 의미에 맞춘다. 리소스 생성은 `201 Created`, 조회는 `200 OK`를 사용한다.
- 요청 형식 검증 실패는 `400 Bad Request`와 `COMMON_INVALID_REQUEST` 코드로 내려간다.
- 비즈니스 규칙 위반은 도메인 `ErrorCode`에 정의된 상태 코드와 코드로 내려간다.
- 인증 사용자 식별자(`userId`)는 인증 계층에서 해석해 컨트롤러로 전달한다. 요청 본문에 담지 않는다.
- 도메인 모델을 응답 본문으로 직접 직렬화하지 않는다. presentation은 service가 반환한 result 모델만 응답 DTO로 변환한다.
- service의 result 모델에도 도메인 모델을 담지 않는다. 필요한 값만 옮긴 record로 만든다.
- 요청 본문을 도메인 모델에 바인딩하지 않는다. 요청 DTO → command로만 들어온다.

## **에러 응답 형식**

모든 에러는 다음 형식을 사용한다. (상세 규칙은 `docs/EXCEPTION.md` 참고)

```json
{
  "code": "COMMON_RESOURCE_NOT_FOUND",
  "message": "요청한 리소스를 찾을 수 없습니다."
}
```

---

# **Auth API**

로그인과 토큰 수명 관리를 담당한다. 관련 도메인은 `member`다.

## **인증 방식**

로그인하면 **access token**과 **refresh token**을 발급한다. 인증이 필요한 API는 access token을 `Authorization` 헤더에 담아 호출한다.

```
Authorization: Bearer {accessToken}
```

| **토큰** | **수명** | **용도** |
| --- | --- | --- |
| access token | 30분 | API 호출 인증 |
| refresh token | 7일 | access token 재발급 |

- **refresh token은 서버에 저장된다.** 저장되어 있는지가 곧 유효한지이며, 재발급·로그아웃 시 폐기된다.
- **재발급하면 refresh token도 함께 교체된다(rotation).** 재발급에 쓴 토큰은 무효가 되므로, 클라이언트는 응답으로 받은 새 refresh token으로 반드시 갈아끼워야 한다.
- **재발급에는 10초의 유예 시간이 있다.** 여러 탭이 같은 refresh token으로 동시에 재발급을 요청해도 모두 성공하며, 각 요청은 서로 다른 토큰 쌍을 받는다. 유예 시간이 지난 뒤 같은 토큰을 다시 쓰면 `AUTH_TOKEN_INVALID`로 거절된다.
- **한 계정의 활성 세션은 하나다.** 다시 로그인하면 이전 기기의 refresh token이 폐기되고, 그 기기는 다음 재발급 시점에 로그아웃된다.
- `/api/auth/**`는 모두 인증 없이 호출한다. 재발급과 로그아웃은 access token이 이미 만료된 상태에서 호출되므로 인증을 요구하지 않는다.

> refresh token을 해싱하는 과정(로그인·재발급·로그아웃 모두 해당)에서 서버 환경이 해시 알고리즘(SHA-256)을 지원하지 않으면 `500 AUTH_TOKEN_HASH_FAILED`를 반환한다. 정상 배포 환경에서는 발생하지 않는 방어적 오류다.

### **인증 실패 응답**

인증이 필요한 API에서 토큰이 유효하지 않으면 모두 `401 Unauthorized`로 내려가며, `code`로 원인을 구분한다.

| **상황** | **code** | **클라이언트 처리** |
| --- | --- | --- |
| `Authorization` 헤더가 없거나 비어 있음 | `AUTH_TOKEN_MISSING` | 로그인 화면으로 유도 |
| `Bearer ` 형식이 아니거나 서명이 맞지 않음 | `AUTH_TOKEN_INVALID` | 저장된 토큰을 버리고 재로그인 |
| 토큰에 권한(`role`)이 담기지 않음 | `AUTH_TOKEN_INVALID` | 저장된 토큰을 버리고 재로그인 |
| 토큰이 만료됨 | `AUTH_TOKEN_EXPIRED` | 재발급을 시도하고, 실패하면 재로그인 |

> 권한 도입 전에 발급된 토큰은 `role`이 없어 무효로 처리한다. 권한 배포 직후 한 번 전원 재로그인이 필요하다는 뜻이며, 그 이후에는 발생하지 않는다.

### **권한**

토큰에는 사용자 권한(`role`)이 함께 담긴다. `REGULAR`(일반 회원) 또는 `ADMIN`(관리자)이며, 로그인 응답으로도 내려준다.

- **관리자 전용 API는 `/api/admin/**` 경로를 쓴다.** 인증을 통과했더라도 `role`이 `ADMIN`이 아니면 `403 Forbidden` `AUTH_FORBIDDEN`을 반환한다.
- **권한 승격은 API로 제공하지 않는다.** 관리자 지정은 운영 DB에서 직접 수행한다. → `docs/DOMAIN.md` 권한 정책
- 재발급 시 권한은 토큰이 아니라 저장된 값을 다시 읽어 담는다. 따라서 권한 변경은 늦어도 access token 수명(30분) 안에 반영된다.

---

## **Google 로그인**

Google OAuth2(Authorization Code Flow)로 로그인한다. 별도의 회원가입 API는 없다 — 최초 로그인 시 회원이 자동 생성된다.

### **로그인 시작**

```
GET /oauth2/authorization/google
```

- 브라우저를 이 경로로 이동시키면 Google 동의 화면으로 리다이렉트된다.
- 인증이 필요 없다.

### **콜백 및 토큰 교환**

Google 인증이 끝나면 서버가 프론트엔드로 리다이렉트하면서 1회용 교환 코드(`code`)를 쿼리 파라미터로 전달한다(`{프론트 URL}?code=...`). 프론트는 이 코드로 토큰을 교환한다.

- Google 로그인 자체가 실패하면(동의 거부 등) 교환 코드 대신 `{프론트 URL}?error=oauth_failed`로 리다이렉트한다.

```
POST /api/auth/oauth/google/exchange
```

- 성공 시 `200 OK`. Access token은 응답 본문으로, refresh token은 `Set-Cookie`로 내려간다.
- refresh token 쿠키 속성: `HttpOnly`, `Secure`(로컬 프로필에서만 꺼짐), `SameSite=Strict`, `Path=/api/auth` — `/api/auth/**` 밖에서는 전송되지 않는다.
- 인증이 필요 없다.

#### Request Body

```json
{
  "code": "a1b2c3d4-..."
}
```

| **필드** | **타입** | **제약** | **설명** |
| --- | --- | --- | --- |
| `code` | String | 필수 | 리다이렉트로 전달받은 1회용 교환 코드. 발급 후 30초 이내 사용해야 한다. |

#### Response Body

```json
{
  "accessToken": "eyJ...",
  "role": "REGULAR"
}
```

| **필드** | **타입** | **설명** |
| --- | --- | --- |
| `accessToken` | String | API 호출에 사용하는 access token. |
| `role` | String | `REGULAR` 또는 `ADMIN`. |

#### 에러

| **상황** | **status** | **code** |
| --- | --- | --- |
| `code`가 없거나 형식이 잘못됨 | 400 | `COMMON_INVALID_REQUEST` |
| `code`가 만료됐거나 이미 사용됨 | 401 | `AUTH_TOKEN_INVALID` |
| 이미 다른 소셜 제공자로 가입된 이메일 | 409 | `MEMBER_DUPLICATE_SOCIAL_ACCOUNT` |
| 정지·탈퇴 등으로 로그인할 수 없는 회원 | 403 | `MEMBER_LOGIN_NOT_ALLOWED` |

### **재발급**

```
POST /api/auth/refresh
```

- `Cookie` 헤더의 refresh token으로 access/refresh token 쌍을 재발급한다. 재발급된 refresh token은 다시 쿠키로 내려간다(속성은 "콜백 및 토큰 교환" 절과 동일).
- 인증이 필요 없다(access token이 이미 만료된 상태에서 호출되므로).

#### Response Body

```json
{
  "accessToken": "eyJ...",
  "role": "REGULAR"
}
```

#### 에러

| **상황** | **status** | **code** |
| --- | --- | --- |
| refresh token 쿠키가 없음 | 401 | `AUTH_TOKEN_MISSING` |
| refresh token이 유효하지 않음(불일치·만료·유예시간 초과) | 401 | `AUTH_TOKEN_INVALID` |

### **로그아웃**

```
POST /api/auth/logout
```

- `Cookie` 헤더의 refresh token을 서버에서 폐기하고, 쿠키를 만료시킨다.
- refresh token 쿠키가 없어도 `204`를 응답한다(멱등 — 이미 로그아웃된 상태를 실패로 보지 않는다).
- 성공 시 `204 No Content`.
- 인증이 필요 없다.

> 위 Auth API는 `feat/WLSH-75-member-social-login-OAtuh2`에서 구현 완료됐다(9/10). 실제 구현 기준으로 최종화한 문서다.

---

# **Member API**

회원 정보 조회·수정·탈퇴를 담당한다. 관련 도메인은 `member`다.

- `/api/members/**`는 `Auth API`에서 발급한 access token으로 인증해야 한다(`/api/auth/**` 제외 공통 규칙).
- 본인 리소스만 조회·수정·탈퇴할 수 있다 — 대상은 URL 경로 변수가 아니라 `/me`로 고정하고, 실제 대상은 access token에서 해석한다.

### **내 정보 조회**

```
GET /api/members/me
```

#### Response Body

```json
{
  "memberId": 1,
  "email": "member@example.com",
  "displayName": "홍길동",
  "status": "ACTIVE",
  "role": "REGULAR"
}
```

### **내 정보 수정**

```
PATCH /api/members/me
```

표시 이름(`displayName`)만 수정한다.

#### Request Body

```json
{
  "displayName": "새이름"
}
```

| **필드** | **타입** | **제약** | **설명** |
| --- | --- | --- | --- |
| `displayName` | String | 필수, 공백 불가 | 표시 이름 |

#### Response Body

내 정보 조회와 동일한 형식.

#### 에러

| **상황** | **status** | **code** |
| --- | --- | --- |
| `displayName`이 없거나 공백 | 400 | `COMMON_INVALID_REQUEST` |

### **회원 탈퇴**

```
DELETE /api/members/me
```

- 로컬 상태를 `WITHDRAWN`으로 변경하고 소프트 삭제한다. Google 쪽 소셜 연동 해제(Unlink)는 호출하지 않는다 — 필요하면 회원이 Google 계정에서 직접 해제해야 한다(`docs/DOMAIN.md` 인증·회원가입 정책).
- 성공 시 `204 No Content`.

### **회원 생성(테스트/관리자용)**

```
POST /api/members
```

실제 가입은 Google 로그인 시 자동으로 이뤄진다 — 이 엔드포인트는 테스트·관리자 용도로만 email/displayName/provider/providerId를 직접 입력받아 회원을 만든다.

#### Request Body

```json
{
  "email": "member@example.com",
  "displayName": "홍길동",
  "provider": "GOOGLE",
  "providerId": "google-sub-1"
}
```

#### Response Body

내 정보 조회와 동일한 형식. 성공 시 `201 Created`와 `Location` 헤더(`/api/members/{memberId}`).

#### 에러

| **상황** | **status** | **code** |
| --- | --- | --- |
| 요청 필드 검증 실패 | 400 | `COMMON_INVALID_REQUEST` |
| 이미 등록된 이메일 | 409 | `MEMBER_DUPLICATE_EMAIL` |

---