# 도메인 문서

## Member

### Member

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | MemberId | O |  |
| 이메일 | email | String | O | 전역 유일 |
| 표시 이름 | displayName | String | O |  |
| 상태 | status | Enum | O | 가입대기 / 활성 / 정지 / 탈퇴 |
| 역할 | role | Enum | O | REGULAR/ADMIN |
| 소셜 제공자 | provider | Enum | O | GOOGLE (MVP1). KAKAO/NAVER는 MVP2 확장 예약 |
| 제공자 식별자 | providerId | String | O | OAuth `sub`. `(provider, providerId)` 조합 유일 |
| 생성일시 | createdAt | DateTime | O |  |
| 수정일시 | updatedAt | DateTime | O |  |

> `Member`는 자기 자신이 가입 주체라 `createdBy`(생성자)를 둘 자연스러운 대상이 없어 다른 엔티티와 달리 생략한다.

---

## Workspace

### Workspace

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | WorkspaceId | O |  |
| 이름 | name | String | O |  |
| 리뷰 규칙 | ruleSet | RuleSet | O | 아래 값 객체. 워크스페이스에 포함된다 |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O |  |
| 수정일시 | updatedAt | DateTime | O |  |

#### RuleSet (리뷰 규칙) — 값 객체

식별자도 생명주기도 따로 두지 않는다. 워크스페이스와 함께 생기고 함께 사라지며, 항상 정확히 1개다. 별도 테이블 없이 `workspace` 테이블의 컬럼으로 둔다.

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 필수 문서 리뷰어 수 | requiredDocumentReviewerCount | Int | O | 승인에 필요한 최소 인원. 기본 0 |
| 필수 사전 리뷰어 수 | requiredDictionaryReviewerCount | Int | O | 승인에 필요한 최소 인원. 기본 0 |

> **정족수는 스냅샷을 두지 않고 발행 시점의 룰셋 값을 실시간 조회한다**(2026-09-10 확정). 룰셋을 올리면 진행 중이던 요청이 발행 불가가 될 수 있는데, GitHub가 브랜치 보호 규칙을 실시간 참조하는 것과 같은 동작이다. 값 객체를 엔티티로 승격하지 않는다.

### Participant

| 속성 | 영문 | 타입 | 필수 | 설명                                     |
| --- | --- | --- | --- |----------------------------------------|
| 식별자 | id | ParticipantId | O |                                        |
| 워크스페이스 | workspaceId | WorkspaceId | O |                                        |
| 회원 | memberId | MemberId | O |                                        |
| 권한 | permission | Enum | O | 소유자(Owner) / 관리자(Admin) / 사용자(Regular) |
| 참여일시 | joinedAt | DateTime | O |                                        |
| 생성일시 | createdAt | DateTime | O |                                        |
| 생성자 | createdBy | MemberId | O |                                        |
| 수정일시 | updatedAt | DateTime | O |                                        |

### Invitation (초대)

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | InvitationId | O |  |
| 워크스페이스 | workspaceId | WorkspaceId | O |  |
| 초대 대상 이메일 | inviteeEmail | String | X | 링크 복사 방식이면 null |
| 초대 토큰 | token | String | O | 초대 링크에 실리는 값. 전역 유일 |
| 부여 권한 | permission | Enum | O | 수락 시 부여할 권한. 관리자(Admin) / 사용자(Regular) |
| 상태 | status | Enum | O | 대기 / 수락 / 만료 / 취소 |
| 만료일시 | expiresAt | DateTime | O |  |
| 수락일시 | acceptedAt | DateTime | X |  |
| 수락 참여자 | acceptedParticipantId | ParticipantId | X | 수락 후 만들어진 참여자 |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O | 초대한 사람 |
| 수정일시 | updatedAt | DateTime | O |  |

---

## Dictionary

### Dictionary

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | DictionaryId | O |  |
| 워크스페이스 | workspaceId | WorkspaceId | O | 워크스페이스당 활성중 1개 + 보관중 N개 |
| 버전 | version | DictionaryVersion | O | 아래 값 객체. 사전집에 포함된다 |
| 상태 | status | Enum | O | 활성중(ACTIVE) / 보관중(ARCHIVED) |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O | 반영을 수행한 사람 |
| 수정일시 | updatedAt | DateTime | O | 상태 전환 외에는 변하지 않음 |

**사전집 행 하나가 확정된 버전 하나다.** 워크스페이스에 사전집 행이 쌓이고, 활성중인 행 하나가 가장 최근 확정본이자 대조의 기준이다. 나머지는 지나간 버전이며 내용이 바뀌지 않는다.

> **삭제 시각 속성을 두지 않는다.** 모든 행이 보존해야 할 버전 이력이라 삭제 경로가 없다. `Term`도 같다.

> **이름(`name`)은 두지 않는다.** 워크스페이스에 활성 사전집이 정확히 1개이므로 워크스페이스와 구분해 부를 이름이 필요 없다. 사전집을 여러 갈래로 풀게 되면 그때 다시 추가한다.

#### DictionaryVersion (사전집 버전) — 값 객체

식별자도 생명주기도 따로 두지 않는다. 사전집과 함께 생기고 함께 사라지며 항상 정확히 1개다. 별도 테이블 없이 `dictionary` 테이블의 컬럼으로 둔다. `RuleSet`이 `Workspace`에 포함된 것과 같은 모양이다.

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 버전 번호 | versionNo | Int | O | 1부터. 워크스페이스 안에서 유일 |
| 확정일시 | publishedAt | DateTime | O | 행이 만들어지는 순간이 확정 순간 |

> **유래 리비전(`originRevisionId`)은 두지 않는다**(2026-09-10 확정). `RevisionDictionary.resultVersionNo`가 같은 관계의 반대 방향이라 정보가 중복이고, 크로스 도메인 논리 FK가 하나 늘어난다. MVP1에 「버전에서 리뷰로 이동」 요구사항이 없다. `DocumentVersion`의 같은 속성도 함께 폐기했다.

### Term

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | TermId | O |  |
| 사전집 | dictionaryId | DictionaryId | O |  |
| 표준어 | preferredForm | String | O | 사전집 내 유일. 100자. 제안어로 제시되는 값 |
| 영문명 | englishName | String | X | 코드·DB 네이밍 기준. 100자 |
| 정의 | definition | Text | O | 대조 시 LLM의 판단 근거 |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O |  |
| 수정일시 | updatedAt | DateTime | O | 생성 이후 변하지 않음 |

용어는 소속 사전집 버전과 함께 얼어붙는다. 개별 용어를 고치는 경로는 없고, 새 버전을 반영할 때 그 버전의 용어가 통째로 새로 쌓인다.

> **동의어·비권장어는 두지 않는다.** 문서 대조는 사전집에 저장된 표기 목록을 훑는 방식이 아니라, **LLM이 문서의 맥락을 파악해 표준어·정의와 비교·대조하고 바꿀 것을 제안하는 방식**이다. 사전집이 대조에 제공하는 것은 "이 워크스페이스의 표준어와 그 뜻" 목록뿐이다.

### ~~TermVersion~~

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | TermVersionId | O |  |
| 사전집 | dictionaryId | DictionaryId | O |  |
| 버전 번호 | versionNo | Int | O |  |
| 용어 스냅샷 | terms | List<Term> | O | 확정 후 불변 |
| 확정일시 | publishedAt | DateTime | O |  |
| 유래 리비전 | originRevisionId | RevisionDictionaryId | X |  |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O |  |
| 수정일시 | updatedAt | DateTime | O | 불변이라 생성 이후 변하지 않음 |

