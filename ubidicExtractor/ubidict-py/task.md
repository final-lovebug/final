# ubidict-py 작업 추적

> **2026-09-15 — 이 파일이 배포 작업의 기준 문서다.** 독립 저장소
> `D:\...\ubidicExtractor`(git remote `final-lovebug/ubidicExtractor`)에도 한때 같은
> 내용이 기록됐지만, **실제 배포 소스는 이 `final` 모노레포의 `ubidicExtractor/ubidict-py`
> 다**(사용자 확인). 독립 저장소는 더 이상 참조하지 않는다.

## 배경

`test/`는 CLI로 검증한 R&D 프로토타입(SPEC.md §10 D-1~D-40). 여기(`ubidict-py/`)는
그 로직을 실제 서비스로 옮긴 것 — SQS로 요청을 받고 결과를 SQS로 돌려주며,
멱등성 확인용 MySQL을 하나 쓴다.

**`test/`의 기존 문서(AGENTS.md·SPEC.md §2/§11)와 다른 점**: 거기엔 "Kafka는
Spring이 담당, 이 서비스는 HTTP만"·"DB 붙이지 마라"고 돼 있는데, 이건 R&D
당시 판단이고 지금은 뒤집혔다 — 이 서비스가 직접 SQS를 구독하고 MySQL도
쓴다. `test/` 문서는 그대로 두고(과거 기록 보존) 새 아키텍처는 여기 적는다.

## 확정된 결정

| 항목 | 결정 |
|---|---|
| 메시지큐 | SQS |
| 결과 회신 | 응답용 큐(reply queue)로 발행 |
| DB | **2026-09-14부터 `final/backend`의 MySQL을 그대로 공유** — 멱등성 테이블(`processed_jobs`) 하나만 우리 것이고, `document`/`document_version`/`term`은 읽기 전용으로 직접 조회. 별도 DB 인스턴스 안 둠 |
| 문서·사전집 전달 방식 | **2026-09-14부터 ID 기반** — 요청은 `documentIds`/`dictionaryId`만 받고, 본문·용어는 `app/backend_db.py`가 DB에서 직접 읽는다(인라인 전송 안 함) |
| 인증 | JWT(`accessToken`)는 **검증하지 않는다** — 그대로 받아서 응답에 그대로 에코 |
| stub/real 모드 | 요청(`ExtractJobRequest`/`ContrastJobRequest.mode`)에 포함. `stub`(기본값)이면 DB·Gemini 둘 다 안 부르고 2.5초 뒤 빈 결과, `real`이면 실제 처리. 백엔드의 `app.ai.extractor.mode`/`checker.mode`와 같은 개념 |
| 배포 | **ECR + CodeDeploy, EC2/On-Premises(IN_PLACE) — 2026-09-15 확정.** ECS 아니다. 실제 대상은 이미 떠 있는 단일 인스턴스 `lovebug-fastapi`(t4g.micro, ARM64, Ubuntu 26.04). **배포 소스는 `final` 모노레포**(`.github/workflows/ubidict-py-deploy.yml`) — 위 안내 참고. 상세는 아래 "AWS 배포(EC2+CodeDeploy)" 절 참고 |
| dev/prod | **dev=로컬**(`.env`+`docker-compose.local.yml`, 기존 그대로) **/ prod=`lovebug-fastapi` EC2**(위와 동일 인스턴스, ALB `lovebug-alb-1930145637.ap-northeast-2.elb.amazonaws.com`로 HTTP 노출 + SQS는 원래대로). EC2를 dev/prod 겸용으로 나누지 않는다 — 인스턴스가 1대뿐이고 IAM 역할도 `/lovebug/llm/*` 경로에만 권한을 주는 것으로 prod 전용으로 프로비저닝돼 있다. **2026-09-15 재개 세션에서 사용자에게 다시 확인 — 기존 결정 유지로 재확정.** SSM 경로는 같은 날 `/prod/llm/*`에서 `/lovebug/llm/*`로 변경(아래 참고) |
| CI/CD | **GitHub Actions로 확정**(`final/.github/workflows/`, 저장소 루트 기준) |
| 모니터링 | CloudWatch(표준출력 구조화 JSON 로그 → awslogs 드라이버). OpenTelemetry는 미정 — 나중에 추가할 자리만 |

## 작업 단계

- [x] 1. 프로젝트 뼈대 — `pyproject.toml`·`.env.example`·`.gitignore` + `test/app/`에서 포팅
      (`schema.py`, `pipeline/{api_keys,model_chain,variant,synonym,llm,contrast_llm,usage_log,contrast_usage_log}.py`,
      `pipeline/normalize.py`는 fixture 로더 빼고 포팅, `prompts/*.md` 8개 복사)
- [x] 2. `app/service.py` — `run_extract`/`run_contrast` 단일 진입점(HTTP·큐 공용)
- [x] 3. `app/main.py` — `/health`·`/extract`·`/contrast`를 mock에서 `service.py` 호출로 교체
- [x] 4. `db/schema.sql` + `app/idempotency.py` — 멱등성 체크·기록
- [x] 5. `app/queue_schema.py`(초안) + `app/queue_consumer.py` — SQS 롱폴링, 멱등성 확인 → 처리 → 응답 큐 발행 → 기록 → 삭제
- [x] 6. `usage_log.py`/`contrast_usage_log.py`에 표준출력 구조화 JSON 로그 추가(로컬 JSONL은 유지)
- [x] 7. `reference/backend/README.md` 자리 마련 — 백엔드 코드 도착 시 `queue_schema.py` 재검토 필요
- [x] 8. `Dockerfile` + `deploy/{appspec.yaml,taskdef.json,buildspec.yml}` 작성 — **2026-09-15, ECS 전용이라 폐기하고 EC2용으로 전면 교체함. 아래 "AWS 배포(EC2+CodeDeploy)" 절 참고**
- [x] 9. 검증:
  - `docker build` 성공 확인
  - 로컬 `uvicorn`/컨테이너 기동 + `curl /health` 확인
  - **`test/fixtures`로 실제 `/extract`·`/contrast` 스모크 테스트 완료**: `/extract`는 실제 Gemini 호출로 15개 후보 반환(gemini-3.5-flash, D-40 자동 모델 체인 정상 작동), `/contrast`는 23개 제안 반환(`구독자→이용자` 등 test/의 D-23 골드셋과 일치하는 결과 확인) — 포팅이 로직을 안 깨뜨렸음을 확인
  - **실제 AWS SQS(`lovebug-llm-request`/`-reply`/`-dlq`, `ap-northeast-2`, 계정 416121583617)로 큐 E2E 완료(2026-09-13)**:
    `scripts/stub_backend.py`(가짜 백엔드)로 실제 요청 큐에 발행 → `ubidict-py` 컨슈머가 실제로 수신·파싱 → 실제 Gemini 호출(extract: gemini-3.5-flash, 5.2s / contrast: 2.7s) → 실제 응답 큐로 결과 발행 → stub이 수신 확인. 멱등성도 확인: 같은 jobId를 다시 발행하니 `이미 처리됨(중복 배달) — 건너뛰고 메시지만 삭제` 로그와 함께 Gemini 재호출 없이 스킵됨(MySQL `processed_jobs`에 기록 남음, 로컬 MySQL은 포트 충돌 회피로 3308 사용 — `docker-compose.local.yml`/`.env.example` 참고). `main.py`에 `logging.basicConfig`를 추가해 `queue_consumer.py`의 INFO 로그가 실제로 보이도록 고침.
    **단, 이건 진짜 백엔드가 아니라 스텁으로 한 것** — `final/backend`엔 SQS 코드·요청 엔드포인트가 전혀 없음을 확인함(D-24/D-34/D-35 계획만 있고 미착수, `REQ-EXT-*`/`REQ-CHK-*` 상태 "대기"). 아래 미결 사항 참고.
