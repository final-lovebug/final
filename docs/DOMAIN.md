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

> 룰셋 변경 이력이나 리뷰 요청 시점의 스냅샷이 필요해지면 값 객체를 엔티티로 승격한다. 아래 **미확정** 항목이다.

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

> **이름(`name`)은 두지 않는다.** 워크스페이스에 활성 사전집이 정확히 1개이므로 워크스페이스와 구분해 부를 이름이 필요 없다. 사전집을 여러 갈래로 풀게 되면 그때 다시 추가한다.

#### DictionaryVersion (사전집 버전) — 값 객체

식별자도 생명주기도 따로 두지 않는다. 사전집과 함께 생기고 함께 사라지며 항상 정확히 1개다. 별도 테이블 없이 `dictionary` 테이블의 컬럼으로 둔다. `RuleSet`이 `Workspace`에 포함된 것과 같은 모양이다.

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 버전 번호 | versionNo | Int | O | 1부터. 워크스페이스 안에서 유일 |
| 확정일시 | publishedAt | DateTime | O | 행이 만들어지는 순간이 확정 순간 |

> **유래 리비전(`originRevisionId`)은 지금 두지 않는다.** 생성 주기상 모든 버전이 `RevisionDictionary`에서 나오므로 이 값은 실제로 의미를 갖지만, 리비전을 만들 수단이 아직 없어 항상 null이 된다. **리뷰 도메인 작업에서 되살릴 후보다.**

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
| 유래 리비전 | originRevisionId | RevisionDictionaryId | X | 리뷰 도메인 작업에서 되살릴 후보 |

---

## Document

### Document

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | DocumentId | O |  |
| 워크스페이스 | workspaceId | WorkspaceId | O |  |
| 제목 | title | String | O |  |
| 본문 | content | Text | O | 작업 중 최신 본문 |
| 현재 버전 | currentVersionNo | Int | O |  |
| 작성자 | ownerId | MemberId | O |  |
| 최종수정자 | updaterId | MemberId | O |  |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O | `ownerId`와 중복 소지 |
| 수정일시 | updatedAt | DateTime | O |  |

### DocumentVersion

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | DocumentVersionId | O |  |
| 문서 | documentId | DocumentId | O |  |
| 버전 번호 | versionNo | Int | O |  |
| 본문 스냅샷 | body | Text | O | 불변 |
| 확정일시 | publishedAt | DateTime | O |  |
| 유래 리비전 | originRevisionId | RevisionDocumentId | X |  |
| 기준 사전집 버전 | dictionaryVersionNo | Int | X | 발행 시점 기준 |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O |  |
| 수정일시 | updatedAt | DateTime | O | 불변이라 생성 이후 변하지 않음 |

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
| 초안 본문 | draftBody | Text | O | 교정 반영이 누적되는 본문 |
| ~~제안어~~ | ~~suggestions~~ | ~~List<SuggestionTerm>~~ | ~~X~~ |  |
| 상태 | status | Enum | O | 교정 중 / 교정완료 |
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
| 거절 사유 | rejectReason | Text | X | 인용·고유명사 등 |
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
| 대상 사전집 | dictionaryId | DictionaryId | O | 어디에 등재될지 |
| 유래 문서 | sourceDocumentIds | DocumentId | O | 여러 문서에서 모을 수 있음 |
| 후보어 | candidates | List<CandidateTerm> | X |  |
| 상태 | status | Enum | O | 교정중 / 리뷰요청됨 |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O |  |
| 수정일시 | updatedAt | DateTime | O |  |

