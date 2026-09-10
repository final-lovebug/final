# 도메인별 유비쿼터스 언어 사전

## 1. Member (회원)

| 메인 단어 | 영문 | 정의 | 대체 후보 |
| --- | --- | --- | --- |
| 회원 | Member | 서비스에 가입해 인증받는 주체 | 사용자(Regular), 계정(Account) |
| 인증 | Authentication | 회원임을 확인하는 행위 | 로그인, 로그인 세션 |

관계: Workspace의 Participants가 Member를 참조한다.

## 2. Workspace (워크스페이스)

| 메인 단어 | 영문 | 정의 | 대체 후보 |
| --- | --- | --- | --- |
| 워크스페이스 | Workspace | 사전집과 문서를 공유하는 협업 단위 | 팀(Team), 프로젝트(Project), 스페이스 |
| 참여자 | Participant | 워크스페이스에 소속되어 역할을 가진 회원(Owner, Admin, Regular) | 구성원, 멤버십(Membership) |
| 룰셋 | RuleSet | 리뷰·대조 시 적용되는 검토 규칙의 묶음 | 규칙집, 검토 기준, 정책(Policy) |
| 설정 | Settings | 워크스페이스 단위의 운영 옵션 | 환경설정, 옵션, 정책 |

관계: Dictionary·Document를 소유한다. 참여자를 통해 Member와 연결된다.

## 3. Dictionary (사전집)

| 메인 단어 | 영문 | 정의 | 대체 후보 |
| --- | --- | --- | --- |
| 사전집 | Dictionary | 워크스페이스에서 합의된 표준 용어의 집합. 대조와 리뷰의 판단 기준 | 용어집, 표준어 사전 |
| 유비쿼터스 언어 | Term | 사전집을 구성하는 개별 표준 용어 | 표준 용어, 어휘 |
| 사전집 버전 | DictionaryVersion | 사전집을 특정 시점으로 확정한 스냅샷 | 개정, 릴리스, 스냅샷 |

관계: Workspace에 소속된다. DraftDictionary가 승인되면 여기에 반영된다(Revise).

## 4. Document (문서)

| 메인 단어 | 영문 | 정의 | 대체 후보 |
| --- | --- | --- | --- |
| 문서 | Document | 워크스페이스에 속한 작성·리뷰 대상 본문 | 노드(Node), 페이지(Page), 산출물 |
| 문서 버전 | DocumentVersion | 문서가 확정된 시점의 불변 스냅샷. **본문의 유일한 저장 위치** | 개정판, 릴리스 |
| 본문 | Content | 문서의 실제 텍스트. 확정 버전에만 있고, 현재 본문은 `currentVersionNo`가 가리키는 버전이다 | 내용, 콘텐츠 |
| 미갱신 | outdated | 최신 사전집과 대조·반영되지 않은 문서 상태. 저장하지 않고 **최신 확정 버전의 기준 사전집 버전**과 활성 사전집 버전의 비교로 판정한다 | 낡음, 비동기화, stale |
| 라벨 | Label | 문서를 분류하는 워크스페이스 소유의 이름표. 문서 목록을 걸러내는 기준 | 태그(Tag), 분류, 카테고리 |
| 위치 | TextRange | 본문 안의 시작·끝 구간 | 앵커(Anchor), 범위 |

관계: 대조(DictionaryContrast)의 입력이자 용어 추출(ExtractTerm)의 원천. 리비전이 반영되면 새 버전이 생긴다.

## 5. DraftDocument (문서 초안)