### ~~DictionaryVersion~~ → `Dictionary`에 값 객체로 흡수

엔티티로 두지 않는다. 사전집 행 하나가 곧 확정된 버전 하나이므로 별도 이력 테이블이 필요 없다. 버전 번호와 확정일시는 위 **DictionaryVersion (사전집 버전) — 값 객체**를 본다.

`terms`(용어 스냅샷)도 두지 않는다. **버전마다 사전집 행이 복제되면서 `Term`도 함께 복제되므로, 각 버전에 매달린 `Term`이 곧 그 버전의 스냅샷이다.** 아무도 편집하지 않으므로 저절로 불변이 된다.

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| ~~식별자~~ | ~~id~~ | ~~DictionaryVersionId~~ | ~~O~~ | 값 객체라 식별자를 두지 않는다 |
| ~~사전집~~ | ~~dictionaryId~~ | ~~DictionaryId~~ | ~~O~~ | `Dictionary`에 포함되므로 불필요 |
| 버전 번호 | versionNo | Int | O | 값 객체로 이동 |
| ~~용어 스냅샷~~ | ~~terms~~ | ~~List\<Term\>~~ | ~~O~~ | `Term`이 버전별로 복제되어 대체 |
| 확정일시 | publishedAt | DateTime | O | 값 객체로 이동 |
| ~~유래 리비전~~ | ~~originRevisionId~~ | ~~RevisionDictionaryId~~ | ~~X~~ | **두지 않는다**(2026-09-10 확정) |

---

## Document

### Document

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | DocumentId | O |  |
| 워크스페이스 | workspaceId | WorkspaceId | O |  |
| 제목 | title | String | O | 200자 이내. 워크스페이스 안에서 중복을 허용한다 |
| 현재 버전 | currentVersionNo | Int | O | 업로드 시 1. **직접 편집과 반영(Revise)마다 오른다.** **이 번호가 가리키는 `DocumentVersion`이 현재 본문이다** |
| 라벨 | labels | List\<Label\> | X | 문서당 최대 5개. `DocumentLabel`로 연결한다 |
| 최종수정자 | updaterId | MemberId | O | 업로드 시 `createdBy`로 초기화. 제목·라벨 수정, **직접 편집**, 반영 때 갱신된다 |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O | 업로더 |
| 수정일시 | updatedAt | DateTime | O |  |

> **`Document`에 본문을 두지 않는다.** 본문은 `DocumentVersion.body`에만 있다. 두 곳에 저장하면 반영 때 둘을 함께 갱신해야 하고 어긋난다 — 사전집이 `Term`을 버전 밑에만 둔 것과 같은 이유다. 이전 표의 `content`와 `ownerId`(`createdBy`와 중복)를 제거했다.
>
> **`aligned`(정렬됨) 판별 속성도 두지 않는다.** 최신 `DocumentVersion`의 `dictionaryVersionNo`와 `edited`가 그 값이다. 아래 「정책 · 제약」의 문서 절을 본다. **이전 표기 `outdated`는 폐기했다**(2026-09-10) — 편집본이 사전집 버전을 승계하면서 「낡음」과 「추출 대상 아님」이 갈라져 두 축이 됐고, 하나로 합쳐 `aligned`로 바꿨다.

### DocumentVersion

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | DocumentVersionId | O |  |
| 문서 | documentId | DocumentId | O |  |
| 버전 | version | PublishedVersion | O | 아래 값 객체. 별도 테이블 없이 `document_version` 컬럼 2개 |
| 본문 스냅샷 | body | Text | O | **문서 본문의 유일한 저장 위치.** 10,000자 이내. 확정 후 불변 |
| ~~유래 리비전~~ | ~~originRevisionId~~ | ~~RevisionDocumentId~~ | ~~X~~ | **두지 않는다**(2026-09-10 확정). `RevisionDocument.resultVersionNo`가 같은 관계의 반대 방향이라 정보가 중복이고, 크로스 도메인 논리 FK가 하나 늘어난다. MVP1에 「버전에서 리뷰로 이동」 요구사항이 없다 |
| 기준 사전집 버전 | dictionaryVersionNo | Int | X | 이 버전이 통과한 사전집 버전. **v1(업로드본)은 `null`**, **직접 편집본은 이전 버전 값을 승계**한다. `aligned` 판정의 기준값 |
| 직접 편집 여부 | edited | Boolean | O | **기본 `false`.** 사용자가 대조·리뷰를 거치지 않고 본문을 고쳐 발행한 버전이면 `true`다(2026-09-10 추가). `dictionaryVersionNo`와 함께 `aligned`를 판정한다 |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O |  |
| 수정일시 | updatedAt | DateTime | O | 불변이라 생성 이후 변하지 않음 |

#### PublishedVersion (문서 버전) — 값 객체

식별자도 생명주기도 따로 두지 않는다. 문서 버전과 함께 생기고 함께 사라지며 항상 정확히 1개다. 사전집의 `DictionaryVersion`과 같은 역할이며, 이름이 다른 이유는 그 자리의 엔티티가 이미 `DocumentVersion`이기 때문이다.

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 버전 번호 | versionNo | Int | O | 1부터. 문서 안에서 유일 |
| 확정일시 | publishedAt | DateTime | O | 행이 만들어지는 순간이 확정 순간 |

> 번호와 확정일시를 묶는 이유는 **둘이 함께 파생되기 때문**이다. 다음 버전은 번호를 하나 올리면서 확정 시각을 새로 찍는다 — 한쪽만 바뀌는 경우가 없다.

### Label (라벨)

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | LabelId | O |  |
| 워크스페이스 | workspaceId | WorkspaceId | O | 라벨은 워크스페이스가 소유한다 |
| 이름 | name | String | O | 1~20자. **워크스페이스 안에서 유일.** 앞뒤 공백을 제거한 뒤 대소문자를 구분한다 |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O |  |
| 수정일시 | updatedAt | DateTime | O |  |

### DocumentLabel (문서-라벨 연결)

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | DocumentLabelId | O |  |
| 문서 | documentId | DocumentId | O |  |
| 라벨 | labelId | LabelId | O | `(documentId, labelId)` 유일 |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O |  |
| 수정일시 | updatedAt | DateTime | O |  |

## DraftDocument

### ~~DictionaryContrast~~ → 1차 mvp에서는 없는 기능이라 제외

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | DictionaryContrastId | O |  |
| 대상 문서 | documentId | DocumentId | O |  |
| 대상 버전 | targetVersionNo | Int | X | 작업 본문 대상이면 null |
| 기준 사전집 버전 | dictionaryId | Int | O | 재현성 확보 |
| 요청자 | requestedBy | MemberId | X | 자동 실행이면 null |
| 상태 | status | Enum | O | 대기 / 실행중 / 성공 / 실패 |
| 결과 초안 문서 | draftDocumentId | DraftDocumentId | X |  |
| 실행일시 | executedAt | DateTime | O |  |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O | 자동 실행이면 시스템 |
| 수정일시 | updatedAt | DateTime | O |  |

