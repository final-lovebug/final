**API Convention**

## **목표**

이 문서는 백엔드가 제공하는 HTTP API의 **요청/응답 규격**을 기록한다. 프론트엔드 연동, 신규 입사자 온보딩, API 변경 리뷰의 기준 문서로 사용한다.

API는 `docs/ARCHITECTURE.md`의 레이어 규칙을 따른다. 요청/응답 DTO는 presentation 레이어에서만 정의하고, service는 command/result 모델로 소통한다. 에러 응답 형식은 `docs/EXCEPTION.md`를 따른다. 도메인 모델이 JPA 엔티티를 겸하므로, 엔티티가 응답으로 새어 나가는 것을 레이어 규칙이 막아 주지 않는다. 아래 공통 규칙으로 명시적으로 금지한다.

## **공통 규칙**

- 기본 경로 접두사는 `/api`다.
- 요청/응답 본문은 모두 `application/json`이다.
- 성공 응답의 HTTP 상태 코드는 유스케이스 의미에 맞춘다. 리소스 생성은 `201 Created`, 조회는 `200 OK`를 사용한다.
- 요청 형식 검증 실패는 `400 Bad Request`와 **`COMMON_INVALID_REQUEST`** 코드로 내려간다. 필드별 상세는 `errors` 배열에 담긴다.
- 비즈니스 규칙 위반은 도메인 `ErrorCode`에 정의된 상태 코드와 코드로 내려간다.
- 인증 사용자 식별자(`userId`)는 인증 계층에서 해석해 컨트롤러로 전달한다. 요청 본문에 담지 않는다.
- 도메인 모델을 응답 본문으로 직접 직렬화하지 않는다. presentation은 service가 반환한 result 모델만 응답 DTO로 변환한다.
- service의 result 모델에도 도메인 모델을 담지 않는다. 필요한 값만 옮긴 record로 만든다.
- 요청 본문을 도메인 모델에 바인딩하지 않는다. 요청 DTO → command로만 들어온다.

## **API 버저닝**

경로에 버전 세그먼트를 두지 않는다. 접두사는 `/api`뿐이고 `/api/v1` 같은 형태를 쓰지 않는다. 호환이 깨지는 변경이 필요해지면 그때 방침을 정한다.

## **요청자 식별 — 임시 방식**

인증 계층(`NFR-USR-001`)이 아직 없어 **요청자 회원 식별자를 `memberId` 요청 파라미터로 받는다.** 위 공통 규칙("인증 사용자 식별자는 인증 계층에서 해석해 컨트롤러로 전달한다")에 대한 의도적 예외이며, **인증 도입 시 이 파라미터는 전부 사라진다.** 누구나 남의 `memberId`를 넣어 호출할 수 있으므로 **인증 전까지 운영 배포 대상이 아니다.**

컨트롤러 클래스 주석에 `TODO(NFR-USR-001)`과 이 사실을 적는다.

## **데이터 격리 — 접근 불가 리소스는 404**

**참여자가 아닌 워크스페이스에 속한 리소스는 `403`이 아니라 `404`로 응답한다.** `403`을 주면 그 리소스가 존재한다는 사실이 드러난다(`NFR-WS-001`). 참여자이지만 서열이 모자란 경우에만 `403`이고, 이때는 WARN 감사 로그를 남긴다(`NFR-REV-001`).

워크스페이스 스코프 리소스 전체에 적용된다 — 워크스페이스, 문서, 사전집, 초안, 리뷰 요청.

## **페이징·정렬 규격**

목록 조회는 페이징을 쓴다. 배열을 그대로 반환하지 않는다. (개수가 구조적으로 작은 목록 — 한 리뷰 요청의 리뷰어 목록 등 — 은 예외로 배열을 반환할 수 있다)

**요청 파라미터**

| 파라미터 | 기본값 | 규칙 |
| --- | --- | --- |
| `page` | `0` | **0-base**. 음수는 `400` |
| `size` | `20` | 최대 `100`. 초과·0 이하는 `400` |
| `sort` | 엔드포인트별 지정 | `필드,방향` 형식(예: `occurrenceCount,desc`). **엔드포인트가 허용하는 필드 화이트리스트 밖이면 `400`** — 임의 필드 정렬을 허용하면 인덱스 없는 컬럼으로 전체 스캔이 난다 |

**응답 본문**

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

- 키 이름은 위 5개로 고정한다. Spring Data `Page`의 직렬화 형태(`pageable`, `first`, `last`, `numberOfElements` 등)를 그대로 내보내지 않는다 — 버전에 따라 필드가 바뀌고 클라이언트가 그것에 의존하게 된다.
- **service는 Spring `Page`를 반환하지 않는다.** infra 타입이 상위 레이어로 새는 것이다. `common/service/PageResult`로 옮겨 반환하고, presentation이 `common/presentation/PageResponse`로 변환한다.

## **에러 응답 형식**

모든 에러는 다음 형식을 사용한다. (상세 규칙은 `docs/EXCEPTION.md` 참고)

```json
{
  "code": "COMMON_RESOURCE_NOT_FOUND",
  "message": "요청한 리소스를 찾을 수 없습니다."
}
```

