#!/bin/bash
set -euo pipefail

# AfterInstall 훅 — 이미지를 받아 컨테이너를 새로 띄운다.
# 시크릿은 여기(인스턴스 자신의 IAM 역할, lovebug-ec2-fastapi)에서만 읽는다.
# GitHub Actions나 이 스크립트를 작성한 세션을 거치지 않는다 — set -x는
# 절대 켜지 않는다(값이 CodeDeploy 로그에 그대로 찍힌다).

REGION=ap-northeast-2
REGISTRY=416121583617.dkr.ecr.ap-northeast-2.amazonaws.com
IMAGE_REPO="$REGISTRY/lovebug/fastapi"

# ── 선행 조건 확인 ────────────────────────────────────────────────────────
# 아래는 전부 외부 명령에 의존한다. 하나라도 없으면 `set -euo pipefail` 때문에
# 파이프 중간에서 그냥 exit 127("command not found")로 죽고, CodeDeploy 로그에는
# "failed with exit code 127"만 남아 무엇이 없는지 알 수 없다(2026-09-15에 실제로
# 겪음 — 인스턴스에 docker·aws 둘 다 없었다). 그래서 여기서 먼저 이름을 찍는다.
# stop_container.sh는 `|| true`로 docker 부재를 삼키므로 이 훅이 첫 신호다.
MISSING=()
# curl은 이 스크립트가 아니라 뒤따르는 validate.sh가 쓴다 — 여기서 같이 잡지 않으면
# 헬스체크가 150초를 헛돌고 나서야 실패한다.
for CMD in aws docker curl; do
  command -v "$CMD" >/dev/null 2>&1 || MISSING+=("$CMD")
done
if [ "${#MISSING[@]}" -ne 0 ]; then
  echo "선행 조건 없음: ${MISSING[*]} — 인스턴스에 설치돼 있지 않다." >&2
  echo "  이 인스턴스는 Ubuntu다(AL2023이 아니다 — dnf가 없다)." >&2
  echo "  docker : sudo apt-get update && sudo apt-get install -y docker.io && sudo systemctl enable --now docker" >&2
  echo "  aws    : awscli-exe-linux-aarch64.zip(Graviton)로 v2를 설치한다. apt의 awscli는 v1이라 쓰지 않는다." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TAG=$(cat "$SCRIPT_DIR/../IMAGE_TAG")
echo "deploying tag: $TAG"

aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "$REGISTRY"

docker pull "$IMAGE_REPO:$TAG"

# ── /lovebug/llm/* 전부를 그대로 환경변수로 만든다 ────────────────────────
# GEMINI_API_KEY_1(_2, ...)·GEMINI_MODEL_1(_2, ...) 등 몇 개를 등록했든
# 파라미터 이름의 마지막 세그먼트가 그대로 환경변수 이름이 된다
# (test/의 D-38~D-40 키·모델 폴백 체인 규칙과 그대로 맞물린다).
# 경로는 스프링 백엔드의 /lovebug/rds/... 관례와 통일한 것이다(2026-09-15,
# 기존 /prod/llm/*에서 변경 — 그 경로엔 실제 파라미터가 하나도 없었다).
# 번호 붙은 이름(GEMINI_MODEL_1/_2/...)을 여기서 고정하지 않는다 — 아래 -e에는
# 단수 GEMINI_MODEL만 기본값으로 두고, 번호 체인은 전적으로 Parameter Store가
# 정한다. 파이썬 쪽은 번호 붙은 게 하나라도 있으면 단수를 무시하므로
# (model_chain.load_default_model_chain), 파라미터가 있으면 그게 곧 체인이고
# 없으면 이 기본값 하나로 동작한다. 여기에 GEMINI_MODEL_1을 박아 두면 대문자
# 이름이 우선해 SSM의 gemini_model_1만 가려지고 _2~_4는 살아남아 체인이
# 뒤섞인다.
ENV_ARGS=()
while IFS=$'\t' read -r NAME VALUE; do
  [ -z "$NAME" ] && continue
  ENV_ARGS+=(-e "${NAME##*/}=${VALUE}")
done < <(aws ssm get-parameters-by-path --path /lovebug/llm --recursive --with-decryption \
  --region "$REGION" --query "Parameters[].[Name,Value]" --output text 2>/dev/null || true)

