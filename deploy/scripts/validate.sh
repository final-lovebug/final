#!/bin/bash
set -uo pipefail

for i in $(seq 1 15); do
  if curl -fsS --max-time 5 http://localhost:8080/actuator/health/readiness >/dev/null 2>&1; then
    echo "healthy after $((i * 10))s"
    exit 0
  fi
  sleep 10
done

echo "health check failed"
docker logs --tail 200 spring 2>&1 || true
exit 1
