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

Google 인증이 끝나면 서버가 프론트엔드로 리다이렉트하면서 1회용 교환 코드(`code`)를 쿼리 파라미터로 전달한다. 프론트는 이 코드로 토큰을 교환한다.

```
POST /api/auth/oauth/google/exchange
```

- 성공 시 `200 OK`. Access token은 응답 본문으로, refresh token은 `Set-Cookie`(HttpOnly, Secure, SameSite)로 내려간다.
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
| `code`가 없거나 형식이 잘못됨 | 400 | `INVALID_INPUT` |
| `code`가 만료됐거나 이미 사용됨 | 401 | `AUTH_TOKEN_INVALID` |
| 이미 다른 소셜 제공자로 가입된 이메일 | 409 | `MEMBER_DUPLICATE_SOCIAL_ACCOUNT` |
| 정지·탈퇴 등으로 로그인할 수 없는 회원 | 403 | `MEMBER_LOGIN_NOT_ALLOWED` |

### **재발급**

```
POST /api/auth/refresh
```

- `Cookie` 헤더의 refresh token으로 access/refresh token 쌍을 재발급한다. 재발급된 refresh token은 다시 쿠키로 내려간다.
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
- 성공 시 `204 No Content`.
- 인증이 필요 없다.

> 위 엔드포인트 경로·요청/응답 필드는 설계 초안이며, 실제 구현 후 최종 확정한다(계획 9장 11단계).

---
# **Workspace API**

사전집과 문서를 공유하는 협업 단위를 만들고 관리한다. 관련 도메인은 `workspace`다.

## **요청자 식별 — 임시 방식**

인증 계층(`NFR-USR-001`)이 아직 없어 **요청자 회원 식별자를 `memberId` 요청 파라미터로 받는다.** 공통 규칙("인증 사용자 식별자는 인증 계층에서 해석해 컨트롤러로 전달한다")에 대한 의도적 예외이며, **인증 도입 시 이 파라미터는 전부 사라진다.** 누구나 남의 `memberId`를 넣어 호출할 수 있으므로 **인증 전까지 운영 배포 대상이 아니다.**

## **권한과 응답 원칙**