- [x] 10. **문서·사전집을 ID 기반으로 백엔드 DB에서 직접 읽는 것으로 전환(2026-09-14)**:
  - `final/backend`를 조사해 실제 스키마 확보(추측 아님, Flyway 마이그레이션 원문 확인) — `document`(메타)+`document_version.body`(본문, TEXT 최대 10,000자), `term(dictionary_id, preferred_form, english_name, definition)`(`synonyms` 컬럼 없음, test/SPEC.md D-22와 같은 결론). `department`는 대응 컬럼이 없어 빈 문자열로 채움
  - `app/backend_db.py` 신규 — `fetch_documents`/`fetch_existing_terms`/`fetch_dictionary_entries`(읽기 전용, 백엔드 소유 테이블에 안 씀)
  - `app/job_schema.py` 신규 — `ExtractJobRequest`/`ContrastJobRequest`(jobId/workspaceId/dictionaryVersionNo/dictionaryId/documentIds/accessToken/mode)
  - `app/service.py`에 `run_extract_job`/`run_contrast_job` 추가 — mode="stub"이면 DB·Gemini 다 스킵하고 2.5초 뒤 빈 결과, "real"이면 DB 조회 후 기존 `run_extract`/`run_contrast`에 위임(판정 로직 자체는 안 바뀜)
  - `app/queue_consumer.py`·`app/main.py`(`/jobs/extract`·`/jobs/contrast` 신설) 둘 다 이 함수를 쓰도록 교체. 결과에 `accessToken`을 그대로 에코(`QueueResultEnvelope`에 필드 추가)
  - **로컬 DB를 `final/backend`의 MySQL로 통합** — 우리만의 `ubidict-py-mysql-1`(docker-compose) 제거, `docker-compose.local.yml`은 이제 LocalStack(SQS)만 남김. `.env.example`도 이에 맞춰 갱신
  - 실측으로 잡은 버그: `backend_db.py`/`idempotency.py`의 pymysql 연결에 `charset="utf8mb4"`를 명시 안 하면 한글이 깨짐(mojibake) — 실제 데이터 넣고 조회해보다가 발견, 수정 완료
  - **검증**: 실제 `backend-mysql-1`에 테스트용 workspace/document/document_version/dictionary/term 행을 임시로 넣고(Python pymysql로 — 처음에 bash heredoc으로 넣었다가 클라이언트 인코딩 문제로 한 번 깨진 데이터가 들어가는 걸 발견해 지움) `/jobs/extract`(stub·real)·`/jobs/contrast`(real)를 HTTP로 직접 호출해 끝까지 확인: stub은 2.5초 뒤 빈 결과+accessToken 에코, real은 실제 DB에서 읽은 본문으로 실제 Gemini 호출(extract 5.5s, contrast 5.1s) 성공. `scripts/stub_backend.py --mode stub`으로 실제 SQS 큐를 통한 전체 흐름도 재확인(큐→컨슈머→DB→SQS 응답). 테스트 데이터는 전부 삭제해 원상복구함
  - 단위 테스트 추가: `tests/test_backend_db.py`(컬럼 매핑), `tests/test_service_job.py`(stub이 DB/Gemini 안 부르는지, real이 fetch 결과를 그대로 전달하는지), `tests/test_queue_consumer.py`를 새 job 스키마 기준으로 재작성 — 전부 통과(23개)
- [ ] 11. 미결 사항(아래 "다음에 할 것" 참고)

## 다음에 할 것 / 미결 사항

- [x] ~~`idempotency.py`·`queue_consumer.py` 단위 테스트(모킹) 작성~~ — 2026-09-14 완료. `tests/{test_idempotency,test_queue_consumer,test_queue_schema,test_main}.py` — boto3/pymysql/Gemini 전부 모킹, 15개 전부 통과(로컬 + GitHub Actions 둘 다 확인)
- [x] ~~실제 AWS 자격증명·SQS 큐가 생기면 `queue_consumer.py` 통합 테스트~~ — 2026-09-13 완료(위 참고). 큐 3개는 실제로 존재(표준 큐, DLQ `maxReceiveCount=3` 연결됨, `VisibilityTimeout=900s`) — `final/docs`가 서술한 "FIFO+MessageGroupId" 방향과 다르지만 표준 큐로도 지금 문제없이 동작함
- [x] ~~MySQL 실제 인스턴스 연결 확인~~ — 2026-09-14부터 우리만의 로컬 MySQL(포트 3308)은 제거하고 `final/backend`의 MySQL(`backend-mysql-1`)을 그대로 씀(위 10번 참고). **실제 운영 RDS는 별도**(이건 로컬 개발용일 뿐)
- [x] ~~CI 파이프라인 확정~~ — 2026-09-14, **GitHub Actions로 확정**(CodeBuild 아님). `.github/workflows/ubidict-py-ci.yml`(lint+test, PR/push마다) 실제로 push해서 GitHub에서 통과 확인(run 34800209082). `.github/workflows/ubidict-py-deploy.yml`(main push 시 ECR→ECS→CodeDeploy)도 같이 작성했지만 **아직 실제로 성공할 수 없음** — 참조하는 AWS 인프라(ECS 클러스터/서비스, CodeDeploy 애플리케이션/배포 그룹, ECR 리포지토리, IAM OIDC 역할, Secrets Manager)가 하나도 없음. `deploy/buildspec.yml`(CodeBuild 템플릿)은 제거함
  - **정정(2026-09-15) — 이 판단 자체가 틀렸다.** 배포 타깃이 ECS가 아니라 **이미 떠 있는 단일 EC2(`lovebug-fastapi`)**로 확정되면서, 위에서 "없다"고 적은 ECS 인프라는 애초에 필요 없어졌다. 대신 EC2/CodeDeploy 인프라를 실제로 조사해보니 그것대로 준비가 안 돼 있었다 — 아래 "AWS 배포(EC2+CodeDeploy)" 절이 이 항목을 대체한다.
- [ ] **백엔드가 실제로 이 기능(용어 추출·사전집 대조 요청)을 구현하면**: `app/queue_schema.py`의 봉투 구조가 실제 계약과 맞는지 재검토 — `final/backend`는 현재 `DI-5`(`POST /api/draft-dictionaries/extractions`)·`DD-5`(`POST /api/draft-documents/checks`)가 미착수라 아직 맞출 대상이 없음. 방향성은 알려짐: `PENDING/RUNNING/SUCCEEDED/FAILED` 작업 테이블 + 폴링, 이벤트는 "불변 record, 식별자/원시값/값객체/시각 타입만"(`final/docs/ARCHITECTURE.md`) — `queue_schema.py`의 초안 봉투가 이 규칙과 크게 어긋나진 않아 보이나 필드명은 재조정 필요할 가능성 높음
- [ ] 표준 큐 vs FIFO 큐 — `final/docs`는 FIFO+`MessageGroupId=workspaceId`를 설계 방향으로 서술하는데 실제 provisioned 큐는 표준 큐다. 어느 쪽이 맞을지 인프라/백엔드 팀 확인 필요(지금 코드는 표준 큐 기준이라 FIFO로 바뀌면 `queue_consumer.py`의 `send_message` 호출에 `MessageGroupId`/`MessageDeduplicationId` 추가 필요)
- [ ] 실패 시 응답 큐에 매번 FAILED를 발행하는 지금 방식이 백엔드 쪽에서 괜찮은지 확인(재시도마다 중복 발행 가능 — `queue_consumer.py` 주석 참고)
- [ ] OpenTelemetry 계측 여부/방식 결정되면 추가(지금은 의존성 자체를 안 넣음)
- [ ] **`accessToken`을 왜 돌려받는지 확인** — 지금은 검증 없이 그대로 에코만 한다(`reference/backend/README.md` 참고). 백엔드가 이걸로 뭘 하려는지 알면 처리 방식이 맞는지 재확인 가능
- [ ] `job.workspaceId`/`dictionaryVersionNo`가 실제로 어떻게 쓰이는지 — 지금은 `fetch_documents`/`fetch_existing_terms`가 `documentIds`/`dictionaryId`만으로 조회하고 `workspaceId`는 안 씀(응답 조립에만 사용). 백엔드가 워크스페이스 소속 검증까지 기대하는지 확인 필요
- [ ] 운영 DB 접속 계정 — 지금은 로컬처럼 백엔드와 같은 계정(`ubidict`)을 가정했다. 운영에서는 `app/backend_db.py`용으로 **읽기 전용 별도 계정**을 만드는 게 안전(쓰기 권한 자체가 없으면 실수로 백엔드 테이블에 쓰는 사고를 원천 차단)
- [ ] **(2026-09-15 새로 발견) `/api/internal/llm/**` 보안 노출** — `docs/AI_CONTRACT.md`는 이 경로가 "워커 출발지로만 제한돼야 하며, 안 하면 `NFR-AI-002` 미충족"이라 명시하는데, 실제로 확인해보니 ①콜백을 ALB 경유로 보내기로 해서 이 경로가 사실상 공개 인터넷에 노출돼 있고(인증 헤더 없음, `requestId` 일치만으로 방어) ②스프링 EC2 보안그룹(`sg-01580c5183a389d13`)의 8080 인바운드도 `0.0.0.0/0`으로 열려 있다. 사용자 확인: **지금은 후속 과제로 미룬다**(IAM/SSM 조치를 우선 마무리). 나중에 조치할 방법 후보: 스프링 SG의 8080 인바운드를 ALB SG·fastapi SG로만 제한 + `/api/internal/**` 경로에 대한 ALB 리스너 규칙 자체를 없애거나 내부 전용 리스너로 분리
- [x] ~~실제 ECS 클러스터·서비스·ALB·IAM 역할·SQS 큐·RDS(MySQL) 프로비저닝~~ — **해당 없음(2026-09-15)**. ECS를 아예 안 쓰기로 확정. ALB·SQS·RDS는 이미 다른 목적(스프링 백엔드)으로 떠 있던 걸 공유해서 쓰는 쪽으로 정리됨 — 아래 절 참고