검증 실패는 `errors` 배열이 붙는다. 비어 있으면 직렬화에서 빠진다.

```json
{
  "code": "COMMON_INVALID_REQUEST",
  "message": "요청 값이 올바르지 않습니다.",
  "errors": [{ "field": "title", "message": "must not be blank" }]
}
```

> `NFR-CMN-003`은 `traceId`도 요구한다. `docs/LOG.md`가 MDC와 「에러 응답에 trace id 포함」을 이미 규정하므로, **인증·공통 설정 태스크(`T-INT-3`)에서 `ErrorResponse`에 한 필드로 추가한다.** 그때까지는 위 형식이다.

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
# **Workspace API**

사전집과 문서를 공유하는 협업 단위를 만들고 관리한다. 관련 도메인은 `workspace`다.

## **권한과 응답 원칙**

참여자 권한은 `OWNER > ADMIN > REGULAR` 3단계이고 **Owner와 Admin은 사실상 동급**이다. 권한 검사는 `validateAtLeast(workspaceId, memberId, Permission.ADMIN)` 한 줄로 하고 Owner 전용 분기를 만들지 않는다(워크스페이스 삭제·소유권 이전만 예외).

요청자 식별(`memberId` 임시 파라미터)과 접근 불가 리소스에 `404`를 주는 규칙은 **공통 규칙 절**에 있다. 이 도메인만의 규칙이 아니다.

| **Method** | **Path** | **권한** | **성공** |
| --- | --- | --- | --- |
| POST | `/api/workspaces` | 로그인 | `201` |
| GET | `/api/workspaces` | — | `200` |
| GET | `/api/workspaces/{workspaceId}` | 참여자 | `200` |
| PATCH | `/api/workspaces/{workspaceId}` | ADMIN 이상 | `204` |
| PATCH | `/api/workspaces/{workspaceId}/rule-set` | ADMIN 이상 | `200` |
| DELETE | `/api/workspaces/{workspaceId}` | OWNER | `204` |

## **워크스페이스 생성**

`POST /api/workspaces?memberId={memberId}` → `201 Created`

생성자를 **OWNER 참여자로 자동 등록**한다. 리뷰 규칙(룰셋)은 기본값 `0 / 0`으로 함께 저장된다.

```json
{
  "name": "개발팀"
}
```

| **필드** | **타입** | **제약** | **설명** |
| --- | --- | --- | --- |
| `name` | String | 필수, 1~50자 | 워크스페이스 이름 |

```json
{
  "workspaceId": 1,
  "name": "개발팀",
  "requiredDocumentReviewerCount": 0,
  "requiredDictionaryReviewerCount": 0,
  "myPermission": "OWNER",
  "createdAt": "2026-09-09T10:24:38.123456Z"
}
```

> 생성·목록·상세가 같은 응답 형식을 쓴다. 생성 직후에는 요청자가 OWNER이고 룰셋이 `0 / 0`이다.

## **워크스페이스 룰셋 수정**

`PATCH /api/workspaces/{workspaceId}/rule-set?memberId={memberId}` → `200 OK`

**ADMIN 이상**만 수정할 수 있다. 각 필수 리뷰어 수는 현재 참여자 수 이하만 허용된다.

```json
{
  "requiredDocumentReviewerCount": 2,
  "requiredDictionaryReviewerCount": 1
}
```

응답은 저장된 실제 룰셋을 포함한 `WorkspaceResponse`다.

## **참여 중인 워크스페이스 목록 조회**

`GET /api/workspaces?memberId={memberId}` → `200 OK`

**참여 중인 워크스페이스만** 내려간다. 참여하지 않은 워크스페이스는 목록에 들어오지 않는다.

> 이 엔드포인트는 **페이징 규격의 예외**로 배열을 반환한다. 한 회원이 참여하는 워크스페이스 수가 구조적으로 작기 때문이다.

```json
[
  {
    "workspaceId": 1,
    "name": "개발팀",
    "requiredDocumentReviewerCount": 0,
    "requiredDictionaryReviewerCount": 0,
    "myPermission": "OWNER",
    "createdAt": "2026-09-09T10:24:38.123456Z"
  }
]
```

## **워크스페이스 상세 조회**

`GET /api/workspaces/{workspaceId}?memberId={memberId}` → `200 OK`

```json
{
  "workspaceId": 1,
  "name": "개발팀",
  "requiredDocumentReviewerCount": 0,
  "requiredDictionaryReviewerCount": 0,
  "myPermission": "OWNER",
  "createdAt": "2026-09-09T10:24:38.123456Z"
}
```

| **필드** | **타입** | **설명** |
| --- | --- | --- |
| `requiredDocumentReviewerCount` | Int | 문서 승인에 필요한 최소 리뷰어 수 |
| `requiredDictionaryReviewerCount` | Int | 사전 승인에 필요한 최소 리뷰어 수 |
| `myPermission` | Enum | 요청자의 권한. `OWNER` / `ADMIN` / `REGULAR` |

## **워크스페이스 이름 수정**