if [ "${#ENV_ARGS[@]}" -eq 0 ]; then
  echo "경고: /lovebug/llm/* 에 파라미터가 하나도 없다 — GEMINI_API_KEY를 아직 안 넣은 상태로 보인다." >&2
  echo "       (task.md '사용자가 할 일' 참고) 컨테이너는 뜨지만 real 모드 호출은 전부 실패한다." >&2
fi

# ── 백엔드와 공유하는 RDS 접속 정보 ──────────────────────────────────────
# /lovebug/rds/url은 스프링이 쓰는 JDBC 형식(jdbc:mysql://host:port/db?...)
# 이라 그대로는 못 쓴다 — pymysql이 쓰는 discrete 환경변수로 쪼갠다.
RDS_URL=$(aws ssm get-parameter --name /lovebug/rds/url --with-decryption \
  --region "$REGION" --query Parameter.Value --output text)
RDS_USER=$(aws ssm get-parameter --name /lovebug/rds/username --with-decryption \
  --region "$REGION" --query Parameter.Value --output text)
RDS_PASSWORD=$(aws ssm get-parameter --name /lovebug/rds/password --with-decryption \
  --region "$REGION" --query Parameter.Value --output text)

HOSTPORT_DB="${RDS_URL#jdbc:mysql://}"
HOSTPORT="${HOSTPORT_DB%%/*}"
DB_AND_QUERY="${HOSTPORT_DB#*/}"
DB_NAME="${DB_AND_QUERY%%\?*}"
MYSQL_HOST="${HOSTPORT%%:*}"
MYSQL_PORT="${HOSTPORT##*:}"

docker rm -f ubidict-py 2>/dev/null || true

# ── 작업 완료 콜백 주소 ───────────────────────────────────────────────────
# app/queue_consumer.py의 _post_callback()이 이 값+/api/internal/llm/**로
# POST한다. 스프링이 블루/그린 2대라 프라이빗 IP를 고정할 수 없어 ALB를
# 경유한다(2026-09-15 확정).
#
# **ALB의 AWS 기본 호스트명을 쓰지 않는다**(2026-09-16). https://lovebug-alb-....
# elb.amazonaws.com 으로 치면 ALB 인증서가 api.ubidic.site 용이라 호스트명이 맞지
# 않아 콜백이 전부 SSLCertVerificationError 로 죽는다(실제로 jobId=7 에서 발생).
# *.elb.amazonaws.com 인증서는 발급받을 수 없으므로 도메인 쪽을 쓴다 — 프론트가 쓰는
# 운영 API 주소와 같다(.github/workflows/front-cd.yml).
#
# 평문 HTTP 로 내리는 선택지도 있었지만 쓰지 않는다. /api/internal/** 은 인증 필터를
# 통과하고 방어가 requestId 대조뿐이라(D-70) 전송 구간까지 평문이면 남는 보호가 없다.
#
# **보안 노출이 남아 있다** — /api/internal/**는
# 원래 워커 출발지로만 제한돼야 하는데(docs/AI_CONTRACT.md, NFR-AI-002)
# 지금은 이 ALB 경로가 공개돼 있고, 스프링 EC2의 8080 인바운드도 아직
# 0.0.0.0/0으로 열려 있다 — 보안그룹 제한은 후속 과제로 남겨 둠(사용자 확인).
docker run -d --name ubidict-py \
  --restart unless-stopped \
  -p 8000:8000 \
  --memory 500m \
  --log-driver json-file \
  --log-opt max-size=100m \
  --log-opt max-file=3 \
  -e AWS_REGION="$REGION" \
  -e SQS_REQUEST_QUEUE_URL="https://sqs.$REGION.amazonaws.com/416121583617/lovebug-llm-request" \
  -e SQS_REPLY_QUEUE_URL="https://sqs.$REGION.amazonaws.com/416121583617/lovebug-llm-reply" \
  -e BACKEND_CALLBACK_BASE_URL="https://api.ubidic.site" \
  -e MYSQL_HOST="$MYSQL_HOST" \
  -e MYSQL_PORT="$MYSQL_PORT" \
  -e MYSQL_DATABASE="$DB_NAME" \
  -e MYSQL_USER="$RDS_USER" \
  -e MYSQL_PASSWORD="$RDS_PASSWORD" \
  -e GEMINI_MODEL=gemini-3.5-flash \
  "${ENV_ARGS[@]}" \
  "$IMAGE_REPO:$TAG"

docker image prune -af --filter "until=168h" || true