## AWS 배포 (EC2 + CodeDeploy) — 2026-09-15

### 아키텍처 확정

ECS가 아니라 이미 떠 있는 단일 EC2 인스턴스 `lovebug-fastapi`(계정 416121583617, `ap-northeast-2`, `t4g.micro`/ARM64/Ubuntu 26.04, IAM 인스턴스 프로파일 `lovebug-ec2-fastapi`)에 ECR+CodeDeploy로 배포한다. **같은 계정에 이미 실제로 운영 중인 스프링 백엔드(같은 `final` 저장소, `.github/workflows/deploy.yml`+`deploy/`) 배포 패턴을 그대로 조회해서 이식했다** — 새로 설계하지 않음. **배포 워크플로 자체도 이 `final` 저장소 루트의 `.github/workflows/ubidict-py-deploy.yml`에 있다** — 스프링 배포와 완전히 같은 구조(같은 OIDC 역할, 같은 S3 버킷, 같은 CodeDeploy 앱)라 별도 IAM 신뢰정책이 필요 없다(아래 "중요한 이점" 참고):

```
push(main, ubidicExtractor/ubidict-py/**) → ubuntu-24.04-arm 러너에서 docker buildx(linux/arm64) 빌드
→ ECR push(lovebug/fastapi:$SHA, latest 태그는 안 씀 — 리포가 IMMUTABLE)
→ ubidicExtractor/ubidict-py/deploy/ 폴더를 zip → S3(lovebug/deploy/fastapi-$SHA.zip) 업로드
→ aws deploy create-deployment(앱 lovebug, 배포그룹 lovebug-fastapi-codedeploy)
→ aws deploy wait deployment-successful
```

CodeDeploy 훅(EC2 위에서 인스턴스 자신의 IAM 역할로 실행): `ApplicationStop`(기존 컨테이너 정지) → `AfterInstall`(ECR pull, `/lovebug/llm/*`·`/lovebug/rds/*` SSM 파라미터를 그 자리에서 읽어 `docker run -e`로 주입) → `ValidateService`(`curl localhost:8000/health` 재시도).

**dev/prod**: dev는 로컬(`.env`+`docker-compose.local.yml`, 변경 없음), prod가 이 EC2. 인스턴스가 1대뿐이고 IAM 역할도 `/lovebug/llm/*` 경로에만 권한을 주는 것으로 prod 전용으로 프로비저닝돼 있어서 dev/prod를 EC2 안에서 컨테이너로 나누지 않기로 함(사용자 확인, 2026-09-15 재개 세션에서 재확인).

**중요한 이점(2026-09-15, 배포 소스를 `final` 모노레포로 정정하며 발견)**: `final` 저장소 자체의 GitHub Actions는 이미 `arn:aws:iam::416121583617:role/lovebug-github-actions`의 OIDC 신뢰를 받고 있다(스프링 `deploy.yml`이 이미 이 역할로 잘 동작 중). 배포 워크플로가 이 저장소 안에 있으므로 **아래 표 1번("OIDC 신뢰정책에 리포 추가 필요") 블로커는 해당 없음** — 별도 독립 저장소에서 배포했다면 필요했을 조치다.

### 조사로 확인한 배포 불가 사유와 조치 (전부 실제 `aws`/`gh` CLI로 조회함, 추측 아님)

| # | 문제 | 조치 | 상태 |
|---|---|---|---|
| 1 | ~~`lovebug-github-actions` IAM 역할의 OIDC 신뢰정책이 `repo:final-lovebug/final`만 허용 — 독립 저장소는 CI가 AWS 인증 자체를 못 함~~ | ~~신뢰정책에 리포 추가~~ | ✅ **해당 없음(2026-09-15) — 배포 소스를 `final` 저장소로 정정하면서 자동 해소됨(위 "중요한 이점" 참고)** |
| 2 | `lovebug-ec2-fastapi`의 인라인 정책이 ECR 리소스를 `repository/fastapi`로 허용하는데 실제 리포는 `repository/lovebug/fastapi` — 이름이 달라 EC2가 이미지를 pull 못 함. SSM 권한도 `/prod/llm/*`로 돼 있는데 그 경로엔 파라미터가 하나도 없음(관례 불일치 — 아래 재개 세션 로그 참고) | 리소스 ARN 수정 + SSM 권한 경로를 `/lovebug/llm/*`로 정정(스프링의 `/lovebug/rds/*` 관례와 통일) + `/lovebug/rds/*` SSM 읽기 권한 추가(RDS 공유 크리덴셜용) | ❌ **미완료 — 세션 자동승인 정책이 IAM 쓰기 작업을 차단함. 아래 "사용자가 직접 실행" 참고** |
| 3 | `lovebug-sg-fastapi` 인바운드가 SSH뿐, 앱 포트(8000) 규칙 없음 | ALB SG(`sg-0a2f734bab8c01d84`)발 TCP 8000 인바운드 추가 | ✅ 완료 |
| 4 | `lovebug-mysql` RDS SG가 스프링 EC2 SG만 3306 허용 — fastapi가 DB 접속 불가 | fastapi SG(`sg-06c53740aeb58ee99`)발 TCP 3306 인바운드 추가 | ✅ 완료 |
| 5 | fastapi용 ALB 타깃그룹·리스너 규칙이 없음 | 타깃그룹 `lovebug-tg-fastapi`(8000, 헬스체크 `/health`) 생성 + 인스턴스 등록 + 443·**80** 리스너 양쪽에 경로 정확일치 규칙(`/health`·`/extract`·`/contrast`·`/jobs/extract`·`/jobs/contrast`) 추가(그 외는 기존대로 스프링/HTTPS 리다이렉트) | ✅ 완료 |
| 6 | CodeDeploy 앱 `lovebug`엔 스프링 전용 배포그룹(`lovebug-codedeploy`, ASG 블루/그린)만 있음 | 같은 앱 아래 신규 배포그룹 `lovebug-fastapi-codedeploy`(**IN_PLACE**, EC2 태그 `Name=lovebug-fastapi`로 직접 타깃 — ASG 아님, 단일 인스턴스라서) 생성 | ✅ 완료 |
| 7 | `/lovebug/llm/*`(EC2 역할이 권한을 받아야 할 경로, IAM 정책은 여전히 `/prod/llm/*`를 가리키고 있어 위 2번과 함께 고쳐야 함)에 파라미터가 하나도 없음 | `/lovebug/llm/GEMINI_API_KEY_1` 등 생성 필요 — **실제 키 값은 이 세션이 절대 안 넣는다** | ⬜ **사용자가 할 일** |
| 8 | 인스턴스에 CodeDeploy 에이전트가 없거나 응답하지 않음 — **2026-09-15 첫 실제 배포 시도(`d-L5YZJ3OTK`)로 확정.** `BeforeBlockTraffic`에서 `CodeDeploy agent was not able to receive the lifecycle event` 에러, 우리 `appspec.yml` 훅은 전혀 실행 안 됨 | 사용자가 EC2에 직접 접속해 `codedeploy-agent` 설치(아래 "PR #75 머지 → 첫 실제 배포 시도" 절의 설치 명령 참고) | ❌ **확정된 블로커 — 사용자가 할 일** |
| 9 | `deploy/appspec.yaml`+`taskdef.json`이 ECS 전용 스키마 | EC2 포맷(`appspec.yml`+`hooks`+쉘 스크립트)으로 전면 교체, 스프링 것 그대로 이식 | ✅ 완료 |
| 10 | 인스턴스가 ARM64인데 빌드 워크플로는 아키텍처 명시 없음(기본 x86_64) | `ubuntu-24.04-arm` 러너 + `platforms: linux/arm64`로 네이티브 빌드(스프링과 동일) | ✅ 완료 |
| 11 | `lovebug/fastapi` 리포가 IMMUTABLE인데 워크플로가 `latest` 태그도 push — 두 번째 배포부터 실패 | `latest` 제거, `$GITHUB_SHA` 단일 태그만 | ✅ 완료 |
| 12 | **(2026-09-15 새로 발견)** `app/queue_consumer.py`의 `_post_callback()`이 작업 완료를 `BACKEND_CALLBACK_BASE_URL`+`/api/internal/llm/**`로 동기 HTTP 콜백하는데, `start_container.sh`가 이 변수를 설정하지 않아 코드 기본값(`http://localhost:8080`)으로 떨어진다 — prod 컨테이너 안에선 그 주소에 아무것도 없어(스프링은 별도 EC2) IAM/SSM을 다 고쳐도 작업 완료 통보가 전부 실패했을 것 | `start_container.sh`에 `BACKEND_CALLBACK_BASE_URL=https://lovebug-alb-1930145637.ap-northeast-2.elb.amazonaws.com` 추가(사용자 확인 — 스프링이 블루/그린 2대라 프라이빗 IP를 고정할 수 없어 ALB 경유로 결정) | ✅ 완료(코드) — ⚠️ **보안 노출 후속 과제 남음, 아래 참고** |

