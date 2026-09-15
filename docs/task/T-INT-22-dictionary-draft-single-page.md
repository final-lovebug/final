# T-INT-22 — 사전집 초안 단일 페이지 전환(백엔드 + 프런트)

상태: **코드 작성 완료(컨테이너 테스트 미검증, 2026-09-15)** | 담당자: (세션 진행)
근거: `docs/plan/DRAFT_PLAN.md` + `ui/draft-plan-preview.html`(프리뷰)
의존: `T-INT-21`(디자인 정합)

후보어별 판정을 걷어내고, 초안 한 페이지에서 대표어·정의만 정리해 바로 리뷰를 요청한다.
개정안 화면은 GitHub PR을 모티브로 리뷰어 지정·판정 제출을 다시 짰다.

## 판정 제거가 연쇄로 흔든 것 (가장 중요한 부분)

판정 상태를 읽던 규칙을 그대로 두면 **새 후보어가 전부 `PENDING`에 머무르므로** 다음이 전부
깨진다. 셋을 함께 고쳐야 흐름이 성립한다.

| 자리 | 예전 규칙 | 지금 |
| --- | --- | --- |
| 교정 완료 조건 | 미판정(`PENDING`) 0건 | **모든 후보어가 대표어·정의를 가졌는가**(`D-87`) |
| 발행 목록(`readFinalTerms`) | `REGISTRATION_APPROVED`·`KEPT`만 | **초안에 남아 있는 전부**(`D-88`) |
| 개정안 표의 변경 유형 | `KEPT` 아니면 「정의 수정」 | `추가`(신규) / `승계`(이전 버전)(`D-89`) |

## 백엔드

- [x] `DraftDictionaryReviewReadinessValidator` — 준비 조건을 유효성으로 교체.
      `DRAFT_DICTIONARY_CANDIDATE_FORM_REQUIRED` 신설, 정의 누락 메시지 수정
- [x] `DraftDictionaryQueryAdapter.readFinalTerms` — 상태 필터 제거, 표기순 고정 조회 추가
      (`CandidateTermRepository.findAllByDraftDictionaryIdAndDeletedAtIsNullOrderByFormAsc`)
- [x] `CandidateTermResponse` — `createdBy` 제거(DB 컬럼은 감사용으로 유지, `D-86`)
- [x] 판정 5종 엔드포인트와 `CandidateTermStatus`에 **「사용 안 함」 주석** — 지우지 않는 이유
      (이미 판정이 기록된 초안 행이 DB에 있다)를 함께 적었다
- [x] **사전 초안 교정 진행률 제거**(2026-09-15 추가) —
      `GET /api/draft-dictionaries/{id}/examine-progress`와 `ExamineProgressResult`·
      `ExamineProgressResponse`·`DraftDictionaryService.readExamineProgress`, 테스트 2건,
      `docs/API.md` 항목을 지웠다. 판정이 사라진 뒤 `pending`이 늘 후보어 전체 수라 뜻을 잃었고
      소비자도 0곳이었다. 꼬리로 죽은 `CandidateTerm`의 판정 술어 8종
      (`isPending`·`isDecided`·`isRegistrationApproved`·`isMergedAsSynonym`·`isRejected`
      ·`isKept`·`isOnHold`·`isPublished`)도 함께 지웠다 — `status` 필드와 enum, 판정 변경자는
      남아 있다(호출부가 있고 과거 행을 읽어야 한다).
      **문서 초안 쪽 `examine-progress`는 그대로다** — 제안어 판정이 살아 있어 「문서 > 초안」
      목록의 미해결 건수가 그 값이다
- [x] ~~초안 응답에 `creatorName` 추가~~ — **하지 않았다.** `D-62`와 충돌해 프런트 해석으로
      결정(`D-86`, 사용자 확인). `DRAFT_PLAN.md`에도 사유를 남겼다
- [x] `docs/API.md` — 교정 완료 조건, 리뷰 요청 자격, 후보어 응답, 에러 코드 표, 판정 절의
      「사용 안 함」 경고

## 프런트

- [x] `fetchDictionaryDraft` 신설 — **초안 정보 + 작성자 이름 + 후보어 + 준비 여부를 한 번에**
      로드한다(플랜의 「함께 로드」). 준비 여부·정의 누락 목록을 여기서 계산해 화면이 버튼을
      막고 무엇이 막는지 알려 준다
- [x] `DictionaryDraftPage` 재작성 — 상단에 작성자 한 명, 후보어별 작성자·담당자·판정·체크박스·
      일괄 처리 전부 제거. 대표어 선택·직접 입력·정의·근거 문장은 유지
