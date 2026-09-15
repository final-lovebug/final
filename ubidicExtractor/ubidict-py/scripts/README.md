# 로컬 Spring ↔ SQS ↔ FastAPI 목 연동

`Spring dev` 프로필은 LocalStack의 `lovebug-llm-request` 큐에 `mode=MOCK` 요청을
발행한다. 이 워커는 모델·DB를 읽지 않고 고정 용어 추출 결과를 Spring의 HTTP
콜백으로 돌려준다. 완료 응답용 SQS 큐는 없다.

## 준비

먼저 Spring을 `dev` 프로필로 실행한다. Spring Boot Docker Compose가 LocalStack을
함께 띄우며, 첫 요청을 발행할 때 큐도 생성한다.

```bash
cd final/backend
./gradlew bootRun --args='--spring.profiles.active=dev'
```

그 다음 `ubidict-py/.env`에 아래 값을 둔다. LocalStack의 기본 계정 ID는
`000000000000`이다.

```dotenv
AWS_REGION=ap-northeast-2
SQS_ENDPOINT_URL=http://localhost:4566
SQS_REQUEST_QUEUE_URL=http://localhost:4566/000000000000/lovebug-llm-request
BACKEND_CALLBACK_BASE_URL=http://localhost:8080
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
```

워커를 호스트에서 실행한다.

```bash
cd final/ubidicExtractor/ubidict-py
uv run uvicorn app.main:app --reload
```

워커가 컨테이너에서 실행되는 경우 `BACKEND_CALLBACK_BASE_URL`은 보통
`http://host.docker.internal:8080`이어야 한다.

## 확인 방법

Spring에서 용어 추출 또는 문서 대조 작업을 접수한다. 작업 메시지의 `mode`가
`MOCK`이면 워커가 다음 콜백 중 하나를 호출한다.

- 용어 추출: `POST /api/internal/llm/extractions/{jobId}/result` — 고정 후보어 `결제` 1건
- 문서 대조: `POST /api/internal/llm/checks/{jobId}/result` — 빈 제안 목록

Spring은 `requestId`를 대조한 뒤 작업 상태를 완료로 바꾸며, 프론트는 기존 작업
조회 API를 폴링해 결과를 확인한다. 콜백이 5xx 또는 네트워크 오류면 워커는 SQS
메시지를 지우지 않아 재시도되고, 2xx·4xx면 메시지를 지운다.

---

# EC2 인스턴스 프로비저닝 — `bootstrap_instance.sh`

`lovebug-fastapi` EC2(Ubuntu)에 **docker와 aws CLI v2를 까는 1회성 스크립트**다.
위의 로컬 목 연동과는 무관하며, CodeDeploy 번들에도 들어가지 않는다(번들은
`deploy/` 하위만 zip한다).

인스턴스에서 root로 한 번 실행한다. 멱등하므로 재실행해도 안전하다.
**세션에서 분리해 띄운다** — ssh가 끊겨도 apt가 중간에 죽지 않는다.

```bash
sudo setsid bash bootstrap_instance.sh > /tmp/bootstrap.log 2>&1 < /dev/null &
tail -f /tmp/bootstrap.log
```

Ubuntu 24.04의 `needrestart`가 패키지 설치 후 ssh·ssm-agent를 자동 재시작해
세션을 끊는 일이 있어, 스크립트가 `NEEDRESTART_SUSPEND=1`로 이를 억제한다.
대신 재시작이 밀린 서비스 목록을 마지막에 출력하므로, 필요하면 배포 창에서
직접 재시작하거나 인스턴스를 재부팅한다.

docker·aws CLI 설치 뒤 `aws sts get-caller-identity`와 ECR 로그인까지 실제로
확인하고 끝난다. 둘 중 하나라도 실패하면 배포는 어차피 실패하므로 여기서 먼저 잡는다.