### DraftDocument

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | DraftDocumentId | O |  |
| 원본 문서 | documentId | DocumentId | O |  |
| 기준 문서 버전 | baseVersionNo | Int | O | 어느 버전에서 갈라졌는지 |
| 초안 본문 | draftBody | Text | O | **교정완료 시 생성되는 본문.** 교정 중에는 원본 그대로다 — 수용된 제안어만 모아 뒤에서 앞으로 한 번에 치환한다(2026-09-10 확정) |
| ~~제안어~~ | ~~suggestions~~ | ~~List<SuggestionTerm>~~ | ~~X~~ | 자식 FK 단방향. 컬렉션으로 매핑하지 않는다 |
| 상태 | status | Enum | O | 교정중 / 교정완료 / 리뷰요청됨 / 반영완료 |
| 요청자 | requestedBy | MemberId | X | 교정할 사람 |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O |  |
| 수정일시 | updatedAt | DateTime | O |  |

### SuggestionTerm (제안어)

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | SuggestionTermId | O |  |
| 초안 문서 | draftDocumentId | DraftDocumentId | O |  |
| 위치 | anchor | TextRange | O | 본문 내 시작·끝 |
| 기존 용어 | originTerm | String | O | 문서에 실제로 쓰인 비표준 표현 |
| 제안 용어 | suggestionTerm | String | O | 사전집 참조. 표기 복사 금지 |
| 상태 | status | Enum | O | 처리 전 / 기존 용어 / 제안 용어 |
| 처리자 | handledBy | MemberId | X |  |
| 거절 사유 | rejectReason | Text | X | 인용·고유명사 등. **거절 시에는 필수**다(`REQ-UPD-002`). 수용된 제안어에서는 비므로 컬럼 자체는 nullable |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O |  |
| 수정일시 | updatedAt | DateTime | O |  |

### ~~Examine (교정)~~

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | ExamineId | O |  |
| 초안 문서 | draftDocumentId | DraftDocumentId | O |  |
| 수행자 | performedBy | MemberId | O |  |
| 회차 | round | Int | O | 1이 최초 교정, 2 이상이 재교정 |
| 처리 건수 | handledCount | Int | O | 이번 회차에 처리한 제안어 수 |
| 수행일시 | performedAt | DateTime | O |  |
| 생성일시 | createdAt | DateTime | O | `performedAt`과 중복 소지 |
| 생성자 | createdBy | MemberId | O | `performedBy`와 중복 소지 |
| 수정일시 | updatedAt | DateTime | O | 이력이라 생성 이후 변하지 않음 |

## DraftDictionary

### ~~ExtractTerm~~ (용어 추출)

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | ExtractTermId | O |  |
| 대상 문서 | documentId | DocumentId | O |  |
| 기준 사전집 버전 | dictionaryVersionNo | Int | O | 이미 있는 용어는 후보에서 제외 |
| 요청자 | requestedBy | MemberId | X |  |
| 상태 | status | Enum | O | 대기 / 실행중 / 성공 / 실패 |
| 결과 초안 사전 | draftDictionaryId | DraftDictionaryId | X |  |
| 추출 건수 | extractedCount | Int | O |  |
| 실행일시 | executedAt | DateTime | O |  |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O | 자동 실행이면 시스템 |
| 수정일시 | updatedAt | DateTime | O |  |

### DraftDictionary

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | DraftDictionaryId | O |  |
| 워크스페이스 | workspaceId | WorkspaceId | O |  |
| 대상 사전집 | dictionaryId | DictionaryId | **X** | 어디에 등재될지. **사전집이 없는 첫 회차는 `null`이다**(2026-09-10 정정) — 첫 사전집이 초안 → 리뷰 → 발행으로 태어나므로 그때는 가리킬 대상이 없다. 발행은 `workspaceId`로 「이 워크스페이스의 다음 버전」을 만든다 |
| 유래 문서 | sourceDocumentIds | List&lt;DocumentId&gt; | O | 여러 문서에서 모을 수 있음. 필드명이 복수이므로 **타입도 복수**다(2026-09-10 정정) |
| ~~후보어~~ | ~~candidates~~ | ~~List<CandidateTerm>~~ | ~~X~~ | 자식 FK 단방향. 후보어가 수백 건까지 늘 수 있어 컬렉션으로 매핑하지 않는다 |
| 상태 | status | Enum | O | 교정중 / 교정완료 / 리뷰요청됨 / 반영완료 |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O |  |
| 수정일시 | updatedAt | DateTime | O |  |

> **초안은 「이전 사전집 + 추출 용어」의 통합 결과다**(2026-09-10 확정). `CandidateTerm`이 두 출처를 `origin`으로 구분해 함께 담고, 발행은 그 최종 목록을 그대로 받아 새 버전으로 만든다.

### CandidateTerm (후보어)

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | CandidateTermId | O |  |
| 초안 사전 | draftDictionaryId | DraftDictionaryId | O |  |
| 표기 | form | String | O | 이 항목의 표기. 추출된 표현이거나 이전 사전집의 표준어다 |
| 원천 | origin | Enum | O | **추출됨(EXTRACTED) / 기존(EXISTING)**(2026-09-10 추가). 통합 모델에서 초안이 두 출처를 함께 담기 때문이다 |
| 원본 용어 | sourceTermId | TermId | X | `origin`이 `기존`일 때 이전 사전집의 `Term`(2026-09-10 추가) |
| 제안 정의 | proposedDefinition | Text | X | AI 초안 또는 사람 작성 |
| 제안 영문명 | proposedEnglishName | String | X |  |
| 출현 문서 | occurredDocumentIds | List<DocumentId> | X | 어디서 몇 번 나왔는지의 근거. **`origin`이 `기존`이면 빈다** |
| 출현 횟수 | occurrenceCount | Int | X | 등재 우선순위 판단. **`origin`이 `기존`이면 빈다** |
| 문맥 조각 | contextSnippets | List<Text> | X | 검토 시 판단 근거. **`origin`이 `기존`이면 빈다** |
| 처리 상태 | status | Enum | O | 대기 / **유지** / 등재승인 / 동의어편입 / 거절 / 보류. **`유지`는 `origin`이 `기존`인 항목의 기본값**이며 「이전 버전 그대로 다음 버전에 실린다」는 뜻이다(2026-09-10 추가) |
| 처리 결과 용어 | resultTermId | TermId | X | 승인 후 생성된 표준 용어 |
| 편입 대상 용어 | mergeTargetTermId | TermId | X | `동의어편입` 시 필수. **어느 대표어와 같은 개념이라 따로 등재하지 않는지**를 가리킨다. 사전집은 동의어를 저장하지 않으므로 판정만 남고 `Term`에는 아무것도 추가되지 않는다. `resultTermId`와 의미가 다르다(2026-09-10 추가) |
| 거절 사유 | rejectReason | Text | X | `거절` 시 필수(`REQ-REV-006`). `SuggestionTerm.rejectReason`과 대칭(2026-09-10 추가) |
| 처리자 | handledBy | MemberId | X | `REQ-REV-007`의 "누가". "언제"는 `updatedAt`, "왜"는 `rejectReason`이 담는다(2026-09-10 추가) |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O |  |
| 수정일시 | updatedAt | DateTime | O |  |

## ReviewRequest