### CandidateTerm (후보어)

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | CandidateTermId | O |  |
| 초안 사전 | draftDictionaryId | DraftDictionaryId | O |  |
| 표기 | form | String | O | 문서에서 추출된 표현 |
| 제안 정의 | proposedDefinition | Text | X | AI 초안 또는 사람 작성 |
| 제안 영문명 | proposedEnglishName | String | X |  |
| 출현 문서 | occurredDocumentIds | List<DocumentId> | O | 어디서 몇 번 나왔는지의 근거 |
| 출현 횟수 | occurrenceCount | Int | O | 등재 우선순위 판단 |
| 문맥 조각 | contextSnippets | List<Text> | X | 검토 시 판단 근거 |
| 처리 상태 | status | Enum | O | 대기 / 등재승인 / 동의어편입 / 거절 / 보류 |
| 처리 결과 용어 | resultTermId | TermId | X | 승인 후 생성된 표준 용어 |
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
| 대상 리비전 | revisionId | UUID | O | 유형에 따라 문서·사전 리비전 |
| 제목 | title | String | O |  |
| 요청자 | requesterId | MemberId | O |  |
| 리뷰어 | reviewers | List<Reviewer> | O |  |
| 상태 | status | Enum | O | 리뷰대기 / 리뷰중 / 변경요청 / 승인 / 반영완료 / 반려 / 취소 |
| 최종 승인일시 | approvedAt | DateTime | X |  |
| 반영일시 | revisedAt | DateTime | X |  |
| 내용 | description | string | X |  |
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
| 유래 초안 사전 | draftDictionaryId | DraftDictionaryId | O |  |
| 등재 제안 | proposedTerms | List<CandidateTerm> | O |  |
| 재교정 회차 | reexamineRound | Int | O |  |
| 결과 사전집 버전 | resultVersionNo | Int | X |  |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O |  |
| 수정일시 | updatedAt | DateTime | O |  |

### RevisedTerm (제안어)

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | RevisedTermId | O |  |
| 초안 문서 | draftDocumentId | DraftDocumentId | O |  |
| 위치 | anchor | TextRange | O | 본문 내 시작·끝 |
| 기존 용어 | originTerm | String | O | 문서에 실제로 쓰인 비표준 표현 |
| 제안 용어 | suggestionTerm | String | O | 사전집 참조. 표기 복사 금지 |
| 상태 | status | Enum | O | 처리 전 / 기존 용어 / 제안 용어 |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O |  |
| 수정일시 | updatedAt | DateTime | O |  |

### Reviewer

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | ReviewerId | O |  |
| 리뷰 요청 | reviewRequestId | ReviewRequestId | O |  |
| 회원 | memberId | MemberId | O | 워크스페이스 참여자여야 함 |
| 필수 여부 | required | Boolean | O |  |
| 지정일시 | assignedAt | DateTime | O |  |
| 생성일시 | createdAt | DateTime | O | `assignedAt`과 중복 소지 |
| 생성자 | createdBy | MemberId | O | 리뷰어를 지정한 사람 |
| 수정일시 | updatedAt | DateTime | O |  |

### Review

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | ReviewId | O |  |
| 리뷰 요청 | reviewRequestId | ReviewRequestId | O |  |
| 리뷰어 | reviewerId | ReviewerId | O |  |
| 대상 회차 | targetRound | Int | O | 어느 재교정 회차를 봤는지 |
| 판정 | verdict | Enum | O | 승인 / 변경요청 / 반대 |
| 코멘트 | comments | List<Comment> | X |  |
| 제출일시 | submittedAt | DateTime | O |  |
| 생성일시 | createdAt | DateTime | O | 초안 저장을 허용하면 `submittedAt`과 달라짐 |
| 생성자 | createdBy | MemberId | O |  |
| 수정일시 | updatedAt | DateTime | O |  |

### Comment

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | CommentId | O |  |
| 리뷰 | reviewId | ReviewId | O |  |
| 작성자 | authorId | MemberId | O | 사람만 |
| 내용 | content | Text | O |  |
| 위치 | anchor | TextRange | X | 전체 대상이면 null |
| 대상 항목 | targetItemId | UUID | X | 사전 리비전에서는 후보어 단위 |
| 상위 코멘트 | parentId | CommentId | X | 답글 |
| 해결 여부 | resolved | Boolean | O |  |
| 생성일시 | createdAt | DateTime | O |  |
| 생성자 | createdBy | MemberId | O | `authorId`와 중복 소지 |
| 수정일시 | updatedAt | DateTime | O |  |