`PATCH /api/workspaces/{workspaceId}?memberId={memberId}` → `204 No Content`

**ADMIN 이상**만 수정할 수 있다. 이름만 바꾸므로 응답 본문이 없다.

```json
{
  "name": "플랫폼팀"
}
```

## **워크스페이스 삭제**

`DELETE /api/workspaces/{workspaceId}?memberId={memberId}` → `204 No Content`

**OWNER만** 삭제할 수 있다. **소프트 삭제**이며 이후 모든 조회에서 빠진다. 참여자 행은 함께 지우지 않는다 — 조회가 워크스페이스에서 먼저 막히기 때문이다.

## **에러**

| **상황** | **status** | **code** |
| --- | --- | --- |
| 없거나 삭제된 워크스페이스, **참여자가 아닌 워크스페이스** | 404 | `WORKSPACE_NOT_FOUND` |
| 참여자지만 ADMIN 미만이 이름 수정을 시도 | 403 | `WORKSPACE_ADMIN_REQUIRED` |
| 참여자지만 OWNER가 아닌 사용자가 삭제를 시도 | 403 | `WORKSPACE_OWNER_REQUIRED` |
| 이름이 비었거나 50자 초과(도메인 검증) | 400 | `WORKSPACE_INVALID_NAME` |
| 필수 리뷰어 수가 0 미만(도메인 검증) | 400 | `WORKSPACE_INVALID_REVIEWER_COUNT` |
| 요청 DTO 검증 실패, `memberId` 누락 | 400 | `COMMON_INVALID_REQUEST` |

> `COMMON_INVALID_REQUEST`는 요청 DTO 검증(`@NotBlank`·`@Size`)에서, `WORKSPACE_INVALID_*`는 도메인 모델 검증에서 발생한다. 같은 입력이라도 앞단에서 걸리면 `COMMON_INVALID_REQUEST`가 먼저 내려간다.

---

# **Document API**

워크스페이스에 속한 문서를 만들고 읽고 지운다. 버전 이력과 라벨도 여기서 다룬다. 관련 도메인은 `document`다.

## **요청자 식별 — 임시 방식**

Workspace API와 같다. 인증 계층(`NFR-USR-001`)이 없어 요청자 회원 식별자를 `memberId` 요청 파라미터로 받는다. **인증 전까지 운영 배포 대상이 아니다.**

## **알아 둘 것 셋**

- **본문은 확정 버전에만 있다.** `Document`에는 본문 컬럼이 없다. 상세 응답의 `content`는 `currentVersionNo`가 가리키는 `DocumentVersion`에서 온다.
- **본문이 바뀌는 경로는 셋이고 엔드포인트는 둘이다**(2026-09-10 확정).

| 경로 | 엔드포인트 | `dictionaryVersionNo` | `edited` |
| --- | --- | --- | --- |
| 업로드(v1) | `POST .../documents` | `null` | `false` |
| **직접 편집** | **`PATCH .../documents/{documentId}/content`** | **이전 버전 값 승계** | **`true`** |
| 갱신 → 교정 → 리뷰 → 반영 | `reviewrequest` 도메인의 발행 | **발행 시점 활성 사전집 버전** | `false` |

  **직접 편집은 대조·초안·개정안을 거치지 않고 즉시 새 버전을 발행한다.** `PATCH .../documents/{documentId}`(제목·라벨)와 **경로를 나눈 이유**는 본문 편집이 버전을 만들고 제목·라벨 수정은 만들지 않기 때문이다 — 같은 엔드포인트에 섞으면 요청 하나가 두 의미를 갖는다. `REQ-CHK-007`(편집 저장 시 대조)은 이 결정으로 **폐기**했다.

- **`aligned`(정렬됨)가 `outdated`를 대체한다**(2026-09-10). `최신 확정 버전의 dictionaryVersionNo == 활성 사전집 versionNo` **AND** `edited == false`일 때 참이다.
  - **참이면 용어 추출 대상이고 갱신이 필요 없다.** 두 의미가 같은 조건이라 필드 하나로 합쳤다.
  - **`edited`가 별도 축인 이유** — 편집본이 이전 사전집 버전을 승계하므로 버전 번호만 보면 「통과했다」로 읽힌다. 사람이 손댄 본문은 그 사전집을 통과한 적이 없다. 그래서 응답에 `aligned`·`edited`·`dictionaryVersionNo` 셋을 함께 내려 **정렬되지 않은 이유**를 화면이 설명할 수 있게 한다.
  - 활성 사전집이 없으면 양쪽이 `null`이라 `aligned`가 참이다 — 갱신할 대상이 없고 첫 추출의 대상이 된다.

## **엔드포인트**

경로는 모두 워크스페이스 하위에 중첩된다. `workspaceId`가 URL에 강제되면 데이터 격리(`NFR-WS-001`) 검증이 모든 엔드포인트에서 같은 모양이 된다.