### ReviewRequest

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | ReviewRequestId | O |  |
| 워크스페이스 | workspaceId | WorkspaceId | O |  |
| 요청 유형 | type | Enum | O | DOCUMENT / DICTIONARY |
| ~~대상 리비전~~ | ~~revisionId~~ | ~~UUID~~ | ~~O~~ | **삭제.** DB로 지킬 수 없는 다형 FK였다. **리비전이 `reviewRequestId`를 소유**하고 조회는 `findByReviewRequestId`로 한다(2026-09-10 확정) |
| 제목 | title | String | O |  |
| 요청자 | requesterId | MemberId | O |  |
| ~~리뷰어~~ | ~~reviewers~~ | ~~List<Reviewer>~~ | ~~O~~ | 자식 FK 단방향. 컬렉션으로 매핑하지 않는다. 리뷰어 0명도 정상 상태다 |
| 상태 | status | Enum | O | 리뷰대기 / 리뷰중 / 변경요청 / 승인 / 반영완료 / 취소 — **`반려`는 두지 않는다**(GitHub PR의 close에 대응하는 `취소`만 둔다) |
| 최종 승인일시 | approvedAt | DateTime | X | 정족수를 처음 충족한 시점. 변경요청으로 되돌아갔다가 다시 충족하면 갱신한다 |
| 반영일시 | revisedAt | DateTime | X |  |
| 낙관적 락 버전 | version | Long | O | `NFR-REV-002` 동시 수정 제어 |
| 내용 | description | String | X |  |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O | `requesterId`와 중복 소지 |
| 수정일시 | updatedAt | DateTime | O |  |

### RevisionDocument (문서 리비전)

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | RevisionDocumentId | O |  |
| 리뷰 요청 | reviewRequestId | ReviewRequestId | O |  |
| 대상 문서 | documentId | DocumentId | O |  |
| 기준 버전 | baseVersionNo | Int | O |  |
| 유래 초안 문서 | draftDocumentId | DraftDocumentId | O |  |
| 변경 본문 | proposedBody | Text | O |  |
| 재교정 회차 | reexamineRound | Int | O | 0이 최초 제출 |
| 결과 문서 버전 | resultVersionNo | Int | X | Revise 후 채워짐 |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O |  |
| 수정일시 | updatedAt | DateTime | O |  |

### RevisionDictionary (사전 리비전)

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | RevisionDictionaryId | O |  |
| 리뷰 요청 | reviewRequestId | ReviewRequestId | O |  |
| 대상 사전집 | dictionaryId | DictionaryId | O |  |
| 기준 버전 | baseVersionNo | Int | O |  |
| 유래 초안 사전 | draftDictionaryId | DraftDictionaryId | O | 등재 제안 항목은 이 초안을 다시 조회해 얻는다 |
| ~~등재 제안~~ | ~~proposedTerms~~ | ~~List<CandidateTerm>~~ | ~~O~~ | **미사용.** 다른 도메인 엔티티를 그대로 담아 경계를 깨뜨린다. `draftDictionaryId` 참조로 대체했다(2026-09-10 확정) |
| 재교정 회차 | reexamineRound | Int | O | **0이 최초 제출**(`RevisionDocument`와 동일) |
| 결과 사전집 버전 | resultVersionNo | Int | X |  |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O |  |
| 수정일시 | updatedAt | DateTime | O |  |

> 리뷰 당시 항목 내용은 **초안 잠금으로 보장한다.** 초안이 `리뷰요청됨`인 동안 후보어를 바꿀 수 없으므로 초안 자체가 스냅샷 역할을 한다. 단 **변경요청으로 초안을 다시 열면 이전 회차 내용은 재현할 수 없다** — 이 한계를 받아들이고 스냅샷 엔티티를 두지 않기로 확정했다(2026-09-10).

### ~~RevisedTerm (제안어)~~

**MVP1 제외로 확정했다(2026-09-10).** 이유는 셋이다.

- `RevisionDocument.proposedBody`가 **본문 전체를 스냅샷으로 담으므로** 항목 단위 스냅샷을 따로 둘 이유가 약하다.
- 절 제목의 한글명이 `SuggestionTerm`과 똑같이 "제안어"여서 **유비쿼터스 언어 원칙에 위반**된다(`docs/UBIQUITOUS_LANGUAGE.md`는 "제안어"를 `SuggestionTerm`에 배정했다).
- FK가 `draftDocumentId`라 ReviewRequest 엔티티가 **DraftDocument를 직접 참조**한다(`docs/ARCHITECTURE.md`의 크로스 도메인 규약 위반).

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| ~~식별자~~ | ~~id~~ | ~~RevisedTermId~~ | ~~O~~ |  |
| ~~초안 문서~~ | ~~draftDocumentId~~ | ~~DraftDocumentId~~ | ~~O~~ |  |
| ~~위치~~ | ~~anchor~~ | ~~TextRange~~ | ~~O~~ | ~~본문 내 시작·끝~~ |
| ~~기존 용어~~ | ~~originTerm~~ | ~~String~~ | ~~O~~ | ~~문서에 실제로 쓰인 비표준 표현~~ |
| ~~제안 용어~~ | ~~suggestionTerm~~ | ~~String~~ | ~~O~~ | ~~사전집 참조. 표기 복사 금지~~ |
| ~~상태~~ | ~~status~~ | ~~Enum~~ | ~~O~~ | ~~처리 전 / 기존 용어 / 제안 용어~~ |
| ~~생성일시~~ | ~~createdAt~~ | ~~DateTime~~ | ~~O~~ |  |
| ~~생성자~~ | ~~createdBy~~ | ~~MemberId~~ | ~~O~~ |  |
| ~~수정일시~~ | ~~updatedAt~~ | ~~DateTime~~ | ~~O~~ |  |

### Reviewer

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | ReviewerId | O |  |
| 리뷰 요청 | reviewRequestId | ReviewRequestId | O |  |
| 회원 | memberId | MemberId | O | 워크스페이스 참여자여야 함 |
| ~~필수 여부~~ | ~~required~~ | ~~Boolean~~ | ~~O~~ | **미사용.** 지정은 강제가 아니라 알림·필터 용도이고, 지정 여부와 무관하게 참여자면 리뷰할 수 있다 |
| 지정일시 | assignedAt | DateTime | O |  |
| 생성일시 | createdAt | DateTime | O | `assignedAt`과 중복 소지 |
| 생성자 | createdBy | MemberId | O | 리뷰어를 지정한 사람 |
| 수정일시 | updatedAt | DateTime | O |  |

### Review

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | ReviewId | O |  |
| 리뷰 요청 | reviewRequestId | ReviewRequestId | O |  |
| 회원 | memberId | MemberId | O | 지정되지 않은 참여자도 리뷰할 수 있으므로 회원을 직접 가리킨다. **집계 기준 키** |
| 대상 회차 | targetRound | Int | O | 어느 리비전 회차를 봤는지(0이 최초 제출). **이력·표시용이고 집계 필터로 쓰지 않는다** |
| 판정 | verdict | Enum | O | 승인 / 변경요청 |
| ~~코멘트~~ | ~~comments~~ | ~~List<Comment>~~ | ~~X~~ | 자식 FK 단방향. 컬렉션으로 매핑하지 않는다 |
| 제출일시 | submittedAt | DateTime | O |  |
| 생성일시 | createdAt | DateTime | O | 초안 저장을 허용하면 `submittedAt`과 달라짐 |
| 생성자 | createdBy | MemberId | O |  |
| 수정일시 | updatedAt | DateTime | O |  |