**참고로 확인된 것(손댈 필요 없었음)**: SQS 큐 이름은 이미 일치한다 — 스프링의 `application-prod.yml`도 `app.messaging.sqs.llm-request-queue: lovebug-llm-request`로 우리와 같은 큐를 본다.

### 정정 — ALB 통신을 HTTPS가 아니라 HTTP로 (2026-09-15)

처음엔 443(HTTPS) 리스너에만 경로 규칙(위 표 5번)을 넣었는데, **평문 HTTP로 통신하기로 바뀌었다.** 80(HTTP) 리스너의 원래 동작이 "전부 443으로 301 리다이렉트"라, 그 상태로 두면 `http://lovebug-alb-.../health` 같은 요청이 fastapi로 가지 못하고 계속 HTTPS로 튕겨나갔다.

**조치**: 80 리스너에도 443과 똑같은 경로 정확일치 규칙(`/health`·`/extract`·`/contrast`·`/jobs/extract`·`/jobs/contrast` → `lovebug-tg-fastapi`)을 추가했다 — 리다이렉트 기본 액션보다 먼저 평가되므로 이 다섯 경로만 리다이렉트를 건너뛰고 바로 fastapi로 간다. 다른 경로(스프링)는 그대로 리다이렉트된다.

**확인(실제 curl)**:
```
$ curl http://lovebug-alb-1930145637.ap-northeast-2.elb.amazonaws.com/health
HTTP/1.1 502 Bad Gateway   # 리다이렉트 없이 바로 fastapi 타깃그룹으로 감 — 컨테이너가 아직 없어서 502(정상, 배포 전이라 타깃 없음)

$ curl http://lovebug-alb-1930145637.ap-northeast-2.elb.amazonaws.com/some-other-path
HTTP/1.1 301 → https://.../some-other-path   # 스프링 경로는 그대로 HTTPS 리다이렉트 유지
```

443 리스너의 규칙은 **지우지 않고 남겨뒀다** — HTTPS로 불러도 여전히 동작한다(둘 다 열어둔 상태). HTTPS 쪽을 막을지는 필요하면 알려달라.

### 바뀐 파일

- `deploy/appspec.yaml`(ECS 전용) 삭제, **`deploy/appspec.yml` 신설**(EC2 포맷)
- `deploy/taskdef.json` 삭제(ECS 전용, EC2 배포에서 안 씀)
- **`deploy/scripts/{stop_container.sh,start_container.sh,validate.sh}` 신설** — `start_container.sh`가 `/lovebug/llm/*`(2026-09-15 재개 세션에서 `/prod/llm/*`→`/lovebug/llm/*`로 변경, 아래 재개 세션 로그 참고) 아래 파라미터를 전부 훑어 그 이름 그대로 환경변수로 넘긴다(`GEMINI_API_KEY_1`/`_2`, `GEMINI_MODEL_1`/`_2`를 몇 개를 등록하든 자동으로 반영됨 — test/의 D-38~D-40 키·모델 폴백 체인 규칙과 그대로 맞물림). `GEMINI_MODEL_1=gemini-3.5-flash`(test/ D-36·D-40 실측 채택 모델)는 파라미터가 없어도 동작하도록 기본값으로 고정해뒀다 — SSM에 같은 이름을 등록하면 그 값으로 덮어써진다. RDS 접속 정보는 `/lovebug/rds/url`(JDBC 형식)을 파싱해 `MYSQL_HOST`/`PORT`/`DATABASE`로 쪼갠다
- **`final/.github/workflows/ubidict-py-deploy.yml` 전면 재작성** — ECS 관련 액션(`amazon-ecs-render-task-definition` 등) 전부 제거, 스프링 `deploy.yml`과 동일하게 `docker/build-push-action` + `aws s3 cp` + `aws deploy create-deployment` CLI 조합으로 교체, push 트리거 재활성화(`ubidicExtractor/ubidict-py/**` 경로 필터)

### 사용자가 직접 실행해야 하는 것 (이 세션이 못 하거나 안 하는 것)

1. **IAM 권한 수정 1건(위 표 2번)** — 이 세션의 자동승인 정책이 IAM 쓰기 작업(`put-role-policy`)을 차단한다(EC2 보안그룹·ALB·CodeDeploy는 허용됨). 아래 명령을 **직접** 실행:

   ```bash
   # EC2 인스턴스 역할의 ECR 리소스 경로 수정 + SSM 파라미터 경로 정정 + RDS 공유 SSM 파라미터 읽기 권한 추가
   aws iam put-role-policy --role-name lovebug-ec2-fastapi \
     --policy-name ecr-kms-s3-sqs-ssm-policy \
     --policy-document file://ec2-fastapi-policy.json   # 아래 내용으로 파일을 만들어서 실행

   # ec2-fastapi-policy.json — 기존 정책과 같고 EcrPull의 Resource·LlmApiKey의 경로만 고치고
   # RdsSharedParams 구문 하나를 추가한 것(diff는 위 조사 표 2번 참고):
   # {
   #   "Version": "2012-10-17",
   #   "Statement": [
   #     {"Sid": "EcrLogin", "Effect": "Allow", "Action": "ecr:GetAuthorizationToken", "Resource": "*"},
   #     {"Sid": "EcrPull", "Effect": "Allow", "Action": [
   #         "ecr:BatchCheckLayerAvailability", "ecr:GetDownloadUrlForLayer", "ecr:BatchGetImage"],
   #       "Resource": "arn:aws:ecr:ap-northeast-2:416121583617:repository/lovebug/fastapi"},
   #     {"Sid": "SqsConsumeRequest", "Effect": "Allow", "Action": [
   #         "sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:ChangeMessageVisibility",
   #         "sqs:GetQueueAttributes", "sqs:GetQueueUrl"],
   #       "Resource": "arn:aws:sqs:ap-northeast-2:416121583617:lovebug-llm-request"},
   #     {"Sid": "SqsSendReply", "Effect": "Allow", "Action": "sqs:SendMessage",
   #       "Resource": "arn:aws:sqs:ap-northeast-2:416121583617:lovebug-llm-reply"},
   #     {"Sid": "PayloadBucket", "Effect": "Allow", "Action": ["s3:GetObject", "s3:PutObject"],
   #       "Resource": "arn:aws:s3:::lovebug-s3-416121583617-ap-northeast-2-an/lovebug/llm/*"},
   #     {"Sid": "LlmApiKey", "Effect": "Allow", "Action": ["ssm:GetParameter", "ssm:GetParameters"],
   #       "Resource": "arn:aws:ssm:ap-northeast-2:416121583617:parameter/lovebug/llm/*"},
   #     {"Sid": "RdsSharedParams", "Effect": "Allow", "Action": ["ssm:GetParameter", "ssm:GetParameters"],
   #       "Resource": "arn:aws:ssm:ap-northeast-2:416121583617:parameter/lovebug/rds/*"},
   #     {"Sid": "DecryptParam", "Effect": "Allow", "Action": "kms:Decrypt", "Resource": "*",
   #       "Condition": {"StringEquals": {"kms:ViaService": "ssm.ap-northeast-2.amazonaws.com"}}},
   #     {"Sid": "DeployBundle", "Effect": "Allow", "Action": "s3:GetObject", "Resource": [
   #         "arn:aws:s3:::lovebug-s3-416121583617-ap-northeast-2-an/lovebug/deploy/*",
   #         "arn:aws:s3:::aws-codedeploy-ap-northeast-2/*"]}
   #   ]
   # }
   ```