### Reexamine (재교정)

| 속성 | 영문 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| 식별자 | id | ReexamineId | O |  |
| 리비전 | revisionId | UUID | O |  |
| 회차 | round | Int | O | 1부터 증가 |
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
| 대상 유형 | targetType | Enum | O | DOCUMENT / DICTIONARY |
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
| 유형 | type | Enum | O | 리뷰요청도착 / 코멘트등록 / 승인 / 반려 / 대조완료 / 추출완료 |
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
- **0이면 리뷰어 없이 바로 반영**할 수 있다.
- 참여자가 빠져 현재 인원이 설정값보다 작아지면, 그 워크스페이스에서는 **참여자 수를 상한으로 간주**한다.
- 리뷰 요청에 지정한 리뷰어 수가 설정값보다 적으면 요청을 만들 수 없다.

### 사전집

**생성 주기** — 사전집은 사람이 빈 껍데기를 만드는 것이 아니라, 문서에서 용어를 추출해 리뷰를 통과시킨 결과로 태어난다.

```
워크스페이스 생성            사전집 없음
  ↓  문서 2개 이상 업로드
용어 추출 (Admin 이상)       ① 문서별 용어 추출
                            ② 유사 의미 용어를 묶어 후보어 생성
                               (a-회원 · b-User · c-사용자 → 후보어 1개)
  ↓
DraftDictionary             ③ 요청자가 후보어마다 대표어를 고른다 → 초안 UPDATE
  ↓  리뷰 요청
RevisionDictionary          ④⑤ 리뷰 진행. 변경요청이면 CandidateTerm의 대표어를 고쳐 UPDATE
  ↓  승인
Dictionary (새 버전)        ⑥ 리비전이 그대로 반영되어 새 버전이 된다 ← 사전집 최종
```

- 워크스페이스를 만든 직후에는 **사전집이 없다.** 첫 사전집은 위 흐름을 거쳐 v1로 태어난다.
- 워크스페이스에 사전집 행은 여러 개지만 **활성중인 것은 정확히 1개**다. 그 행이 **가장 최근 확정본이자 문서 대조의 기준**이다.
- **새 버전은 리뷰 승인(Revise)의 반영으로만 생긴다.** 반영하면 기존 활성 사전집이 보관중으로 내려가고, 새 행이 다음 버전 번호로 활성중이 된다.
- **모든 사전집 행과 그 용어는 확정된 뒤 바뀌지 않는다.** 상태 전환만 예외다. 따라서 개별 용어를 추가·수정·삭제하는 경로가 없고, 용어를 바꾸려면 새 버전을 만들어야 한다.
- **사전집은 삭제하지 않는다.** 모든 행이 보존해야 할 버전 이력이다.
- **표준어는 사전집 내 유일**하다. 앞뒤 공백을 제거한 뒤 대소문자를 구분해 비교한다.
- **용어가 0개인 버전은 만들 수 없다.** 빈 사전집은 대조에 쓸 수 없다.
- 생성·반영은 **Admin 이상**, 조회는 참여자면 누구나 가능하다.
- **대조는 사전집에 저장된 동의어 목록을 훑는 방식이 아니다.** 사전집에는 표준어와 정의만 있고, LLM이 문서의 맥락을 파악해 그것과 비교·대조하여 바꿀 것을 제안한다.

> **동시 반영을 따로 막지 않는 이유** — 아래 「초안 사전」 정책이 사전집당 진행 중인 등재 흐름을 1개로 제한하므로, 같은 버전을 기준으로 편집하는 주체가 둘이 될 수 없다. 기준 버전은 `DraftDictionary.dictionaryId`와 `RevisionDictionary.baseVersionNo`가 이미 들고 있고, 검사가 필요해지면 그 자리는 **리뷰 승인 시점**이다.