> `Review`는 **이력이라 제출 후 판정을 수정하지 않는다.** 같은 회원이 판정을 바꾸려면 새 `Review`를 제출하고, **집계는 회원별 최신 1건만 본다.** 따라서 `(reviewRequestId, memberId, targetRound)` 유니크를 걸지 않는다. 재교정으로 회차가 올라가도 이전 승인은 유지된다(GitHub 기본 동작).

### Comment

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | CommentId | O |  |
| 리뷰 | reviewId | ReviewId | O |  |
| 작성자 | authorId | MemberId | O | 사람만 |
| 내용 | content | Text | O |  |
| 위치 | anchor | TextRange | X | 전체 대상이면 null |
| 대상 항목 | targetItemId | CandidateTermId | X | 사전 리비전에서는 후보어 단위 |
| 상위 코멘트 | parentId | CommentId | X | 답글 |
| 해결 여부 | resolved | Boolean | O |  |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O | `authorId`와 중복 소지 |
| 수정일시 | updatedAt | DateTime | O |  |

### Reexamine (재교정)

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | ReexamineId | O |  |
| 리뷰 요청 | reviewRequestId | ReviewRequestId | O | 리비전은 `(reviewRequestId, round)`로 찾을 수 있어 요청을 직접 가리킨다 |
| 회차 | round | Int | O | 1부터 증가. 리비전의 `reexamineRound`와 같은 값을 가리킨다(0은 최초 제출이라 재교정 이력이 없음) |
| 수행자 | performedBy | MemberId | O | 보통 요청자 |
| 반영한 코멘트 | addressedCommentIds | List<CommentId> | X |  |
| 수행일시 | performedAt | DateTime | O |  |
| 생성일시 | createdAt | DateTime | O | `performedAt`과 중복 소지 |
| 생성자 | createdBy | MemberId | O | `performedBy`와 중복 소지 |
| 수정일시 | updatedAt | DateTime | O | 이력이라 생성 이후 변하지 않음 |

### Revise (반영)

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | ReviseId | O |  |
| 리뷰 요청 | reviewRequestId | ReviewRequestId | O |  |
| 결과 버전 번호 | resultVersionNo | Int | O | 새로 만들어진 버전 |
| 수행자 | performedBy | MemberId | O |  |
| 수행일시 | performedAt | DateTime | O |  |
| 생성일시 | createdAt | DateTime | O | `performedAt`과 중복 소지 |
| 생성자 | createdBy | MemberId | O | `performedBy`와 중복 소지 |
| 수정일시 | updatedAt | DateTime | O | 이력이라 생성 이후 변하지 않음 |

## Notification

### Notification

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | NotificationId | O |  |
| 수신자 | recipientId | MemberId | O |  |
| 워크스페이스 | workspaceId | WorkspaceId | O |  |
| 유형 | type | Enum | O | 리뷰요청도착 / 코멘트등록 / 승인 / 변경요청 / 반영완료 / 취소 / 대조완료 / 추출완료 — `반려`는 `ReviewRequest.status`에서 삭제됐다(2026-09-10) |
| 대상 | targetType, targetId | Enum, UUID | O | 눌렀을 때 이동할 곳 |
| 본문 | message | String | O |  |
| 채널 | channels | List<Enum> | O | 인앱 / 이메일 / 슬랙 |
| 읽음 | readAt | DateTime | X | null이면 안 읽음 |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O | 시스템 발행이면 시스템 |
| 수정일시 | updatedAt | DateTime | O | 읽음 처리 시점과 연동 |

---

## 정책 · 제약

### 워크스페이스 · 권한
- 워크스페이스 **참여자는 최대 5명**이다. 정원이 찬 워크스페이스에는 초대할 수 없다.
- 참여자 권한은 **소유자(Owner) / 관리자(Admin) / 사용자(Regular)** 3단계다. (`Member.role`의 사이트 레벨 권한도 `REGULAR/ADMIN`으로, 일반 등급을 가리키는 이름을 두 레벨에서 일부러 통일했다 — `Participant.permission`은 워크스페이스 단위, `Member.role`은 사이트 단위로 범위가 다르다.)
- **Owner와 Admin은 사실상 동급 권한이다.** Owner는 별도 상위 권한이 아니라 워크스페이스 생성자에게 붙는 명칭일 뿐이며, 할 수 있는 일은 Admin과 같다(9/8 확정). **단 워크스페이스 삭제만 Owner 전용이다**(아래 «워크스페이스 생성 · 삭제» 참고).
- **Admin 이상(Owner 포함)만 가능한 기능**: **용어 추출**, **문서 삭제**, **멤버 초대**, **워크스페이스 설정**(이름·설명 수정 등, REQ-WS-006).
- Regular는 문서 작성·교정·리뷰까지만 가능하다.

### 워크스페이스 생성 · 삭제
- 워크스페이스를 만든 회원은 **Owner 참여자로 자동 등록**된다. 워크스페이스에는 **Owner가 정확히 1명** 있다.
- 룰셋은 워크스페이스에 포함된 값이라 **기본값 0 / 0으로 함께 저장된다.** 따로 만들 행은 없다.
- 이름 변경은 Admin 이상, **삭제는 Owner만** 가능하다.

### 워크스페이스 설정 — 참여자

**초대 (Invitation)**
- Admin 이상이 초대 링크(토큰)를 발급하고, 받은 사람이 링크를 수락하면 참여자로 등록된다. 이메일 발송은 후순위다.
- 정원 검사는 **수락 시점**에 한다. 그 사이 정원이 찼으면 수락은 실패한다.
- **이미 참여자인 회원**에게는 초대를 발급하지 않는다. 같은 워크스페이스·같은 대상에 **대기 상태 초대는 1개**만 둔다.
- 만료·취소된 토큰으로는 수락할 수 없다.
- 초대로 줄 수 있는 권한은 **Admin / Regular**다. Owner는 초대로 부여하지 않는다.

**삭제**
- **Owner는 삭제 대상이 아니다.** Admin은 **Regular만** 내보낼 수 있고, Admin을 내보내는 것은 Owner만 가능하다.
- 자기 자신은 내보내기로 처리하지 않는다(탈퇴로 처리).
- 내보낸 참여자가 남긴 문서·리뷰·코멘트는 그대로 유지된다.
- 진행 중인 리뷰 요청에서 그 참여자가 리뷰어였다면 리뷰어에서 제외되고, 필요 리뷰어 수는 아래 룰셋 규칙을 따른다.

**권한 설정**
- 소유권 이전은 기존 Owner가 다른 참여자를 Owner로 지정하는 방식이고, 이때 기존 Owner는 Admin이 된다.
- 권한을 낮춰도 그 참여자가 Admin 권한으로 이미 한 작업은 되돌리지 않는다.