2. **`/lovebug/llm/GEMINI_API_KEY_1`(SecureString) 값 채우기** — 실제 Gemini API 키. 로컬 dev용 키를 재사용할지 prod 전용 키를 새로 발급할지 정해서 직접:
   ```bash
   aws ssm put-parameter --name /lovebug/llm/GEMINI_API_KEY_1 --type SecureString \
     --value "<실제 키>" --region ap-northeast-2
   ```
   (이 값을 이 세션에는 절대 붙여넣지 않는다 — `AGENTS.md`/`.env.example`의 "API 키는 로그·커밋에 넣지 마라" 원칙)
3. **CodeDeploy 에이전트 설치 여부 확인** — 위 IAM 조치를 마치고 최초 배포를 한 번 돌려서 실패하면(`Unable to find AWS CodeDeploy agent`), EC2 Instance Connect나 SSH로 직접 접속해 설치(Ubuntu이므로 `aws-codedeploy-agent`를 `ruby`로 설치, `s3://aws-codedeploy-ap-northeast-2/latest/install`). 이 세션은 인스턴스 내부에 명령을 보낼 IAM 권한이 없어 원격으로 확인·설치 불가
4. (선택) GitHub 리포(`final-lovebug/final`) Variables에 `AWS_REGION=ap-northeast-2` 등록 — 없어도 워크플로 기본값(하드코딩된 `env.AWS_REGION`)으로 동작

### 검증 순서 (위 1~3이 끝난 뒤)

1. `aws deploy create-deployment --application-name lovebug --deployment-group-name lovebug-fastapi-codedeploy --s3-location ...`를 **GitHub Actions보다 먼저 수동으로 한 번** 돌려 훅 스크립트 자체를 확인 — 또는 `main`에 `ubidicExtractor/ubidict-py/**` 변경을 push하면 자동으로 트리거됨(2026-09-15부터 push 트리거 활성화)
2. `aws deploy get-deployment --deployment-id ...`로 `ApplicationStop → AfterInstall → ValidateService` 단계별 상태 확인, 실패 시 해당 인스턴스에서 `docker logs ubidict-py`
3. `curl http://lovebug-alb-1930145637.ap-northeast-2.elb.amazonaws.com/health` — ALB 경유 확인(**2026-09-15 정정: HTTP로 통신하기로 변경됨** — 위 참고. HTTPS로 불러도 여전히 동작은 한다)
4. `mode: stub`으로 `/jobs/extract` 먼저 확인(비용 없음) → `mode: real`로 실제 확인
5. GitHub Actions "ubidict-py Deploy" 워크플로 실행 로그(Actions 탭)에서 build→deploy 단계별 성공 여부 확인

## 재개 세션 로그 (2026-09-15, 새 세션)

배포 실패(`ea43030`) 이후 새 세션에서 이어받아 진행. **AWS 상태를 read-only로 다시
조회해 위 조사 내용이 여전히 정확한지 독립적으로 재확인했다** — 결과 동일:
`lovebug-fastapi` 인스턴스(`i-069a154f01e676a57`, running), ECR `lovebug/fastapi`(이미지
0개), ALB 타깃그룹 `lovebug-tg-fastapi`(unhealthy — 아직 아무것도 안 떠서 정상),
80·443 리스너 둘 다 5개 경로 규칙 존재, CodeDeploy 배포그룹
`lovebug-fastapi-codedeploy`(배포 이력 0건) — 전부 위 표와 일치. `ssm get-parameters-by-path
--path /lovebug --recursive`로도 `/lovebug/llm/*`엔 파라미터가 없고 `/lovebug/rds/*` 등
기존 스프링 관례 경로만 있음을 재확인(값은 확인하지 않음, 이름만).

**SSM 경로 결정 — `/prod/llm/*` → `/lovebug/llm/*`로 변경.** 사용자에게 다시 확인:
`/prod/llm/*`는 이 프로젝트 전체에서 유일하게 `/lovebug/` 접두어를 안 쓰는 경로라 스프링
백엔드의 `/lovebug/rds/...`·`/lovebug/jwt/...` 등 기존 관례와 어긋난다고 지적했고,
**`/lovebug/llm/*`로 통일하기로 확정**(AWS에 아직 파라미터가 하나도 없어 마이그레이션
비용 없음). 이 세션에서 바꾼 파일: `deploy/scripts/start_container.sh`(SSM 조회 경로),
이 문서의 관련 표·IAM 정책 JSON 예시·`put-parameter` 예시 명령 전부.

**dev/prod 분리 — 기존 결정 유지로 재확정.** "dev, prod로 나눠서"라는 이번 요청이 AWS에
별도 dev 환경을 새로 만들라는 뜻인지 사용자에게 확인했고, 기존 결정(dev=로컬,
prod=이 EC2 1대) 유지로 답변받음 — AWS 쪽에 dev용 리소스를 추가하지 않는다.

**배포 소스 정정 — 독립 저장소가 아니라 `final` 모노레포.** 처음엔 독립 저장소
(`D:\...\ubidicExtractor`, git remote `final-lovebug/ubidicExtractor`)를 배포 소스로
가정하고 거기 SSM 경로 수정을 커밋했으나(`8a9067e`, push 안 함), **사용자가 정정**:
"submodule쪽에서 배포하는 게 아니라 develop 브랜치에 있는 것을 배포할 예정" →
"final/ubidicExtractor에 있는 것을 aws에 배포할 예정이고 독립 저장소는 이제 상관 없다."
확인해보니 `final` 쪽 복사본(원래 깨진 submodule을 `897d983`로 일반 디렉터리 편입한
것)은 **EC2 전환(2026-09-15) 이전 스냅샷에서 멈춰 있었다** — `deploy/appspec.yaml`+
`taskdef.json`(ECS 전용, 폐기된 설계) 그대로, `task.md`도 77줄짜리 구버전,
`.github/workflows/ubidict-py-deploy.yml`도 ECS 액션+`workflow_dispatch`만 트리거.
이번 세션에서 `final` 쪽에 EC2 전환을 그대로 이식했다: `deploy/appspec.yaml`·
`taskdef.json` 삭제 후 `appspec.yml`+`scripts/{stop_container.sh,start_container.sh,
validate.sh}` 신설, `.github/workflows/ubidict-py-deploy.yml` 전면 재작성(EC2/S3-번들
패턴, push 트리거 재활성화), 이 `task.md`를 최신 내용으로 갱신. **부수 효과 하나**:
배포 워크플로가 이미 신뢰된 `final` 저장소 안에 있으므로 독립 저장소 쪽에서 막혔던
"OIDC 신뢰정책에 리포 추가 필요" 블로커가 자동으로 없어졌다(위 표 1번 참고).

