#!/bin/bash
set -euo pipefail

REGION=ap-northeast-2
REGISTRY=416121583617.dkr.ecr.ap-northeast-2.amazonaws.com
IMAGE_REPO="$REGISTRY/lovebug/spring"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TAG=$(cat "$SCRIPT_DIR/../IMAGE_TAG")
echo "deploying tag: $TAG"

aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "$REGISTRY"

docker pull "$IMAGE_REPO:$TAG"

docker rm -f spring 2>/dev/null || true

# OTEL_SERVICE_VERSION 은 이미지 태그(= 커밋 SHA)다. 리소스 속성 service.version 으로 실려 나가
# 어느 리비전이 낸 텔레메트리인지 Grafana 에서 구분할 수 있게 한다. 나머지 관측 설정(엔드포인트·
# 자격증명·on/off)은 Parameter Store 의 /lovebug/otel/ 에서 온다.
docker run -d --name spring \
  --restart unless-stopped \
  -p 8080:8080 \
  --memory 1600m \
  --log-driver json-file \
  --log-opt max-size=100m \
  --log-opt max-file=3 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_FLYWAY_ENABLED=false \
  -e AWS_REGION="$REGION" \
  -e OTEL_SERVICE_VERSION="$TAG" \
  "$IMAGE_REPO:$TAG"

docker image prune -af --filter "until=168h" || true