### 워크스페이스 설정 — 리뷰 규칙 (RuleSet)
- 룰셋은 **Workspace에 포함된 값 객체**다. 워크스페이스당 1개이고, 생성 시 기본값(0 / 0)으로 함께 저장된다.
- `requiredDocumentReviewerCount`, `requiredDictionaryReviewerCount`는 각각 **0 이상, 워크스페이스 참여자 수 이하**다(현 정원 기준 **0~5**).
- **0이면 리뷰어 없이 바로 반영**할 수 있다. 승인·변경요청 존재 여부와 무관하게 Admin 이상이 언제든 발행할 수 있다.
- **상한(참여자 수 이하) 검증은 룰셋을 설정·수정하는 Workspace 쪽에서 한다.** 리뷰 요청 도메인은 룰셋 값을 그대로 신뢰하고 참여자 수를 조회하지 않는다. 참여자가 빠져 정족수를 채울 수 없게 되면 Admin 이상이 발행 조건을 우회해 처리한다. (2026-09-10 확정)
- **리뷰어 수 제약은 요청 생성 제약이 아니라 발행 제약이다.** 즉 "지정한 리뷰어가 설정값보다 적으면 요청을 만들 수 없다"가 아니라, **승인한 회원 수가 설정값보다 적으면 리비전을 발행할 수 없다**는 뜻이다. 요청 생성 시점에는 리뷰어 수를 검증하지 않는다. (2026-09-10 확정)
- 리뷰어 **지정은 알림·목록 필터 용도이고 리뷰 자격을 제한하지 않는다.** 지정되지 않은 워크스페이스 참여자도 리뷰를 달 수 있고 정족수에 산입된다.

### 사전집

**생성 주기** — 사전집은 사람이 빈 껍데기를 만드는 것이 아니라, 문서에서 용어를 추출해 리뷰를 통과시킨 결과로 태어난다.

```
워크스페이스 생성            사전집 없음
  ↓  문서 업로드
용어 추출 (Admin 이상)       ① 추출할 문서를 고른다 (aligned인 문서만)
                            ② 문서별 용어 추출
                            ③ 유사 의미 용어를 묶어 후보어 생성
                               (a-회원 · b-User · c-사용자 → 후보어 1개)
                            ④ 이전 버전 사전집과 통합       ← 첫 회차는 사전집이 없어 생략
  ↓
DraftDictionary             ⑤ 요청자가 항목마다 판정한다 → 초안 UPDATE
                               추출된 것: 등재승인 / 동의어편입 / 거절 / 보류
                               기존 것:   유지(기본) / 수정 / 삭제
  ↓  리뷰 요청 (이전 버전과 달라진 항목이 1건 이상)
RevisionDictionary          ⑥⑦ 리뷰 진행. 변경요청이면 초안을 다시 열어 UPDATE
  ↓  정족수 충족 → Admin 이상이 반영
Dictionary (새 버전)        ⑧ 초안의 최종 용어 목록이 그대로 새 버전이 된다 ← 사전집 최종
```

**④가 이 흐름의 핵심이다**(2026-09-10 확정). 초안은 신규 후보어만 모은 집합이 아니라 **차기 버전에 실릴 전체 용어 목록**이다. 그래서 ⑧이 「목록을 그대로 새 버전으로」가 되고, 사전집이 이전 버전을 복사하는 로직을 갖지 않는다.

- 워크스페이스를 만든 직후에는 **사전집이 없다.** 첫 사전집은 위 흐름을 거쳐 v1로 태어난다.
- 워크스페이스에 사전집 행은 여러 개지만 **활성중인 것은 정확히 1개**다. 그 행이 **가장 최근 확정본이자 문서 대조의 기준**이다.
- **새 버전은 리뷰 승인(Revise)의 반영으로만 생긴다.** 반영하면 기존 활성 사전집이 보관중으로 내려가고, 새 행이 다음 버전 번호로 활성중이 된다.
  - **정족수를 채우면 Admin 이상이 수동으로 반영한다**(2026-09-10 확정). 승인이 자동 발행을 일으키지 않는다 — `NFR-UPD-001`(Human-in-the-Loop).
  - 반영은 **초안이 만든 통합 용어 목록을 그대로 받아** 새 버전으로 만든다. 이전 버전에서 복사하지 않는다.
- **모든 사전집 행과 그 용어는 확정된 뒤 바뀌지 않는다.** 상태 전환만 예외다. 따라서 개별 용어를 추가·수정·삭제하는 경로가 없고, 용어를 바꾸려면 새 버전을 만들어야 한다.
- **사전집은 삭제하지 않는다.** 모든 행이 보존해야 할 버전 이력이다.
- **표준어는 사전집 내 유일**하다. 앞뒤 공백을 제거한 뒤 대소문자를 구분해 비교한다.
- **용어가 0개인 버전은 만들 수 없다.** 빈 사전집은 대조에 쓸 수 없다.
- 생성·반영은 **Admin 이상**, 조회는 참여자면 누구나 가능하다.
- **대조는 사전집에 저장된 동의어 목록을 훑는 방식이 아니다.** 사전집에는 표준어와 정의만 있고, LLM이 문서의 맥락을 파악해 그것과 비교·대조하여 바꿀 것을 제안한다.

> **동시 반영을 따로 막지 않는 이유** — 아래 「초안 사전」 정책이 사전집당 진행 중인 등재 흐름을 1개로 제한하므로, 같은 버전을 기준으로 편집하는 주체가 둘이 될 수 없다. 기준 버전은 `DraftDictionary.dictionaryId`와 `RevisionDictionary.baseVersionNo`가 이미 들고 있고, 검사가 필요해지면 그 자리는 **리뷰 승인 시점**이다.

### 문서
- 업로드 대상은 **`txt`, `md`**지만 **파일을 서버로 보내지 않는다**(2026-09-10 확정). 브라우저가 파일을 읽어 **요청 본문의 텍스트로 담아 보내고** 서버는 RDBMS에 직접 저장한다. multipart·S3·NoSQL을 쓰지 않는다.
  - 그래서 **확장자·크기·인코딩 검증은 클라이언트 책임**이다. 서버가 파일을 받지 않으므로 검증할 수단이 없다. 서버는 길이 검증(제목 200자·본문 10,000자)만 한다(`NFR-DOC-001`).
- 문서 본문(`DocumentVersion.body`)은 **10,000자 이내**다. (AI 토큰 사용량에 비례하므로 가볍게 유지) 제목은 200자 이내다.
- **문서 본문은 확정 버전에만 있다.** 업로드는 `Document`와 `DocumentVersion` v1을 한 트랜잭션에서 만든다. 확정된 버전은 불변이다.
- **본문이 바뀌는 경로는 셋이다**(2026-09-10 확정).

| 경로 | 리뷰를 거치는가 | `dictionaryVersionNo` | `edited` |
| --- | --- | --- | --- |
| 업로드(v1) | 아니다 | `null` | `false` |
| **직접 편집** | **아니다 — 즉시 새 버전을 발행한다** | **이전 버전 값 승계** | **`true`** |
| 갱신 → 교정 → 리뷰 → 반영 | 그렇다 | **발행 시점 활성 사전집 버전** | `false` |

