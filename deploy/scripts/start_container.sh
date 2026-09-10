#!/bin/bash
set -euo pipefail

REGION=ap-northeast-2
REGISTRY=416121583617.dkr.ecr.ap-northeast-2.amazonaws.com
IMAGE_REPO="$REGISTRY/lovebug/spring"

TAG=$(cat "$DEPLOYMENT_ARCHIVE_DIR/IMAGE_TAG" 2>/dev/null \
      || cat "$(dirname "$0")/../IMAGE_TAG")
echo "deploying tag: $TAG"

aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "$REGISTRY"

docker pull "$IMAGE_REPO:$TAG"

docker rm -f spring 2>/dev/null || true

docker run -d --name spring \
  --restart unless-stopped \
  -p 8080:8080 \
  --memory 1600m \
  --log-driver json-file \
  --log-opt max-size=100m \
  --log-opt max-file=3 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e AWS_REGION="$REGION" \
  "$IMAGE_REPO:$TAG"

docker image prune -af --filter "until=168h" || true
