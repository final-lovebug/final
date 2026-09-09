**API Convention**

## **목표**

이 문서는 백엔드가 제공하는 HTTP API의 **요청/응답 규격**을 기록한다. 프론트엔드 연동, 신규 입사자 온보딩, API 변경 리뷰의 기준 문서로 사용한다.

API는 `docs/ARCHITECTURE.md`의 레이어 규칙을 따른다. 요청/응답 DTO는 presentation 레이어에서만 정의하고, service는 command/result 모델로 소통한다. 에러 응답 형식은 `docs/EXCEPTION.md`를 따른다. 도메인 모델이 JPA 엔티티를 겸하므로, 엔티티가 응답으로 새어 나가는 것을 레이어 규칙이 막아 주지 않는다. 아래 공통 규칙으로 명시적으로 금지한다.

## **공통 규칙**

- 기본 경로 접두사는 `/api`다.
- 요청/응답 본문은 모두 `application/json`이다.
- 성공 응답의 HTTP 상태 코드는 유스케이스 의미에 맞춘다. 리소스 생성은 `201 Created`, 조회는 `200 OK`를 사용한다.
- 요청 형식 검증 실패는 `400 Bad Request`와 `INVALID_INPUT` 코드로 내려간다.
- 비즈니스 규칙 위반은 도메인 `ErrorCode`에 정의된 상태 코드와 코드로 내려간다.
- 인증 사용자 식별자(`userId`)는 인증 계층에서 해석해 컨트롤러로 전달한다. 요청 본문에 담지 않는다.
- 도메인 모델을 응답 본문으로 직접 직렬화하지 않는다. presentation은 service가 반환한 result 모델만 응답 DTO로 변환한다.
- service의 result 모델에도 도메인 모델을 담지 않는다. 필요한 값만 옮긴 record로 만든다.
- 요청 본문을 도메인 모델에 바인딩하지 않는다. 요청 DTO → command로만 들어온다.

## **에러 응답 형식**

모든 에러는 다음 형식을 사용한다. (상세 규칙은 `docs/EXCEPTION.md` 참고)

```json
{
  "code": "SOLVED_SESSION_BROKEN_CHAIN",
  "message": "꼬리질문 연결이 올바르지 않습니다."
}
```

---

# **Auth API**

회원가입·로그인과 토큰 수명 관리를 담당한다. 관련 도메인은 `auth`다.

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

> refresh token을 해싱하는 과정(회원가입·로그인·재발급·로그아웃 모두 해당)에서 서버 환경이 해시 알고리즘(SHA-256)을 지원하지 않으면 `500 AUTH_TOKEN_HASH_FAILED`를 반환한다. 정상 배포 환경에서는 발생하지 않는 방어적 오류다.

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

토큰에는 사용자 권한(`role`)이 함께 담긴다. `USER`(일반 사용자) 또는 `ADMIN`(관리자)이며, 로그인 응답으로도 내려준다.

- **관리자 전용 API는 `/api/admin/**` 경로를 쓴다.** 인증을 통과했더라도 `role`이 `ADMIN`이 아니면 `403 Forbidden` `AUTH_FORBIDDEN`을 반환한다.
- **권한 승격은 API로 제공하지 않는다.** 관리자 지정은 운영 DB에서 직접 수행한다. → `docs/DOMAIN.md` 권한 정책
- 재발급 시 권한은 토큰이 아니라 저장된 값을 다시 읽어 담는다. 따라서 권한 변경은 늦어도 access token 수명(30분) 안에 반영된다.

---

## **회원가입**

계정을 생성한다. 토큰은 발급하지 않으므로 가입 후 로그인을 별도로 호출해야 한다. 직무(`position`)는 현재 `BACKEND`로 고정된다.

### **Endpoint**

```
POST /api/auth/signup
```

- 성공 시 `201 Created`와 생성된 사용자 ID를 반환한다.
- 인증이 필요 없다.

### **Request Body**

```json
{
  "email": "member@example.com",
  "password": "password123",
  "nickname": "테스터"
}
```

| **필드** | **타입** | **제약** | **설명** |
| --- | --- | --- | --- |
| `email` | String | 필수, 이메일 형식 | 로그인 ID로 사용한다. |
| `password` | String | 필수, 8~12자 | 저장 시 해싱된다. |
| `nickname` | String | 필수, 4~8자 | 중복될 수 없다. |

### **Response Body**

```json
{
  "userId": 1
}
```

| **필드** | **타입** | **설명** |
| --- | --- | --- |
| `userId` | Long | 생성된 사용자 ID. |

### **에러**

| **상황** | **status** | **code** |
| --- | --- | --- |
| 형식 검증 실패 | 400 | `INVALID_INPUT` |
| 이미 사용 중인 이메일 | 409 | `USER_DUPLICATE_EMAIL` |
| 구글 계정으로 가입된 이메일 | 409 | `USER_DUPLICATE_EMAIL_SOCIAL` |
| 이미 사용 중인 닉네임 | 409 | `USER_DUPLICATE_NICKNAME` |
| 닉네임 길이 규칙 위반 | 400 | `USER_INVALID_NICKNAME` |
| 이메일 형식 규칙 위반 | 400 | `USER_INVALID_EMAIL` |

> `INVALID_INPUT`은 요청 DTO 검증(`@Email`·`@Size`)에서, `USER_INVALID_*`는 도메인 모델 검증에서 발생한다. 같은 입력이라도 앞단에서 걸리면 `INVALID_INPUT`이 먼저 내려간다.

---
# **Workspace API**

사전집과 문서를 공유하는 협업 단위를 만들고 관리한다. 관련 도메인은 `workspace`다.

## **요청자 식별 — 임시 방식**

인증 계층(`NFR-USR-001`)이 아직 없어 **요청자 회원 식별자를 `memberId` 요청 파라미터로 받는다.** 공통 규칙("인증 사용자 식별자는 인증 계층에서 해석해 컨트롤러로 전달한다")에 대한 의도적 예외이며, **인증 도입 시 이 파라미터는 전부 사라진다.** 누구나 남의 `memberId`를 넣어 호출할 수 있으므로 **인증 전까지 운영 배포 대상이 아니다.**

## **권한과 응답 원칙**

참여자 권한은 `OWNER > ADMIN > MEMBER` 3단계다.

- **참여자가 아닌 워크스페이스는 `403`이 아니라 `404`로 응답한다.** `403`을 주면 그 워크스페이스가 존재한다는 사실이 드러난다(`NFR-WS-001` 데이터 격리).
- 참여자이지만 서열이 모자라면 `403`이다.

| **Method** | **Path** | **권한** | **성공** |
| --- | --- | --- | --- |
| POST | `/api/workspaces` | 로그인 | `201` |
| GET | `/api/workspaces` | — | `200` |
| GET | `/api/workspaces/{workspaceId}` | 참여자 | `200` |
| PATCH | `/api/workspaces/{workspaceId}` | ADMIN 이상 | `204` |
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
  "workspaceId": 1
}
```

## **참여 중인 워크스페이스 목록 조회**

`GET /api/workspaces?memberId={memberId}` → `200 OK`

**참여 중인 워크스페이스만** 내려간다. 참여하지 않은 워크스페이스는 목록에 들어오지 않는다.

```json
[
  {
    "workspaceId": 1,
    "name": "개발팀",
    "myPermission": "OWNER"
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
| `myPermission` | Enum | 요청자의 권한. `OWNER` / `ADMIN` / `MEMBER` |

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