## 배포 시도 — `main`에 워크플로가 없어 막힘 (2026-09-15)

사용자가 IAM 정책 수정 + `/lovebug/llm/GEMINI_API_KEY_1` 등록을 AWS 콘솔에서 완료.
`develop`에 `BACKEND_CALLBACK_BASE_URL` 수정까지 커밋·push한 뒤 `gh workflow run`으로
"ubidict-py Deploy"를 `develop` ref로 수동 트리거하려 했으나 **실패**:

```
$ gh workflow list
deploy          active  354713746
front-cd        active  356485235
oidc-check      active  354576700
ubidict-py CI   active  358451588   # ubidict-py Deploy가 없다
```

원인: GitHub Actions는 `workflow_dispatch`로 실행하려면 그 워크플로 **파일이 기본
브랜치(`main`)에도 있어야** API가 인식한다. 확인해보니(`git ls-tree origin/main --
.github/workflows/`) `main`엔 `deploy.yml`·`front-cd.yml`·`oidc-check.yml`뿐이고
`ubidict-py-deploy.yml`은 **`develop`에만 있고 `main`엔 한 번도 올라간 적이 없다**
(`ubidict-py-ci.yml`은 `push`로 이미 여러 번 실행돼서 GitHub이 인식하지만, deploy는
`workflow_dispatch` 전용이라 실행 이력이 없어 인식 자체가 안 됨).

**해결하려면 `develop`을 `main`에 병합해야 하는데, 지금 `develop`엔 다른 팀원들이 최근
머지한 검증 안 된 변경(문서 삭제 기능, 리뷰 UI 개편, 프론트 라우트 개명, DB 마이그레이션
`V430` 등)이 함께 쌓여 있다.** `main`에 push하면 `paths` 필터 때문에 스프링
`deploy.yml`(`backend/**`·`deploy/**` 변경)·프론트 `front-cd.yml`(`frontend/**` 변경)도
같이 트리거될 가능성이 높다 — 즉 "fastapi 배포 한 번 시도"가 **스프링+프론트 운영
배포까지 함께 일으키는** 훨씬 큰 액션이 된다.

**사용자 결정(2026-09-15): 지금은 멈추고 팀과 먼저 논의.** 이 세션은 `main` 병합을
진행하지 않는다. 재개 시 고려할 선택지:
1. 팀원들과 `develop→main` 병합 시점을 맞춘 뒤 정상적으로 병합(스프링·프론트도 같이
   배포되는 걸 감수)