- [x] **재교정 문맥** — `?reviewRequest={id}`로 들어오면 리뷰어 코멘트를 배너와 상세 패널에
      함께 보여주고 주 버튼이 「재교정 완료」로 바뀐다(`POST .../reexaminations`)
- [x] `ReviewerPanel` 재작성 — GitHub 우측 사이드바 모티브. 리뷰어별 상태(`✓ 승인`/`↻ 변경
      요청`/`대기`)는 **`reviewers` + `reviews` 두 조회를 조인**해서 만든다(`ReviewerResponse`에
      판정이 없다). 지난 회차 판정은 「대기」로 되돌린다. ⚙·× 는 **요청자이며 ADMIN 이상**일 때만
- [x] `ReviewSubmitPopover` 신설 — GitHub 「Finish your review」. 전체 코멘트 + 판정 라디오 +
      제출 한 흐름. **「Comment」 판정은 없다**(백엔드 `ReviewVerdict`가 둘뿐)
- [x] `DictionaryRevisionPage` — 「재교정」을 초안으로 **이동**하게 바꾸고, `Change request`·
      `Approve` 버튼 둘을 팝오버 하나로 교체
- [x] `DictionaryPage` 승인 대기 배너 집계 수정 — `'정의 수정'`으로 세고 있어 `D-89` 이후
      **승계 용어가 통째로 빠지고 늘 「정의 수정 0」**이 찍혔다. 축을 `신규`·`승계`로 맞췄다
- [x] 제거: `fetchCandidates`·`decideCandidateTerm`·`bulkDecideCandidateTerms`와 훅 3개,
      후보어 `ownerName` 필드
- [x] `submitDictionaryRevision` — `examine-progress`의 `pending` 선검사 제거(그 값이 이제
      준비 여부를 뜻하지 않는다). 최종 판정은 백엔드에 맡기고 에러를 그대로 보여준다

## 검증

- [x] `./gradlew compileJava`·`compileTestJava` 통과
- [x] `DraftDictionaryReviewReadinessValidatorTest` **9건 통과**(새 규칙으로 재작성 —
      「판정하지 않아도 통과」·「판정 안 한 후보어도 등재 대상」을 못 박았다)
- [x] `DraftDictionaryQueryAdapterTest` 재작성(모든 후보어 포함 / 삭제분 제외)
- [x] 프런트 `tsc -b --force`·`npm run build`·`npm run lint` 통과(새 경고 0건)
- [x] **컨테이너를 쓰는 백엔드 테스트 검증 완료**(2026-09-15, 디스크 확보 후). `./gradlew check`
      **963건 전부 통과.** 처음 돌렸을 때 **3건이 깨져 있었다** — 이 태스크가 판정을 걷어낸
      뒤 컨테이너 테스트를 한 번도 돌리지 못해 드러나지 않았던 것이다.
      - `DraftDictionaryServiceTest`의 「미판정 후보어가 있으면 교정을 완료할 수 없다」 —
        `D-87`이 없앤 규칙을 검증하고 있었다. **새 규칙으로 바꿔 썼다**(판정 없이도 완료되고,
        대신 **정의가 비면** 막힌다). 조건을 무르게 한 것이 아니라 막는 지점을 옮긴 것이다
      - `UbiquitousLanguageLifecycleTest`의 첫 사전집 기대값 — `D-88`로 후보어가 전부
        발행되므로 추출 대역의 고정 후보어("결제"·"주문")까지 실린다. 기대값을 셋으로 고쳤다
      - `UbiquitousLanguageLifecycleTest`의 2회차 추출 실패 — **`D-88`이 만든 실제 결함**이다.
        승계 후보어와 재추출된 표기가 부딪혀 `DUPLICATE_CANDIDATE_FORM`으로 추출 작업 전체가
        실패했다. `D-91`로 고쳤다(승계된 표기는 건너뛴다)
- [ ] 수동 QA — 로컬 백엔드 기동 후 초안 → 리뷰 요청 → 개정안 → 재교정 → 재교정 완료 왕복

## 남겨 둔 것

- **문서 개정안 화면(`DocumentReviewThreadPage`)은 아직 `Change request`·`Approve` 버튼 둘이다.**
  이번 지시는 사전집 개정안 화면이었고, 문서 쪽까지 바꾸면 범위를 넘는다. `ReviewerPanel`은
  공용이라 GitHub 모양으로 함께 바뀌었으니, 판정 제출만 나중에 맞추면 된다
- `useAddReviewThreadComment`는 이 세션 이전부터 호출부가 없다(`D-63`의 pending 방식으로
  대체됨). 이번 범위가 아니라 그대로 뒀다