| 메인 단어 | 영문 | 정의 | 대체 후보 |
| --- | --- | --- | --- |
| 문서 초안 | DraftDocument | 사전집 대조 결과로 생성된, 교정이 필요한 문서 | 임시 문서(TemporaryDocument), 제안본, 후보 문서 |
| 사전집 대조 | DictionaryContrast | 문서 본문을 사전집과 비교해 비표준 표현을 찾는 행위. **진입점이 둘이다** — 「최신 사전집으로 갱신」은 최신 확정 본문을, 「문서 편집 저장」은 사용자가 고친 본문을 넣는다 | 대조(Collation), 비교(Comparison), 검출(Detection) |
| 제안어 | SuggestionTerm | 문서의 특정 표현을 대신할 표준 용어 후보. 대조가 만든다 | 대체어(Replacement), 교체 후보, 추천어 |
| 교정 | Examine | 팀원이 문서 초안의 제안어를 확인해 수용·거절하는 작업 | 검토(Examination), 수정(Correction) |

관계: Document + Dictionary → 대조 → DraftDocument → RequestReview(type=DOCUMENT).

## 6. DraftDictionary (사전 초안)

| 메인 단어 | 영문 | 정의 | 대체 후보 |
| --- | --- | --- | --- |
| 사전 초안 | DraftDictionary | 문서에서 추출된 용어 후보들을 모아둔, 확정 전 사전집 | 임시 사전, 후보 사전, 제안 사전 |
| 용어 추출 | ExtractTerm | 문서 본문에서 사전집에 없는 용어 후보를 찾아내는 행위 | 추출(Extraction), 발굴, 수집 |
| 후보어 | CandidateTerm | 표준 용어로 등재될지 아직 결정되지 않은 용어 | 제안 용어, 신규 용어, 미확정어 |

관계: Document → 용어 추출 → DraftDictionary → RequestReview(type=DICTIONARY) → 승인 시 Dictionary에 반영.

## 7. ReviewRequest (리뷰 요청)

| 메인 단어 | 영문 | 정의 | 대체 후보 |
| --- | --- | --- | --- |
| 리뷰 요청 | ReviewRequest | 초안(문서·사전)에 대해 리뷰어의 검토를 요청한 건 | 검토 요청, 승인 요청, 제출(Submission) |
| 요청 유형 | RequestType | 리뷰 대상 구분. DOCUMENT / DICTIONARY | 대상 종류, 타깃 타입 |
| 문서 개정안 | RevisionDocument | 리뷰 요청에 실린 문서 변경안 | 개정안, 수정본, 변경요청 |
| 사전 개정안 | RevisionDictionary | 리뷰 요청에 실린 사전집 변경안 | 사전 개정안, 등재안 |
| 리뷰 | Review | 리뷰어가 개정안을 검토한 행위와 그 결과 | 검토, 심사, 판정 |
| 리뷰어 | Reviewer | 개정안을 검토할 책임을 부여받은 참여자 | 검토자, 승인자(Approver) |
| 코멘트 | Comment | 개정안의 특정 위치에 남기는 사람의 의견 | 의견, 댓글, 검토 의견 |
| 재교정 | Reexamine | 리뷰 결과를 반영해 개정안을 다시 고치는 작업 | 재검토(Re-examination), 재작업, 보완 |
| 반영 | Revise | 승인된 개정안을 원본(문서 버전·사전집)에 적용하는 행위 | 병합(Merge), 확정, 적용, 발행 |

관계: DraftDocument·DraftDictionary에서 시작되고, 승인 후 Revise로 Document/Dictionary에 반영된다.

## 8. Notification (알림) — 미정

그래프에 상자만 있고 내용이 비어 있어서, 흐름상 필요한 것만 제안해봤어.

| 메인 단어 | 영문 | 정의 | 대체 후보 |
| --- | --- | --- | --- |
| 알림 | Notification | 회원에게 전달되는 도메인 사건 통지 | 통지, 메시지, 피드 |
| 알림 유형 | NotificationType | 어떤 사건인지 구분 (리뷰요청 도착, 코멘트 등록, 승인, 반려, 대조 완료) | 종류, 카테고리 |
| 수신자 | Recipient | 알림을 받는 회원 | 대상자, 받는 이 |
| 알림 채널 | Channel | 전달 경로. 인앱 / 이메일 / 슬랙 | 전달 수단, 경로 |
| 읽음 | Read | 수신자가 알림을 확인한 상태 | 확인, 열람 |