### 문서
- 업로드 가능한 파일 형식은 **`txt`, `md`** 뿐이다.
- **업로드 시 자동으로 사전집 대조나 용어 추출을 실행하지 않는다.** 사전집이 있는 상태에서 올라온 문서는 `outdated`이며, 사용자가 **"최신 사전집으로 갱신"**을 실행할 때 활성 사전집과 대조된다.
- 문서 본문(`Document.content`)은 **10,000자 이내**다. (AI 토큰 사용량에 비례하므로 가볍게 유지)
- 문서에 **라벨**을 붙일 수 있고, 문서 목록은 라벨로 **필터링**할 수 있다.
- **outdated 문서는 용어 추출 대상이 아니다.** 최신 사전집 버전을 기준으로 대조되지 않은 문서에서는 `ExtractTerm`을 실행할 수 없다.

### 초안 (DraftDocument)
- **한 문서당 초안은 1개만** 존재할 수 있다. 기존 초안이 있으면 새 초안을 생성할 수 없다.
- 해당 문서가 **리뷰 요청 중이면 초안을 생성할 수 없다.** (`ReviewRequest.status`가 리뷰대기 / 리뷰중 / 변경요청인 동안)

### 초안 사전 (DraftDictionary)
- 사전집에 **초안(DraftDictionary) 또는 개정안(RevisionDictionary)이 존재하면 추가 초안을 생성할 수 없다.** 사전집당 진행 중인 등재 흐름은 **1개**다.

### 인증 · 회원가입
- 소셜 로그인은 **MVP1은 구글만** 지원한다. 카카오/네이버는 MVP2에서 확장한다.
- 별도의 회원가입 절차는 없다 — 최초 Google 로그인 시 회원이 자동 생성된다.
- **한 회원은 하나의 소셜 계정으로만 가입**한다. 이미 가입된 이메일로 다른 소셜을 통해 가입을 시도하면 거부한다.
  - 동일인 판별은 OAuth로 받아오는 개인정보(이메일 등) 범위에 따라 가능 여부가 갈린다.

> **모델 반영 필요(미확정)** — 위 정책 중 아직 엔티티 표에 없는 항목:
> - **참여자 권한 변경 주체** — Owner 전용으로 둘지, Admin에게도 Regular 승격·강등을 허용할지
> - **참여자 삭제 방식** — 내보낸 참여자를 행 삭제로 지울지, 이력으로 남길지(`Participant.leftAt` 등). 참여자 관리 작업에서 확정한다
>   - 워크스페이스 삭제는 **소프트 삭제로 확정**했다. `deletedAt`을 남기고 모든 조회에서 `deleted_at is null`을 건다
> - **룰셋 이력·스냅샷** — 룰셋을 바꿨을 때 진행 중인 리뷰 요청에 소급 적용할지, 요청 생성 시점 값을 리비전에 남길지. 이력이 필요해지면 값 객체를 별도 엔티티로 승격한다
> - **알림 설정 모델** — 유형별 수신 여부·채널을 어떤 단위(워크스페이스 / 참여자)로 둘지. notification 도메인 작업에서 확정한다
> - `Document`의 **라벨** 속성 및 Label 엔티티, 문서당 라벨 개수 상한. document 도메인 작업에서 확정한다
> - `Document`의 **outdated 판별 속성**(상태 또는 기준 사전집 버전 비교 방식)
> - `Member`의 **소셜 제공자(provider)** 속성

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
| Document (+Dictionary) | DictionaryContrast | DraftDocument | 비표준 표현 찾아 제안어 생성 |
| Document | ExtractTerm | DraftDictionary | 미등재 용어를 후보어로 수집 |
| DraftDocument | RequestReview(DOCUMENT) | ReviewRequest |  |
| DraftDictionary | RequestReview(DICTIONARY) | ReviewRequest |  |
| RevisionDocument | Reexamine | RevisionDocument | 자기 반복 |
| RevisionDictionary | Reexamine | RevisionDictionary | 자기 반복 |
| ReviewRequest | Revise | DocumentVersion | 새 문서 버전 생성 |
| ReviewRequest | Revise | Dictionary | 새 사전집 버전 생성. 기존 활성 사전집은 보관중으로 내려간다 |
| 각 단계 | 이벤트 발행 | Notification | 그래프에는 선이 없지만 필요 |