2. `ubidict-py-deploy.yml`(과 CI 파일)만 먼저 `main`에 올리는 별도의 작은 PR을 만들어
   다른 변경과 분리 — 워크플로 파일 자체는 배포 로직이 없어(다른 파일을 안 건드림)
   `deploy.yml`/`front-cd.yml`의 `paths` 필터에 안 걸리므로 스프링·프론트 배포를
   유발하지 않을 가능성이 높다(단, 실제로 그런지는 검증 필요) — **✅ 아래 절 참고,
   실행 완료(PR #75)**
3. `develop`에서 직접 `aws deploy create-deployment`를 수동으로 한 번 돌려 CodeDeploy
   훅 자체만 검증(빌드된 이미지가 있어야 함 — ECR에 수동으로 이미지를 먼저 push해야
   하므로 이것도 결국 CI 없이 수동 작업이 많이 필요)

### 후속 — 옵션 2 실행: PR #75 (2026-09-15)

사용자가 `paths` 필터 동작을 직접 재확인해 달라고 요청해 4개 워크플로의 `on:` 블록을
전부 대조했다 — **필터는 정확히 그렇게 동작한다.** 문제는 `develop→main` 전체 병합이
"fastapi 파일만 바뀐 push"가 아니라서(스프링·프론트 파일도 같이 바뀜) 필터가 다 걸리는
것뿐. 추가로 확인하며 **더 근본적인 원인을 하나 더 발견**: `main`의 `ubidicExtractor`가
아직 **깨진 submodule gitlink**(`git ls-tree origin/main -- ubidicExtractor` →
`160000 commit eb5a4ee...`, 해석 불가 SHA, `.gitmodules`도 없음)였다 — 실제 파일이
전혀 없었다. `897d983`(깨진 submodule → 일반 디렉터리 전환) 커밋이 `develop`에만
있고 `main`엔 병합된 적이 없어서다.

**조치**: `main` 기준 새 브랜치(`feat/ubidict-py-fastapi-deploy`)를 만들어
`git checkout origin/develop -- ubidicExtractor/ .github/workflows/ubidict-py-ci.yml
.github/workflows/ubidict-py-deploy.yml`로 **fastapi 관련 파일만 선별적으로** 가져와
커밋(269개 파일 — `ubidicExtractor/test/`·`ubidict-py/` 전체 + 워크플로 2개,
`backend/**`·`frontend/**`는 전혀 안 건드림 확인) → push →
**[PR #75](https://github.com/final-lovebug/final/pull/75)** 생성. 머지되면 깨진
gitlink가 실제 디렉터리로 바뀌면서 `ubidict-py-deploy.yml`이 `main`에 생겨
`workflow_dispatch`가 가능해진다 — `paths` 필터 덕분에 스프링·프론트 워크플로는
트리거되지 않는다. **아직 머지되지 않았다** — 팀·사용자 검토 후 머지 여부 결정.

**새로 발견한 것 — 낡은 복사본(위에서 이미 처리).** `final`에 이 프로젝트의 ECS 시절
낡은 복사본이 들어와 있다는 걸 먼저 발견해 사용자에게 물었으나, 실제로는 그게 바로
**진짜 배포 소스**였다 — 이 세션의 첫 이해가 거꾸로였다(독립 저장소를 배포 소스로
오인). 정정 후 위 내용대로 이식 완료.

**여전히 사용자가 직접 해야 함(위 "사용자가 직접 실행해야 하는 것" 절 그대로, 이제
2건뿐)**: EC2 역할 인라인 정책 수정 1건(ECR ARN+SSM 경로), Gemini API 키 등록
(`/lovebug/llm/GEMINI_API_KEY_1`), CodeDeploy 에이전트 설치 확인. 이 셋 없이는 push든
수동 트리거든 배포가 끝까지 성공할 수 없다 — 이 세션은 IAM 쓰기·시크릿 등록을 실행하지
않는다(루트 `CLAUDE.md` "운영 DB·AWS 리소스에 영향을 주는 명령 실행 금지").

### PR #75 머지 → 첫 실제 배포 시도 → 실패 (2026-09-15, 표 8번 확정)

사용자가 IAM 정책 수정 + Gemini 키 등록을 AWS 콘솔에서 완료했다고 확인(2026-09-15)한
뒤 PR #75를 머지. **머지 push가 `ubidict-py-deploy.yml`의 `push: branches: [main]`
트리거에 걸려 자동으로 첫 배포가 시작됐다**(수동 트리거 필요 없었음):

- ✅ **build 단계 성공**(41초) — 이미지가 ECR `lovebug/fastapi`에 정상 push됨(즉 표
  2번의 IAM 수정이 실제로 반영됐다는 뜻은 아님 — build 단계는 ECR push 권한만 쓰고,
  EC2 역할의 ECR **pull** 권한은 별개라 여기서는 검증되지 않는다)
- ✅ merge가 스프링(`deploy.yml`)·프론트(`front-cd.yml`)는 전혀 안 건드림 재확인(실행
  이력에 변화 없음) — `paths` 필터가 설계대로 동작
- ❌ **CodeDeploy 배포(`d-L5YZJ3OTK`) 실패** — `BeforeBlockTraffic` 단계에서 막히고
  `ApplicationStop`부터 `AfterAllowTraffic`까지 전부 `Skipped`(우리 `appspec.yml` 훅은
  하나도 실행조차 안 됐다). 에러:
  ```
  errorCode: UnknownError
  message: "CodeDeploy agent was not able to receive the lifecycle event.
             Check the CodeDeploy agent logs on your host and make sure
             the agent is running and can connect to the CodeDeploy server."
  deploymentInfo.errorInformation.code: HEALTH_CONSTRAINTS
  ```
  **표 8번이 확정됐다** — CodeDeploy 에이전트가 `lovebug-fastapi` 인스턴스에 없거나
  실행 중이 아니다. IAM/SSM/Gemini 키 문제가 전혀 아니라 애초에 에이전트가 없어서 우리
  배포 스크립트(`stop_container.sh`/`start_container.sh`/`validate.sh`)는 한 줄도
  실행되지 못했다.

**사용자가 직접 해야 함(신규 확정)**: EC2 Instance Connect 또는 Session Manager로
`lovebug-fastapi`(`i-069a154f01e676a57`)에 접속해 CodeDeploy 에이전트 설치:
```bash
sudo apt update && sudo apt install -y ruby-full wget
cd /home/ubuntu
wget https://aws-codedeploy-ap-northeast-2.s3.ap-northeast-2.amazonaws.com/latest/install
chmod +x ./install
sudo ./install auto
sudo systemctl status codedeploy-agent   # active (running) 확인
```
인스턴스 역할(`lovebug-ec2-fastapi`)에 `AmazonSSMManagedInstanceCore`가 이미 붙어 있어
**Session Manager 접속이 SSH 키 없이 가능할 가능성이 높다**(EC2 콘솔 → 인스턴스 선택 →
"연결" → "Session Manager" 탭 → "연결" 버튼) — SSM 에이전트 자체가 응답하는지는 이
세션이 확인 못 함(`ssm:DescribeInstanceInformation` 권한 없음). 안 되면 SSH(키
`lovebug-pem`, 22번 포트는 NAT 인스턴스 보안그룹에서만 허용돼 있어 바스천 경유 필요).

설치 후 다시 `main`에 아무 커밋(예: 이 `task.md` 갱신)을 push하거나
`gh workflow run "ubidict-py Deploy" --ref main`으로 재시도하면 된다.

### 진짜 원인 — 에이전트가 아니라 IAM 인스턴스 프로파일 자체가 안 붙어 있었다 (2026-09-16)

사용자가 에이전트를 설치하고 나니 로그에 더 구체적인 에러가 찍혔다:

```
ERROR [codedeploy-agent]: InstanceAgent::Plugins::CodeDeployPlugin::CommandPoller:
Missing credentials - please check if this instance was started with an IAM instance profile
```

`aws ec2 describe-instances`로 직접 확인 — **`lovebug-fastapi` 인스턴스에 IAM
인스턴스 프로파일이 현재 전혀 연결돼 있지 않았다**(`IamInstanceProfile.Arn: null`).
`aws ec2 describe-iam-instance-profile-associations`도 빈 배열. 에이전트 로그 메시지가
말 그대로 사실이었다 — 역할 자체가 안 붙어 있어 인스턴스 메타데이터로 자격 증명을
못 받아온 것. (참고: 이전 조사 때는 `lovebug-ec2-fastapi`가 붙어 있는 것으로
확인됐었다 — 그 사이 콘솔 작업 중 분리된 것으로 추정, 원인은 불명.)

**조치(사용자가 콘솔에서 직접)**: EC2 콘솔 → 인스턴스 선택 → 작업(Actions) →
보안(Security) → "IAM 역할 수정(Modify IAM role)" → `lovebug-ec2-fastapi` 선택 →
업데이트. 이후 `sudo systemctl restart codedeploy-agent`로 에이전트 재시작.

## 배포 성공 확인 (2026-09-16)

여러 차례 재시도(`main`에 직접 커밋된 `21c797a`·`d52546c` 핫픽스 포함, 이 세션이 관여
안 한 시도도 다수) 끝에 CodeDeploy 배포 `d-ZA4MW0QTK`가 **전체 lifecycle 훅 성공**
(`ApplicationStop`→`DownloadBundle`→`BeforeInstall`→`Install`→`AfterInstall`→
`ApplicationStart`→`ValidateService` 전부 `Succeeded`, `ValidateService`가 인스턴스
내부에서 `curl localhost:8000/health`로 검증까지 통과).

**단, 그 사이 EC2 인스턴스 자체가 교체돼 있었다** — 이전 `i-069a154f01e676a57`가 아니라
새 인스턴스 `i-06dff037d2928927c`(2026-09-15 22:23 KST 기동, IAM 역할
`lovebug-ec2-fastapi` 정상 연결 확인)가 지금의 `lovebug-fastapi`다. 배포 자체는
성공했는데 ALB 쪽 `curl /health`가 503을 냈던 이유: **이 배포그룹은
`deploymentOption: WITHOUT_TRAFFIC_CONTROL`(`LoadBalancerInfo: null`)이라 CodeDeploy가
ALB 타깃그룹 등록을 자동으로 안 한다** — 예전 인스턴스는 누군가 수동으로 등록해 뒀던
것이고, 교체된 새 인스턴스는 등록이 안 된 채 남아 있었다(`lovebug-tg-fastapi` 타깃 0개).

**조치**: 사용자가 새 인스턴스를 타깃그룹에 수동 등록(`aws elbv2 register-targets` 또는
콘솔 "대상 그룹 → 등록"). 등록 직후 확인:

```
$ aws elbv2 describe-target-health --target-group-arn ... (lovebug-tg-fastapi)
TargetHealth.State: healthy

$ curl http://lovebug-alb-1930145637.ap-northeast-2.elb.amazonaws.com/health
HTTP 200
{"status":"ok","model":"gemini-3.5-flash"}
```

**✅ ubidict-py EC2 배포 최초 성공.** ALB → 타깃그룹 → 컨테이너까지 plain HTTP로 정상
동작 확인. `GEMINI_API_KEY_1`·`GEMINI_MODEL_1`이 SSM에서 정상적으로 읽혀 `/health`
응답에 실제 모델명이 찍히는 것까지 확인했다(env var 대소문자 문제·`GEMINI_MODEL_1`
넘버링 문제 등 `main`에 직접 반영된 핫픽스들이 실제로 필요했다는 뜻).

**남은 후속 과제(모두 이번 배포 성공과 무관, 별도 작업)**:
- `/api/internal/llm/**` 보안 노출(위 표 12번·"다음에 할 것" 절 참고) — 아직 미해결
- 인스턴스가 교체될 때마다 타깃그룹 재등록이 수동이라는 문제 — Auto Scaling Group으로
  바꾸거나, CodeDeploy 배포그룹에 `LoadBalancerInfo`를 연결해 자동화하는 것을 고려할 것
- `mode: stub`으로 `/jobs/extract` 먼저 확인(비용 없음) → `mode: real`로 실제 확인은
  아직 안 함(이번엔 `/health`까지만 검증)

## OpenTelemetry / Grafana Cloud 연동 — 실행 계획 (2026-09-16, 아직 미착수)

`모니터링` 결정 표(위)가 예전부터 "OpenTelemetry는 미정 — 나중에 추가할 자리만"으로
열어 뒀고, 미결 사항 목록에도 "[ ] OpenTelemetry 계측 여부/방식 결정되면 추가(지금은
의존성 자체를 안 넣음)"로 남아 있었다. `final/backend`가 이미 같은 Grafana Cloud로
trace·log·metric 세 신호를 OTLP로 보내는 걸 실제로 확인했으니(아래 "참고 — 백엔드
패턴" 절), 그걸 그대로 재사용하는 방향으로 이번에 구체적인 실행 계획을 세운다.
**코드는 아직 하나도 안 건드렸다 — 이 절은 계획만이다.**

### 목표

ubidict-py도 trace·log·metric을 OTLP로 같은 Grafana Cloud 인스턴스에 보낸다 —
새 관측 스택을 따로 만들지 않고 백엔드가 이미 쓰는 것에 합류한다.

### 참고 — 백엔드 패턴(`final/backend/src/main/resources/application-prod.yml`에서 그대로 읽음)

- 엔드포인트·인증은 SSM `/lovebug/otel/endpoint`·`/lovebug/otel/auth`에서 온다.
  `auth`는 `instanceID:token` 원문이고, 쓰는 쪽이 직접 base64 인코딩해
  `Authorization: Basic <base64>` 헤더를 만들어야 한다(백엔드는 이걸
  `OtlpAuthHeaderEnvironmentPostProcessor`라는 커스텀 코드로 한다).
- 신호별 엔드포인트는 `{otel.endpoint}/v1/traces`·`/v1/logs`·`/v1/metrics` — 베이스
  URL 뒤에 신호별 경로를 직접 붙인다.
- 샘플링은 `OTEL_SAMPLE_RATIO`(기본 `0.1`)로 트레이스만 조절.
- 킬스위치 `/lovebug/otel/enabled` — 파라미터가 없으면 켠 것으로 본다(현재 실제로
  이 파라미터는 없다 — 즉 지금 상태 그대로 켜는 게 기본값과 일치).
- 리소스 속성으로 `service.version`(배포 이미지 태그)·`deployment.environment.name=prod`를
  싣는다 — 어느 배포가 낸 신호인지 추적하기 위함.

### ubidict-py에서 다른 점 — Python은 훨씬 가볍게 갈 수 있다

Java/Spring과 달리 Python OTel SDK는 **표준 `OTEL_*` 환경변수를 그대로 읽는다** —
백엔드의 `OtlpAuthHeaderEnvironmentPostProcessor` 같은 커스텀 코드가 필요 없다(단,
`Authorization` 헤더의 base64 인코딩 자체는 여전히 `start_container.sh`에서 해 줘야
한다 — 원문을 SSM이 그대로 주기 때문). `opentelemetry-instrument`라는 실행 래퍼로
uvicorn을 감싸기만 하면 FastAPI HTTP 요청(`/health`·`/extract`·`/contrast`·
`/jobs/*`)의 트레이스는 코드 변경 없이 나온다.

### Phase 1 — 최소 계측(HTTP 요청 트레이스 + 로그 브리지)

- [ ] **(제안, 승인 필요 — 루트 `CLAUDE.md` "새 의존성은 사전 제안·승인")**
      `pyproject.toml`에 추가할 의존성:
      - `opentelemetry-distro` (`opentelemetry-instrument` 실행 래퍼 포함)
      - `opentelemetry-exporter-otlp-proto-http`(OTLP HTTP 익스포터 — 백엔드와 같은
        프로토콜, gRPC 포트가 아니라 HTTP 포트로 나간다는 뜻)
      - `opentelemetry-instrumentation-fastapi`(HTTP 요청 자동 계측)
      - `opentelemetry-instrumentation-botocore`(boto3/SQS 호출 자동 계측 — 설치만
        하면 `receive_message`/`send_message`/`delete_message`가 스팬으로 잡힌다,
        커스텀 코드 불필요)
      - `opentelemetry-instrumentation-logging`(Python `logging` 모듈 → OTLP 로그
        브리지 — `main.py`에 이미 있는 `logging.basicConfig`를 그대로 살리면서 로그도
        Grafana Cloud로 보낼 수 있다)
- [ ] `Dockerfile`의 `CMD`를 `["opentelemetry-instrument", "uvicorn", "app.main:app",
      "--host", "0.0.0.0", "--port", "8000"]`로 변경(`opentelemetry-instrument`가
      환경변수를 읽어 자동 계측을 부팅한 뒤 원래 커맨드를 실행)
- [ ] `deploy/scripts/start_container.sh`에 SSM 조회 + 환경변수 배선 추가(RDS 정보를
      읽는 기존 블록과 같은 자리에):
  ```bash
  OTEL_RAW_ENDPOINT=$(aws ssm get-parameter --name /lovebug/otel/endpoint --with-decryption \
    --region "$REGION" --query Parameter.Value --output text 2>/dev/null || true)
  OTEL_RAW_AUTH=$(aws ssm get-parameter --name /lovebug/otel/auth --with-decryption \
    --region "$REGION" --query Parameter.Value --output text 2>/dev/null || true)

  if [ -n "$OTEL_RAW_ENDPOINT" ] && [ -n "$OTEL_RAW_AUTH" ]; then
    OTEL_AUTH_HEADER="Authorization=Basic $(printf '%s' "$OTEL_RAW_AUTH" | base64 -w0)"
    OTEL_ARGS=(
      -e OTEL_SERVICE_NAME=ubidict-py
      -e OTEL_EXPORTER_OTLP_TRACES_ENDPOINT="${OTEL_RAW_ENDPOINT}/v1/traces"
      -e OTEL_EXPORTER_OTLP_LOGS_ENDPOINT="${OTEL_RAW_ENDPOINT}/v1/logs"
      -e OTEL_EXPORTER_OTLP_METRICS_ENDPOINT="${OTEL_RAW_ENDPOINT}/v1/metrics"
      -e OTEL_EXPORTER_OTLP_HEADERS="$OTEL_AUTH_HEADER"
      -e OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
      -e OTEL_TRACES_SAMPLER=traceidratio
      -e OTEL_TRACES_SAMPLER_ARG=0.1
      -e OTEL_RESOURCE_ATTRIBUTES="deployment.environment.name=prod,service.version=$TAG"
      -e OTEL_LOGS_EXPORTER=otlp
      -e OTEL_METRICS_EXPORTER=otlp
      -e OTEL_PYTHON_LOGGING_AUTO_INSTRUMENTATION_ENABLED=true
    )
  else
    echo "경고: /lovebug/otel/* 파라미터를 못 읽었다 — 관측 없이 기동한다." >&2
    OTEL_ARGS=(-e OTEL_SDK_DISABLED=true)
  fi
  ```
  (`docker run` 호출의 `"${ENV_ARGS[@]}"` 옆에 `"${OTEL_ARGS[@]}"`도 추가)
- [ ] **사용자가 직접(AWS 콘솔/CLI)**: `lovebug-ec2-fastapi` 인라인 정책에 새 Sid
      추가 — 기존 `RdsSharedParams`와 같은 패턴:
  ```json
  {"Sid": "OtelSharedParams", "Effect": "Allow", "Action": ["ssm:GetParameter", "ssm:GetParameters"],
    "Resource": "arn:aws:ssm:ap-northeast-2:416121583617:parameter/lovebug/otel/*"}
  ```
  (`DecryptParam` Sid는 이미 `kms:ViaService=ssm...`로 범위가 잡혀 있어 추가 조치 불필요)

### Phase 2 — 커스텀 스팬(선택, Phase 1 이후 필요성 보고 결정)

- [ ] SQS 컨슈머 루프(`app/queue_consumer.py`)는 HTTP 요청이 아니라 백그라운드
      asyncio 태스크라 자동 계측이 안 잡는다 — 메시지 하나 처리할 때마다
      `tracer.start_as_current_span("process_llm_job")`으로 수동 스팬을 열어야
      실제로 무슨 일이 오래 걸리는지(멱등성 조회·DB 읽기·Gemini 호출·콜백) 트레이스에서
      보인다
- [ ] Gemini 호출(`app/pipeline/llm.py`·`contrast_llm.py`)에 수동 스팬 — 모델 폴백
      체인이 몇 번째 키/모델에서 성공했는지를 스팬 속성으로 남기면 실패 원인 추적이
      쉬워진다
- [ ] 메트릭(선택) — 잡 처리 시간·성공/실패 카운터를 OTel Metrics API로 직접 만들지,
      로그 기반 메트릭으로 충분한지는 Phase 1 배포 후 실제로 Grafana Cloud에서 뭐가
      부족한지 보고 결정

### 검증

1. Phase 1 배포 후 `curl .../health` 몇 번 호출 → Grafana Cloud(Tempo/Explore)에서
   `service.name=ubidict-py`로 트레이스가 잡히는지 확인
2. 로그 브리지가 되는지 — 기존 `usage_log.py`/`contrast_usage_log.py`의 stdout JSON
   로그는 그대로 두고, Python `logging` 경유 로그(`main.py`의 `logging.basicConfig`
   로거)가 Grafana Cloud Loki에도 나타나는지 확인
3. `/lovebug/otel/*` 파라미터를 못 읽는 상황(IAM 조치 전)에서도 컨테이너가
   `OTEL_SDK_DISABLED=true`로 정상 기동하는지 확인 — 관측 때문에 서비스가 죽으면 안
   된다(백엔드의 `D-98`과 같은 원칙)