| **Method** | **Path** | **권한** | **성공** |
| --- | --- | --- | --- |
| POST | `/api/workspaces/{workspaceId}/documents` | 참여자 | `201` |
| GET | `/api/workspaces/{workspaceId}/documents` | 참여자 | `200` |
| GET | `/api/workspaces/{workspaceId}/documents/{documentId}` | 참여자 | `200` |
| PATCH | `/api/workspaces/{workspaceId}/documents/{documentId}` | 참여자 | `204` |
| PATCH | `/api/workspaces/{workspaceId}/documents/{documentId}/content` | 참여자 | `200` |
| DELETE | `/api/workspaces/{workspaceId}/documents/{documentId}` | **ADMIN 이상** | `204` |
| GET | `/api/workspaces/{workspaceId}/documents/{documentId}/versions` | 참여자 | `200` |
| GET | `/api/workspaces/{workspaceId}/documents/{documentId}/versions/{versionNo}` | 참여자 | `200` |
| GET | `/api/workspaces/{workspaceId}/labels` | 참여자 | `200` |

## **문서 생성**

`POST /api/workspaces/{workspaceId}/documents?memberId={memberId}` → `201 Created`

`Document`와 `DocumentVersion` v1을 한 트랜잭션에서 만든다. **파일 업로드가 아니라 JSON 본문 작성**이다(`REQ-DOC-001`. multipart 업로드는 후속).

```json
{
  "title": "결제 도메인 설계",
  "content": "회원은 결제할 수 있다.",
  "labels": ["설계", "결제"]
}
```

| **필드** | **타입** | **제약** | **설명** |
| --- | --- | --- | --- |
| `title` | String | 필수, 1~200자 | 워크스페이스 안에서 중복을 허용한다 |
| `content` | String | 필수, 1~10,000자 | v1 버전의 본문이 된다 |
| `labels` | String[] | 선택, 최대 5개, 각 1~20자 | **없는 이름은 라벨이 새로 만들어진다** |

```json
{
  "documentId": 1,
  "workspaceId": 1,
  "title": "결제 도메인 설계",
  "content": "회원은 결제할 수 있다.",
  "currentVersionNo": 1,
  "aligned": true,
  "edited": false,
  "dictionaryVersionNo": null,
  "labels": ["결제", "설계"],
  "uploaderId": 7,
  "createdAt": "2026-09-10T10:24:38.123456Z",
  "updatedAt": "2026-09-10T10:24:38.123456Z"
}
```

| **필드** | **타입** | **설명** |
| --- | --- | --- |
| `content` | String | **최신 확정 버전의 본문** |
| `aligned` | Boolean | 사전집 기준에 맞춰져 있고 그 뒤로 사람이 손대지 않았는지. **참이면 용어 추출 대상이고 갱신이 필요 없다.** 판정 기준은 아래 |
| `edited` | Boolean | 최신 확정 버전이 **직접 편집본**인지. `aligned`가 거짓인 이유를 구분하게 해 준다 |
| `dictionaryVersionNo` | Int | 최신 확정 버전이 통과한 사전집 버전. 업로드본은 `null`, 편집본은 이전 값 승계 |
| `uploaderId` | Long | 문서를 올린 회원 |

> **`aligned` 판정** — `최신 확정 버전의 dictionaryVersionNo == 활성 사전집 versionNo` **AND** `edited == false`. 활성 사전집이 없으면 양쪽이 `null`이라 `true`다. 기준이 「마지막으로 대조한 시점」이 아니라 「마지막으로 반영된 버전」이므로, **대조만 하고 교정을 끝내지 않은 문서는 `false`다.** 직접 편집본도 `false`다 — 사람이 손댄 본문은 그 사전집을 통과한 적이 없다.

## **문서 목록 조회**

`GET /api/workspaces/{workspaceId}/documents?memberId={memberId}&label={name}&page=0&size=20&sort=createdAt,desc` → `200 OK`

`label`은 선택이다. 주면 그 라벨이 붙은 문서만 내려간다. 페이징·정렬은 **공통 규칙 절**을 따르고, `sort` 화이트리스트는 `createdAt`(기본, 내림차순)·`title`이다.

**항목에 본문(`content`)을 담지 않는다.** 10,000자 × N을 목록에 실을 이유가 없다. 아래 응답의 `content` 키는 페이징 규격의 항목 배열이며 문서 본문과 무관하다.

