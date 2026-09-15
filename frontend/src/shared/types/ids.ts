// docs/DOMAIN.md 엔티티들의 식별자 타입. 실제 값은 문자열(UUID 등)이고 지금은 단순 별칭이다.
// 여러 도메인이 서로의 식별자를 외래키처럼 참조해야 하는데, 그때마다 다른 도메인의
// features/{domain}/model을 직접 import하면 ARCHITECTURE.md의 "features 간 직접 참조 금지"
// 규칙과 부딪힌다. 그래서 식별자 타입만 shared로 모았다 — 이건 특정 도메인의 비즈니스 로직이
// 아니라 여러 도메인이 공유하는 순수 참조 타입이라 shared에 두는 게 맞다.
export type MemberId = string
export type WorkspaceId = string
export type ParticipantId = string
export type RuleSetId = string
export type SettingsId = string

export type DictionaryId = string
export type DictionaryVersionId = string
export type TermId = string

export type DocumentId = string
export type DocumentVersionId = string
export type DraftDocumentId = string
export type SuggestionTermId = string

export type DraftDictionaryId = string
export type CandidateTermId = string

export type ReviewRequestId = string
export type RevisionDocumentId = string
export type RevisionDictionaryId = string
export type ReviewerId = string
export type ReviewId = string
export type CommentId = string

export type NotificationId = string

// docs/DOMAIN.md 467~469줄 "모델 반영 필요(미확정)" — Label 엔티티가 아직 없다.
// 백엔드 모델이 정해지기 전까지 쓰는 프론트엔드 임시 식별자.
export type LabelId = string