참여자 권한은 `OWNER > ADMIN > REGULAR` 3단계다.

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
  "workspaceId": 1,
  "name": "개발팀",
  "requiredDocumentReviewerCount": 0,
  "requiredDictionaryReviewerCount": 0,
  "myPermission": "OWNER",
  "createdAt": "2026-09-09T10:24:38.123456Z"
}
```

> 생성·목록·상세가 같은 응답 형식을 쓴다. 생성 직후에는 요청자가 OWNER이고 룰셋이 `0 / 0`이다.

## **참여 중인 워크스페이스 목록 조회**

`GET /api/workspaces?memberId={memberId}` → `200 OK`

**참여 중인 워크스페이스만** 내려간다. 참여하지 않은 워크스페이스는 목록에 들어오지 않는다.

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
- **본문을 바꾸는 엔드포인트가 없다.** 본문이 바뀌는 경로는 대조 → 초안 → 리뷰 → 반영뿐이고, 업로드(v1)만 예외다. 문서 편집도 그 경로를 타므로 `draftdocument` 도메인이 `POST /documents/{documentId}/drafts`로 연다(`REQ-CHK-007`). 아래 `PATCH`는 **제목·라벨 전용**이다.
- **`outdated`는 사전집 도메인이 붙기 전까지 항상 `false`다.** 판정 규칙은 구현돼 있으나 활성 사전집 버전을 읽을 곳이 아직 없다. 사전집이 없으면 갱신할 대상이 없으므로 `false`가 맞는 값이다.

## **엔드포인트**

경로는 모두 워크스페이스 하위에 중첩된다. `workspaceId`가 URL에 강제되면 데이터 격리(`NFR-WS-001`) 검증이 모든 엔드포인트에서 같은 모양이 된다.

| **Method** | **Path** | **권한** | **성공** |
| --- | --- | --- | --- |
| POST | `/api/workspaces/{workspaceId}/documents` | 참여자 | `201` |
| GET | `/api/workspaces/{workspaceId}/documents` | 참여자 | `200` |
| GET | `/api/workspaces/{workspaceId}/documents/{documentId}` | 참여자 | `200` |
| PATCH | `/api/workspaces/{workspaceId}/documents/{documentId}` | 참여자 | `204` |
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
  "outdated": false,
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
| `outdated` | Boolean | 최신 사전집에 맞춰지지 않았는지. 판정 기준은 아래 |
| `dictionaryVersionNo` | Int | 최신 확정 버전이 통과한 사전집 버전. 업로드본은 `null` |
| `uploaderId` | Long | 문서를 올린 회원 |

> **`outdated` 판정** — 활성 사전집이 없으면 `false`, 최신 확정 버전의 `dictionaryVersionNo`가 `null`이거나 활성 사전집 버전과 다르면 `true`. 기준이 「마지막으로 대조한 시점」이 아니라 「마지막으로 반영된 버전」이므로, **대조만 하고 교정을 끝내지 않은 문서는 여전히 `true`다.**

## **문서 목록 조회**

`GET /api/workspaces/{workspaceId}/documents?memberId={memberId}&label={name}` → `200 OK`

`label`은 선택이다. 주면 그 라벨이 붙은 문서만 내려간다.

**목록 응답에는 `content`가 없다.** 10,000자 × N을 목록에 실을 이유가 없다.

```json
[
  {
    "documentId": 1,
    "title": "결제 도메인 설계",
    "currentVersionNo": 1,
    "outdated": false,
    "labels": ["결제", "설계"],
    "uploaderId": 7,
    "createdAt": "2026-09-10T10:24:38.123456Z",
    "updatedAt": "2026-09-10T10:24:38.123456Z"
  }
]
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

## **문서 삭제**

`DELETE /api/workspaces/{workspaceId}/documents/{documentId}?memberId={memberId}` → `204 No Content`

**ADMIN 이상만** 삭제할 수 있다. **소프트 삭제**이며 이후 모든 조회에서 빠진다. 확정된 버전 행과 라벨 연결 행은 함께 지우지 않는다 — 조회가 문서에서 먼저 막히기 때문이다.

## **버전 이력 조회**

`GET /api/workspaces/{workspaceId}/documents/{documentId}/versions?memberId={memberId}` → `200 OK`

`versionNo` 내림차순. **본문을 담지 않는다.**

```json
[
  { "versionNo": 2, "publishedAt": "2026-09-11T09:00:00.000000Z", "dictionaryVersionNo": 3, "publishedBy": 7 },
  { "versionNo": 1, "publishedAt": "2026-09-10T10:24:38.123456Z", "dictionaryVersionNo": null, "publishedBy": 7 }
]
```

> 반영(Revise) 경로가 아직 없어 **현재는 v1 하나만 쌓인다.** v2 이상은 `reviewrequest` 도메인이 붙어야 생긴다.

## **특정 버전 조회**

`GET /api/workspaces/{workspaceId}/documents/{documentId}/versions/{versionNo}?memberId={memberId}` → `200 OK`

위 형식에 `body`가 붙는다.

```json
{
  "versionNo": 1,
  "body": "회원은 결제할 수 있다.",
  "publishedAt": "2026-09-10T10:24:38.123456Z",
  "dictionaryVersionNo": null,
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
| 요청 DTO 검증 실패, `memberId` 누락 | 400 | `COMMON_INVALID_REQUEST` |

> 권한 부족에 document 전용 코드를 두지 않고 `WORKSPACE_ADMIN_REQUIRED`를 그대로 쓴다. 검증 주체가 `WorkspaceAccessValidator`이므로 같은 뜻의 코드를 도메인마다 늘리지 않는다.

---