```json
{
  "content": [
    {
      "documentId": 1,
      "title": "결제 도메인 설계",
      "currentVersionNo": 1,
      "aligned": true,
      "edited": false,
      "dictionaryVersionNo": null,
      "labels": ["결제", "설계"],
      "uploaderId": 7,
      "createdAt": "2026-09-10T10:24:38.123456Z",
      "updatedAt": "2026-09-10T10:24:38.123456Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

## **문서 상세 조회**

`GET /api/workspaces/{workspaceId}/documents/{documentId}?memberId={memberId}` → `200 OK`

생성 응답과 같은 형식이다. `content`는 최신 확정 버전에서 온다.

## **문서 수정**

`PATCH /api/workspaces/{workspaceId}/documents/{documentId}?memberId={memberId}` → `204 No Content`

**제목과 라벨만 바꾼다.** 본문은 이 경로로 바뀌지 않는다.

```json
{
  "title": "정산 도메인 설계",
  "labels": ["정산"]
}
```

> **`labels`는 통째로 교체된다.** 빈 배열이나 생략은 「라벨을 모두 뗀다」는 뜻이다. 부분 추가·삭제 엔드포인트를 따로 두지 않는 이유는 문서당 5개뿐이라 전체 교체가 더 단순하기 때문이다. **문서에서 뗀 라벨도 워크스페이스에는 남는다.**

## **본문 편집**

`PATCH /api/workspaces/{workspaceId}/documents/{documentId}/content?memberId={memberId}` → `200 OK`

**대조·초안·개정안을 거치지 않고 즉시 새 버전을 발행한다**(2026-09-10 확정). 참여자면 누구나 할 수 있다 — `DOMAIN.md`가 「Regular는 문서 작성·교정·리뷰까지 가능」으로 두었고 편집은 작성에 해당한다.

```json
{
  "content": "회원은 결제 수단을 선택해 결제할 수 있다."
}
```

| **필드** | **타입** | **제약** | **설명** |
| --- | --- | --- | --- |
| `content` | String | 필수, 1~10,000자 | 새 버전의 본문이 된다 |

응답은 문서 상세와 같은 형식이며 **`currentVersionNo`가 오르고 `edited`가 `true`, `aligned`가 `false`**다.

- `dictionaryVersionNo`는 **이전 버전 값을 승계**한다. 편집은 대조를 거치지 않아 어떤 사전집도 통과하지 않았지만, 그 사실은 `edited`가 나타낸다.
- **응답이 `204`가 아닌 이유** — 새 버전 번호와 갱신된 `aligned`를 클라이언트가 알아야 한다. 제목·라벨 수정(`204`)과 차이가 여기서 드러난다.
- **진행 중인 초안이 걸린 문서는 편집할 수 없다**(`409`). 두 경우다 — 그 문서에 진행 중인 문서 초안이 있거나(초안의 `baseVersionNo`가 낡고 발행 시 편집 내용을 덮어쓴다), 그 문서가 진행 중인 사전 초안의 원천 문서다(초안이 이미 없는 본문을 근거로 삼는다).

## **문서 삭제**

`DELETE /api/workspaces/{workspaceId}/documents/{documentId}?memberId={memberId}` → `204 No Content`

**ADMIN 이상만** 삭제할 수 있다. **소프트 삭제**이며 이후 모든 조회에서 빠진다. 확정된 버전 행과 라벨 연결 행은 함께 지우지 않는다 — 조회가 문서에서 먼저 막히기 때문이다.

## **버전 이력 조회**

`GET /api/workspaces/{workspaceId}/documents/{documentId}/versions?memberId={memberId}&page=0&size=20` → `200 OK`

`versionNo` 내림차순 고정. **본문을 담지 않는다.**

```json
{
  "content": [
    { "versionNo": 2, "publishedAt": "2026-09-11T09:00:00.000000Z", "dictionaryVersionNo": null, "edited": true, "publishedBy": 7 },
    { "versionNo": 1, "publishedAt": "2026-09-10T10:24:38.123456Z", "dictionaryVersionNo": null, "edited": false, "publishedBy": 7 }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

`edited`로 **그 버전이 어떻게 만들어졌는지**가 드러난다 — `true`는 사람이 직접 고친 것, `false`는 업로드본이거나 교정 반영본이다.

> 반영(Revise) 경로는 `reviewrequest` 도메인이 붙어야 생긴다. **그때까지 v2 이상은 직접 편집으로만 쌓인다.**

## **특정 버전 조회**

`GET /api/workspaces/{workspaceId}/documents/{documentId}/versions/{versionNo}?memberId={memberId}` → `200 OK`

위 형식에 `body`가 붙는다.

```json
{
  "versionNo": 1,
  "body": "회원은 결제할 수 있다.",
  "publishedAt": "2026-09-10T10:24:38.123456Z",
  "dictionaryVersionNo": null,
  "edited": false,
  "publishedBy": 7
}
```

## **워크스페이스 라벨 목록 조회**

`GET /api/workspaces/{workspaceId}/labels?memberId={memberId}` → `200 OK`

이름 오름차순. 목록 필터 UI를 채우기 위한 것이다. **라벨 생성·수정·삭제 엔드포인트는 없다** — 라벨은 문서에 붙일 때 없으면 만들어진다.

```json
[
  { "labelId": 1, "name": "결제" },
  { "labelId": 2, "name": "설계" }
]
```

## **에러**

| **상황** | **status** | **code** |
| --- | --- | --- |
| 없거나 삭제된 워크스페이스, **참여자가 아닌 워크스페이스** | 404 | `WORKSPACE_NOT_FOUND` |
| 없거나 삭제된 문서, **다른 워크스페이스의 문서 식별자** | 404 | `DOCUMENT_NOT_FOUND` |
| 없는 버전 번호 | 404 | `DOCUMENT_VERSION_NOT_FOUND` |
| 참여자지만 ADMIN 미만이 삭제를 시도 | 403 | `WORKSPACE_ADMIN_REQUIRED` |
| 제목이 비었거나 200자 초과(도메인 검증) | 400 | `DOCUMENT_INVALID_TITLE` |
| 본문이 비었거나 10,000자 초과(도메인 검증) | 400 | `DOCUMENT_INVALID_CONTENT` |
| 라벨이 6개 이상 | 400 | `DOCUMENT_LABEL_LIMIT_EXCEEDED` |
| 라벨 이름이 비었거나 20자 초과 | 400 | `LABEL_INVALID_NAME` |
| **진행 중인 문서 초안이 있어 본문을 편집할 수 없음** | 409 | `DOCUMENT_DRAFT_IN_PROGRESS` |
| **진행 중인 사전 초안의 원천 문서라 본문을 편집할 수 없음** | 409 | `DOCUMENT_SOURCE_OF_DICTIONARY_DRAFT` |
| 요청 DTO 검증 실패, `memberId` 누락 | 400 | `COMMON_INVALID_REQUEST` |

> 권한 부족에 document 전용 코드를 두지 않고 `WORKSPACE_ADMIN_REQUIRED`를 그대로 쓴다. 검증 주체가 `WorkspaceAccessValidator`이므로 같은 뜻의 코드를 도메인마다 늘리지 않는다.

---

# **Dictionary API**

워크스페이스에서 합의된 표준 용어의 집합을 버전 단위로 조회하고, 리뷰 승인이 위임한 발행을 수행한다. 관련 도메인은 `dictionary`다.

## **요청자 식별 — 임시 방식**

Workspace API와 같다. 인증 계층(`NFR-USR-001`)이 없어 요청자 회원 식별자를 `memberId` 요청 파라미터로 받는다. **인증 전까지 운영 배포 대상이 아니다.**

## **알아 둘 것 셋**

- **사전집 행 하나가 확정된 버전 하나다.** 워크스페이스에 행이 쌓이고 **활성중인 행 하나**가 가장 최근 확정본이자 대조·추출의 기준이다. 나머지는 보관 버전이며 내용이 바뀌지 않는다.
- **경로가 단수 `/dictionary`인 이유** — 활성 1개 + 보관 N개로 존재하고 「이름」을 두지 않으므로 리소스 컬렉션이 아니라 워크스페이스의 단일 속성처럼 읽힌다. 버전은 그 하위에 둔다.
- **워크스페이스에 사전집이 없는 기간은 정상이다.** 첫 발행 전까지가 그 상태이며 오류 상황이 아니다. 사전집은 사람이 빈 껍데기를 만드는 것이 아니라 **문서에서 용어를 추출해 리뷰를 통과시킨 결과로 태어난다**(`DOMAIN.md` «사전집 생성 주기»).

## **엔드포인트**

| **Method** | **Path** | **권한** | **성공** | **태그** |
| --- | --- | --- | --- | --- |
| POST | `/api/workspaces/{workspaceId}/dictionary/versions` | **ADMIN 이상** | `201` | **INTERNALIZE** |
| GET | `/api/workspaces/{workspaceId}/dictionary` | 참여자 | `200` | KEEP |
| GET | `/api/workspaces/{workspaceId}/dictionary/versions` | 참여자 | `200` | KEEP |
| GET | `/api/workspaces/{workspaceId}/dictionary/versions/{versionNo}` | 참여자 | `200` | KEEP |

### **임시 API 태그**

| 태그 | 뜻 |
| --- | --- |
| KEEP | 리뷰 도메인이 붙어도 그대로 남는다 |
| **INTERNALIZE** | **HTTP 노출을 걷어내고 서비스 메서드만 남긴다.** 리뷰 승인이 발행 위임 포트로 호출하는 진입점이 되고 엔드포인트는 사라진다 |

**`POST /versions`가 INTERNALIZE다.** `DOMAIN.md` «사전집»의 「새 버전은 리뷰 승인(Revise)의 반영으로만 생긴다」와 `NFR-UPD-001`(Human-in-the-Loop)에 어긋나므로 최종 형태가 아니다.

**지금 유지하는 이유** — 이것이 사전집을 만드는 유일한 경로여서, 잠그면 문서의 `aligned` 판정과 활성 버전 조회를 검증할 수 없다. **리뷰 도메인 완성 시 제거 — `DIC-7`**.

## **사전집 버전 반영**

`POST /api/workspaces/{workspaceId}/dictionary/versions?memberId={memberId}` → `201 Created`

**ADMIN 이상**만 할 수 있다. 용어 목록 전체를 실어 새 버전을 만든다.

```json
{
  "terms": [
    { "preferredForm": "회원", "englishName": "Member", "definition": "서비스에 가입해 인증받는 주체" },
    { "preferredForm": "사전집", "englishName": "Dictionary", "definition": "워크스페이스에서 합의된 표준 용어의 집합" }
  ]
}
```

| **필드** | **타입** | **제약** | **설명** |
| --- | --- | --- | --- |
| `terms` | Object[] | 필수, 1개 이상 | **넘어온 목록이 그 버전의 전체 내용이 된다** |
| `terms[].preferredForm` | String | 필수, 1~100자 | 표준어. **사전집 안에서 유일** |
| `terms[].englishName` | String | 선택, 100자 이하 | 코드·DB 네이밍 기준. 빈 문자열은 `null`로 모은다 |
| `terms[].definition` | String | 필수 | 대조 시 LLM의 판단 근거 |

> **이전 버전에서 복사하지 않는다.** 넘어온 목록만 그 버전의 내용이 되므로, 기존 용어를 유지하려면 **호출자가 함께 실어야** 한다. 사전 초안이 「이전 사전집 + 추출 용어」의 통합 결과인 것이 이 구조와 맞물린다(2026-09-10 확정).
>
> **표준어 유일성은 이중으로 막는다.** 요청 안의 중복은 저장 전에 `409 TERM_DUPLICATE_PREFERRED_FORM`으로 걸리고(`NFR-DIC-002`), DB 유니크가 최후 방어선이다. DB 제약에 닿으면 사용자에게 줄 메시지를 만들 수 없으므로 앞단에서 잡는다.

```json
{
  "versionNo": 2,
  "status": "ACTIVE",
  "publishedAt": "2026-09-10T11:00:00.000000Z",
  "publishedBy": 7,
  "terms": {
    "content": [
      { "termId": 11, "preferredForm": "사전집", "englishName": "Dictionary", "definition": "워크스페이스에서 합의된 표준 용어의 집합" },
      { "termId": 10, "preferredForm": "회원", "englishName": "Member", "definition": "서비스에 가입해 인증받는 주체" }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1
  }
}
```

용어는 `preferredForm` 오름차순이다. **이전 활성 사전집은 같은 트랜잭션에서 보관 버전으로 내려간다** — 활성 사전집은 워크스페이스당 정확히 1개이고 DB 유니크가 그것을 보장한다.

## **활성 사전집 조회**

`GET /api/workspaces/{workspaceId}/dictionary?memberId={memberId}&page=0&size=20&sort=preferredForm,asc&keyword=회원` → `200 OK`

현재 확정본을 읽는다. **문서 대조와 용어 추출이 기준으로 삼는 사전집**이다.

| **파라미터** | **기본값** | **설명** |
| --- | --- | --- |
| `page`·`size` | `0` / `20` | 공통 규칙 절을 따른다 |
| `sort` | `preferredForm,asc` | 화이트리스트는 `preferredForm`·`createdAt`. 밖이면 `400` |
| `keyword` | — | `preferredForm`·`englishName` 접두 검색(`REQ-DIC-002`) |

응답은 반영과 같은 형식이다. **사전집 메타(버전·상태·확정일시)는 페이징 밖에 있고 `terms`만 페이징된다** — 용어가 수백 건까지 늘 수 있다.

## **버전 이력 조회**

`GET /api/workspaces/{workspaceId}/dictionary/versions?memberId={memberId}&page=0&size=20` → `200 OK`

`versionNo` 내림차순 고정. **용어 목록을 담지 않고 개수만 담는다.**

```json
{
  "content": [
    { "versionNo": 2, "status": "ACTIVE", "publishedAt": "2026-09-10T11:00:00.000000Z", "publishedBy": 7, "termCount": 2 },
    { "versionNo": 1, "status": "ARCHIVED", "publishedAt": "2026-09-09T10:00:00.000000Z", "publishedBy": 7, "termCount": 1 }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

**사전집을 만든 적 없는 워크스페이스는 빈 페이지다** — 조회의 빈 결과는 실패가 아니다.

## **특정 버전 조회**

`GET /api/workspaces/{workspaceId}/dictionary/versions/{versionNo}?memberId={memberId}&page=0&size=20` → `200 OK`

활성 사전집 조회와 같은 형식이며 보관 버전도 읽을 수 있다. **버전마다 용어가 복제되므로 그 버전에 매달린 용어가 곧 그 시점의 스냅샷이다.**

## **에러**

| **상황** | **status** | **code** |
| --- | --- | --- |
| 없거나 삭제된 워크스페이스, **참여자가 아닌 워크스페이스** | 404 | `WORKSPACE_NOT_FOUND` |
| 워크스페이스에 사전집이 없음, 없는 버전 번호 | 404 | `DICTIONARY_NOT_FOUND` |
| 참여자지만 ADMIN 미만이 반영을 시도 | 403 | `WORKSPACE_ADMIN_REQUIRED` |
| `terms`가 비어 있음 | 400 | `DICTIONARY_EMPTY_TERMS` |
| 버전 정보가 올바르지 않음(도메인 검증) | 400 | `DICTIONARY_INVALID_VERSION` |
| 표준어가 비었거나 100자 초과 | 400 | `TERM_INVALID_PREFERRED_FORM` |
| 영문명이 100자 초과 | 400 | `TERM_INVALID_ENGLISH_NAME` |
| 정의가 비어 있음 | 400 | `TERM_INVALID_DEFINITION` |
| **한 요청 안에 같은 표준어가 두 번** | 409 | `TERM_DUPLICATE_PREFERRED_FORM` |
| `sort`가 화이트리스트 밖 | 400 | `COMMON_INVALID_REQUEST` |
| 요청 DTO 검증 실패, `memberId` 누락 | 400 | `COMMON_INVALID_REQUEST` |

> `DICTIONARY_NOT_FOUND`가 「아직 없음」과 「없는 버전」을 겸하는 것은 의도다. 비참여자에게는 그보다 앞서 `WORKSPACE_NOT_FOUND`가 나가므로 사전집의 존재 여부가 드러나지 않는다.
>
> 권한 부족에 dictionary 전용 코드를 두지 않고 `WORKSPACE_ADMIN_REQUIRED`를 그대로 쓴다. 검증 주체가 `WorkspaceAccessValidator`이므로 같은 뜻의 코드를 도메인마다 늘리지 않는다.

---

# **ReviewRequest API**

초안에서 만든 개정안의 검토 흐름을 관리한다. Phase 1에서는 리뷰 요청 한 건의 생성·조회·수정·취소를 제공한다. 관련 도메인은 `reviewrequest`다.

## **요청자 식별 — 임시 방식**

Workspace API와 같다. 인증 계층(`NFR-USR-001`)이 없어 요청자 회원 식별자를 `memberId` 요청 파라미터로 받는다. **인증 전까지 운영 배포 대상이 아니다.**

## **알아 둘 것 둘**

- 리뷰 요청은 `DOCUMENT` 또는 `DICTIONARY` 유형을 갖고 `PENDING_REVIEW` 상태로 시작한다. 리뷰어 지정과 개정안 등록은 후속 Phase에서 제공한다.
- `POST /api/review-requests`는 초안 흐름이 완성되기 전까지만 쓰는 **INTERNALIZE** 엔드포인트다. 최종 흐름에서는 초안의 리뷰 요청 API가 요청과 개정안을 한 트랜잭션에서 함께 만든다.

## **엔드포인트**

| **Method** | **Path** | **권한** | **성공** | **태그** |
| --- | --- | --- | --- | --- |
| POST | `/api/review-requests` | 참여자 | `201` | **INTERNALIZE** |
| GET | `/api/review-requests/{reviewRequestId}` | 참여자 | `200` | KEEP |
| PATCH | `/api/review-requests/{reviewRequestId}` | 참여자 | `200` | KEEP |
| POST | `/api/review-requests/{reviewRequestId}/cancellation` | 요청자 | `200` | KEEP |

## **리뷰 요청 생성**

`POST /api/review-requests?memberId={memberId}` → `201 Created`

```json
{
  "workspaceId": 10,
  "type": "DOCUMENT",
  "title": "결제 문서 리뷰",
  "description": "결제 문서의 개정안을 검토합니다."
}
```

`type`은 `DOCUMENT` 또는 `DICTIONARY`다. 제목은 필수이고 255자 이하다. 설명은 생략할 수 있다.

```json
{
  "reviewRequestId": 100,
  "workspaceId": 10,
  "type": "DOCUMENT",
  "title": "결제 문서 리뷰",
  "description": "결제 문서의 개정안을 검토합니다.",
  "requesterId": 7,
  "status": "PENDING_REVIEW",
  "approvedAt": null,
  "revisedAt": null,
  "createdAt": "2026-09-11T10:00:00.000000Z",
  "updatedAt": "2026-09-11T10:00:00.000000Z"
}
```

## **리뷰 요청 상세 조회**

`GET /api/review-requests/{reviewRequestId}?memberId={memberId}` → `200 OK`

응답 형식은 생성 응답과 같다. 요청이 속한 워크스페이스의 참여자만 조회할 수 있다.

## **리뷰 요청 수정**

`PATCH /api/review-requests/{reviewRequestId}?memberId={memberId}` → `200 OK`

```json
{
  "title": "정산 문서 리뷰",
  "description": "정산 문서의 개정안을 검토합니다."
}
```

`title`과 `description`은 각각 생략할 수 있고, 전달한 필드만 바뀐다. 응답 형식은 생성 응답과 같다.

## **리뷰 요청 취소**

`POST /api/review-requests/{reviewRequestId}/cancellation?memberId={memberId}` → `200 OK`

요청자만 취소할 수 있다. 반영 완료 또는 이미 취소된 요청은 다시 취소할 수 없다. 응답의 `status`는 `CANCELED`다.

## **에러**

| **상황** | **status** | **code** |
| --- | --- | --- |
| 없거나 삭제된 워크스페이스, **참여자가 아닌 워크스페이스** | 404 | `WORKSPACE_NOT_FOUND` |
| 없거나 삭제된 리뷰 요청 | 404 | `REVIEW_REQUEST_NOT_FOUND` |
| 제목이 비어 있음 | 400 | `REVIEW_REQUEST_TITLE_REQUIRED` |
| 요청 유형이 올바르지 않음 | 400 | `REVIEW_REQUEST_INVALID_TYPE` |
| 요청자가 아닌 참여자가 취소를 시도 | 403 | `REVIEW_REQUEST_NOT_REQUESTER` |
| 현재 상태에서 취소할 수 없음 | 409 | `REVIEW_REQUEST_INVALID_STATUS_TRANSITION` |
| 요청 DTO 검증 실패, `memberId` 누락 | 400 | `COMMON_INVALID_REQUEST` |

---