- **직접 편집은 대조·초안·개정안을 거치지 않는다.** 사용자가 본문을 고쳐 저장하면 그 자리에서 새 리비전이 된다. 대신 그 버전이 `edited = true`가 되어 추출 대상에서 빠지고, 「갱신」을 실행해야 다시 정렬된다.
- **대조의 진입점은 「최신 사전집으로 갱신」 하나다.** 최신 확정 본문을 대조에 넣어 `DraftDocument`를 만든다. **사전집이 없으면 갱신을 실행할 수 없다.**
- **업로드는 대조를 자동으로 걸지 않는다.** 사전집이 있는 상태에서 올라온 문서는 정렬되지 않은 상태로 남아 있다가 사용자가 갱신을 눌러야 대조된다. (`REQ-CHK-001` 「업로드 시 자동 대조」는 이 결정으로 **폐기**했다 — 2026-09-10)
- **`aligned`(정렬됨)는 저장된 상태가 아니라 파생 판정이다.** **최신 확정 버전**의 `dictionaryVersionNo`가 활성 사전집의 `versionNo`와 같고 **`edited`가 `false`**여야 참이다. 활성 사전집이 없으면 양쪽이 `null`이라 참이 된다.
  - 기준이 「마지막으로 대조한 시점」이 아니라 「마지막으로 반영된 버전」이므로 **대조만 하고 교정을 끝내지 않은 문서는 정렬되지 않은 상태다.**
  - **`edited`가 별도 축인 이유** — 편집본이 이전 사전집 버전을 승계하므로 버전 번호만 보면 「통과했다」로 읽힌다. 사람이 손댄 본문은 그 사전집을 통과한 적이 없다.
- **정렬되지 않은 문서는 용어 추출 대상이 아니다.** 추출 허용 조건은 `최신 확정 버전의 dictionaryVersionNo == 활성 사전집 versionNo` **AND** `edited == false`다. 사전집이 없는 첫 회차에는 기준 버전이 `null`인 문서도 대상이 된다.
- **진행 중인 초안이 걸린 문서는 직접 편집할 수 없다**(2026-09-10 확정). 두 경우다.
  - 그 문서에 진행 중인 **문서 초안**이 있다 — 편집하면 초안의 `baseVersionNo`가 낡고, 그 초안이 발행되면 사용자가 고친 내용을 덮어써 날린다.
  - 그 문서가 진행 중인 **사전 초안의 원천 문서**다 — 추출은 정렬된 문서만 대상으로 하는데, 초안 진행 중에 원천이 편집되면 초안이 이미 존재하지 않는 본문을 근거로 삼는다.

### 초안 공통 — 상태 축과 잠금 (2026-09-10 확정)

`DraftDocument`와 `DraftDictionary`는 **같은 4상태 축**을 쓴다.

| 상태 | 의미 | 이 상태에서만 되는 일 |
| --- | --- | --- |
| 교정중 | 사람이 제안어·후보어를 처리하는 중 | 항목 수용·거절·판정, 항목 추가·수정·삭제 |
| 교정완료 | 미처리 항목이 0건이 되어 리뷰 요청 자격을 얻음 | 리뷰 요청 생성 |
| 리뷰요청됨 | 리뷰 요청이 만들어져 진행 중 | 같은 대상에 새 초안 생성 차단 |
| 반영완료 | 리뷰가 발행되어 원본에 반영됨. **종단** | 없음 — 이력으로만 남는다 |

- **교정중이면 항목 판정을 자유롭게 바꿀 수 있고, 교정완료 이후에는 잠긴다.** 감사 기록은 최종 상태 + `handledBy` + `updatedAt`으로 남기고 **변경 이력 엔티티는 두지 않는다.**
- 전이는 `교정중 → 교정완료 → 리뷰요청됨 → 반영완료`이고, 리뷰가 **취소되면 `교정완료`로** 되돌아가 다시 요청할 수 있다.
- `DraftDictionary`만 **변경요청 시 `교정중`으로** 되돌아간다. 후보어 판정을 다시 해야 새 회차 개정안을 만들 수 있기 때문이다. `DraftDocument`는 재교정 본문을 리뷰 요청 쪽에서 직접 받으므로 초안을 다시 열지 않는다.
- **문서 삭제는 Admin 이상**이고 **소프트 삭제**다. 버전 행은 함께 지우지 않는다. **파생 데이터(초안·리뷰) 정리는 삭제 이벤트를 각 도메인이 구독해 처리한다** — `document`가 초안 도메인을 참조하지 않는다.
- 제목은 **워크스페이스 안에서 중복을 허용**한다.

### 라벨 (Label)
- **라벨은 워크스페이스가 소유한다.** 이름은 워크스페이스 안에서 유일하고, **문서당 최대 5개**다.
- 문서에 **없는 이름을 붙이면 라벨이 새로 만들어진다.** 별도의 라벨 생성 절차를 두지 않는다.
- **문서에서 라벨을 떼도 라벨 자체는 남는다.**
- 문서 목록은 라벨로 **필터링**할 수 있다.

### 초안 — 워크스페이스 단위 상호 배타 (2026-09-10 확정)

**워크스페이스에 진행 중인 초안 흐름은 문서든 사전이든 하나뿐이다.**

| 진행 중인 것 | 막히는 것 |
| --- | --- |
| 문서 초안 또는 문서 개정안 | **사전집 초안 생성**, 같은 문서에 추가 초안·개정안 |
| 사전집 초안 또는 사전 개정안 | **문서 초안 생성**, 같은 사전집에 추가 초안·개정안 |

**왜 도메인을 가로질러 막는가** — 문서 갱신은 사전집 v_N으로 대조하는데 사전 개정이 동시에 v_N+1을 만들면, 문서 개정안이 발행될 때 「발행 시점 활성 사전집 버전」이 이미 v_N+1이어서 **대조하지 않은 버전을 기준으로 표시**하게 된다. 이 제약이 그 구멍을 막는다.

- **「진행 중」은 초안 `status != 반영완료` AND 삭제되지 않음**이다. `리뷰요청됨` 상태가 개정안 진행을 함의하므로 **초안 조회 하나로 초안과 개정안을 함께 판정한다.**
- **DB 유니크로 걸지 않고 애플리케이션에서 검증한다** — 소프트 삭제를 쓰는데 MySQL 8.4에 부분 유니크 인덱스가 없다.

### 초안 (DraftDocument)
- **한 문서당 진행 중 초안은 1개만** 존재할 수 있다.
- 해당 문서가 **리뷰 요청 중이면 초안을 생성할 수 없다.** (`ReviewRequest.status`가 리뷰대기 / 리뷰중 / 변경요청인 동안)
- **사전집이 없으면 초안을 만들 수 없다** — 갱신은 대조이고 대조에는 기준 사전집이 필요하다.

### 초안 사전 (DraftDictionary)
- 사전집에 **초안(DraftDictionary) 또는 개정안(RevisionDictionary)이 존재하면 추가 초안을 생성할 수 없다.** 사전집당 진행 중인 등재 흐름은 **1개**다.
- **초안은 「이전 사전집 + 추출 용어」의 통합 결과다**(2026-09-10 확정). 신규 후보어만 모은 집합이 아니라 **차기 버전에 실릴 전체 용어 목록**을 든다. 사전집이 없는 첫 회차는 추출 결과만으로 구성된다.
  - 그래서 **사람이 용어를 추가·수정·삭제하는 자리가 초안 교정이다**(`REQ-DIC-004`). 확정된 사전집의 `Term`은 고칠 수 없다.
  - 발행은 초안의 최종 목록을 그대로 받아 새 버전으로 만든다 — 이전 버전에서 복사하지 않는다.
- **초안 생성·교정 주체는 Admin 이상**이다.
- **이전 버전과 달라진 항목이 0건이면 리뷰 요청을 만들 수 없다.** 기존 용어를 전부 유지하고 신규가 없으면 변경 없는 개정안이 된다(2026-09-10 확정).
- **초안이 진행 중인 동안 그 원천 문서(`sourceDocumentIds`)를 직접 편집할 수 없다.** 위 「문서」 절의 편집 차단과 같은 규칙이다.

