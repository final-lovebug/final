# T-INT-7 — SQS 작업 큐 계약 문서화

상태: 대기 | 담당자: (미정)
근거: `docs/plan/INTEGRATION_PLAN.md` 1절(SQS 작업 큐 계약) 전체
의존: 없음(T-INT-8의 선행)

이 태스크는 **코드보다 문서가 먼저**다(루트 `CLAUDE.md` 원칙). `INTEGRATION_PLAN.md`
1절에 이미 확정된 계약을 `CONFLICTS.md`에 결정 ID로 등재하고, `application.yml`에
주석 처리돼 있던 큐 키를 활성화하는 것까지가 범위다. 어댑터 구현(T-INT-8)은 여기 포함하지
않는다.

## 체크리스트

- [ ] `docs/plan/INTEGRATION_PLAN.md` 1-1·1-2·1-3절 내용을 `docs/plan/CONFLICTS.md`
      3절(또는 적절한 절)에 **`D-62`**로 등재 — 봉투 구조, payload/result가
      `ubidict-py/app/schema.py`를 따른다는 것, 변환 매핑표, `HOMOGRAPH` 한계를 그대로
      인용
- [ ] `docs/plan/EXECUTION_ORDER.md`에 `T-INT-7`·`T-INT-8`·`T-INT-17`을 태스크 표에 추가
- [ ] `backend/src/main/resources/application.yml`의 주석 처리된 큐 키 활성화:
      ```yaml
      app:
        messaging:
          sqs:
            queue: ${MESSAGING_SQS_QUEUE:lovebug-domain-event}
            llm-request-queue: ${MESSAGING_SQS_LLM_REQUEST_QUEUE:lovebug-llm-request}
            llm-reply-queue: ${MESSAGING_SQS_LLM_REPLY_QUEUE:lovebug-llm-reply}
      ```
- [ ] `application-prod.yml`에도 동일하게 반영(주석에 같은 내용이 있었는지 확인)
- [ ] `src/test/resources/application-test.yml`에도 필요하면 반영(테스트가 이 큐를
      실제로 쓰지 않으면 생략 가능 — T-INT-8 진행 시 재확인)
- [ ] `ubidicExtractor/ubidict-py`의 `task.md`에 "백엔드 실 스키마 도착, 재검토 완료"
      기록(다른 레포이므로 별도 커밋)
- [ ] `ubidict-py/app/queue_schema.py` 상단의 "초안" 주석 — 실제 구조가 확정됐음을
      반영해 정리(ubidict-py 쪽 작업, 선택)
- [ ] 커밋 브랜치 `chore/WLSH-{티켓}-t-int-7`(백엔드), PR 생성