### 인증 · 회원가입
- 소셜 로그인은 **MVP1은 구글만** 지원한다. 카카오/네이버는 MVP2에서 확장한다.
- 별도의 회원가입 절차는 없다 — 최초 Google 로그인 시 회원이 자동 생성된다.
- **한 회원은 하나의 소셜 계정으로만 가입**한다. 이미 가입된 이메일로 다른 소셜을 통해 가입을 시도하면 거부한다.
  - 동일인 판별은 OAuth로 받아오는 개인정보(이메일 등) 범위에 따라 가능 여부가 갈린다.
- **회원 탈퇴 시 소셜 연동 해제(Unlink)는 로컬 상태 변경(soft delete)만 한다.** Google API를 호출해
  토큰을 취소(revoke)하지는 않는다(9/10 확정). Google 토큰(refresh token)을 장기 보관해야만 나중에
  revoke를 호출할 수 있는데, 이 프로젝트는 Google 토큰을 저장하지 않는 stateless 인증 구조라 맞지
  않는다. 필요하면 회원이 Google 계정의 '타사 앱 연결'에서 직접 해제해야 한다.

> **모델 반영 필요(미확정)** — 위 정책 중 아직 엔티티 표에 없는 항목:
> - **알림 설정 모델** — 유형별 수신 여부·채널을 어떤 단위(워크스페이스 / 참여자)로 둘지. notification 도메인 작업에서 확정한다. **6개 도메인 범위 밖이다** — 각 도메인은 이벤트만 발행하고 소비자가 없어도 된다
> - `Workspace`의 **설명·태그·공개여부** — `REQ-WS-001`·`REQ-WS-006`이 「모델 미정의」로 미뤘다. 필요해질 때 여기에 먼저 추가한다
>
> **해소된 항목**
> - ~~**참여자 권한 변경 주체**~~ — **해소(9/10).** **Owner 전용**이다. 「Admin은 Regular만 내보낼 수 있고 Admin을 내보내는 것은 Owner만」이라는 «삭제» 규칙과 같은 서열 원칙을 권한 변경에 적용한 결과다 — 같은 서열끼리 서로를 조작하지 못한다
> - ~~**참여자 삭제 방식**~~ — **해소(9/10).** **소프트 삭제**다. `Participant`가 `deletedAt`을 갖고 모든 조회에서 `deleted_at is null`을 건다. `leftAt`을 따로 두지 않는다
>   - 워크스페이스 삭제도 소프트 삭제다
>   - **재초대는 소프트 삭제된 행을 되살린다** — `(workspace_id, member_id)`가 유니크라 새 행을 만들 수 없다
> - ~~`Document`의 **라벨** 속성 및 Label 엔티티, 문서당 라벨 개수 상한~~ — **해소(9/10).** `Label` + `DocumentLabel`, 문서당 5개, 이름 1~20자
> - ~~`Document`의 **outdated 판별 속성**~~ — **해소(9/10).** 속성을 두지 않는다. **`outdated` 자체를 폐기하고 `aligned`로 바꿨다**(2026-09-10) — 최신 `DocumentVersion`의 `dictionaryVersionNo`와 `edited`로 판정한다. 초안·리뷰 도메인은 `DocumentQueryPort.isExtractable`로 이 판정을 소비한다
> - ~~**룰셋 이력·스냅샷**~~ — **해소(9/10).** 정족수는 스냅샷을 두지 않고 **발행 시점의 룰셋 값을 실시간 조회**한다(GitHub가 브랜치 보호 규칙을 실시간 참조하는 것과 같다). 값 객체를 엔티티로 승격하지 않는다

> **속성 표의 식별자 타입은 개념 표기다.** `WorkspaceId`·`DocumentId`·`TermId` 같은 표기는 「그 엔티티의 식별자」를 뜻하며, **구현은 `Long`으로 통일한다**(2026-09-10 확정). 값 객체로 감싸지 않는다 — 타입 혼동을 컴파일 타임에 막는 이득보다 6개 도메인 시그니처와 JPA 매핑을 모두 바꾸는 비용이 크다.

---

# C. 관계 표 (그래프의 화살표)

| 출발 | 행위 | 도착 | 설명 |
| --- | --- | --- | --- |
| Workspace.Participant | 참조 | Member | 참여자가 회원을 가리킴 |
| Workspace | 포함 | RuleSet | 값 객체. 리뷰어 수 제한 |
| Workspace | 발급 | Invitation | 초대 링크(토큰) |
| Invitation | 수락 | Workspace.Participant | 수락하면 참여자로 등록 |
| Dictionary | 소속 | Workspace | 활성중 1개 + 보관중 N개 |
| Dictionary | 포함 | DictionaryVersion | 값 객체. 버전 번호·확정일시 |
| Dictionary | 포함 | Term | 버전마다 용어가 함께 복제된다 |
| Document | 소속 | Workspace |  |
| Document | 포함 | DocumentVersion | 확정된 본문 스냅샷. v1은 업로드본이고 이후는 **직접 편집과 반영**이 만든다 |
| Document | 직접 편집 | DocumentVersion | 리뷰를 거치지 않고 즉시 새 버전을 발행한다(2026-09-10 추가). 그 버전은 `edited = true`다 |
| DocumentVersion | 포함 | PublishedVersion | 값 객체. 버전 번호와 확정일시 |
| Workspace | 소유 | Label | 라벨 이름은 워크스페이스 안에서 유일 |
| Document | 참조 | Label | `DocumentLabel`을 경유. 문서당 최대 5개 |
| Document (+Dictionary) | DictionaryContrast | DraftDocument | 비표준 표현 찾아 제안어 생성. **진입점은 「최신 사전집으로 갱신」 하나다**(2026-09-10 정정). 사전집이 없으면 실행할 수 없다 |
| Document | ExtractTerm | DraftDictionary | 미등재 용어를 후보어로 수집하고 **이전 사전집과 통합**한다(2026-09-10 정정). **대상은 `aligned`인 문서만**이며, 사전집이 없는 첫 회차에는 기준 버전이 없는 문서도 포함된다 |
| DraftDocument | 소유 | SuggestionTerm | 자식 FK 단방향. 컬렉션 매핑 없음 |
| DraftDictionary | 소유 | CandidateTerm | 자식 FK 단방향. 컬렉션 매핑 없음 |
| ReviewRequest | 소유 | RevisionDocument / RevisionDictionary | 리비전이 `reviewRequestId`를 가짐. 회차마다 1건 |
| ReviewRequest | 소유 | Reviewer / Review / Reexamine / Revise | 모두 자식 FK 단방향 |
| Review | 소유 | Comment | 자식 FK 단방향 |
| DraftDocument | RequestReview(DOCUMENT) | ReviewRequest |  |
| DraftDictionary | RequestReview(DICTIONARY) | ReviewRequest |  |
| RevisionDocument | Reexamine | RevisionDocument | 자기 반복 |
| RevisionDictionary | Reexamine | RevisionDictionary | 자기 반복 |
| ReviewRequest | Revise | DocumentVersion | 새 문서 버전 생성 |
| ReviewRequest | Revise | Dictionary | 새 사전집 버전 생성. 기존 활성 사전집은 보관중으로 내려간다 |
| 각 단계 | 이벤트 발행 | Notification | 그래프에는 선이 없지만 필요 |
