#!/bin/bash
set -euo pipefail

# lovebug-fastapi EC2(Ubuntu 24.04) 1회성 프로비저닝 — docker와 aws CLI v2를 깐다.
#
# 왜 필요한가: CodeDeploy의 AfterInstall 훅(deploy/scripts/start_container.sh)이
# `aws ecr get-login-password | docker login`으로 시작하는데, 둘 다 인스턴스에
# 없어서 exit 127("command not found")로 죽었다(2026-09-15). ApplicationStop 훅은
# `|| true`로 docker 부재를 삼키기 때문에 AfterInstall이 첫 신호였다.
#
# 이 스크립트는 CodeDeploy 번들에 들어가지 않는다 — 번들은 deploy/ 하위만 zip한다.
# 인스턴스에서 root로 한 번 실행한 뒤에는 다시 돌릴 일이 없다(멱등하므로 재실행은 안전).
#
# 사용법 (인스턴스에서) — 세션이 끊겨도 설치가 중단되지 않게 분리해서 띄운다:
#
#   sudo setsid bash bootstrap_instance.sh > /tmp/bootstrap.log 2>&1 < /dev/null &
#   tail -f /tmp/bootstrap.log
#
# tail을 Ctrl-C로 끊거나 ssh가 떨어져도 설치는 계속 돈다. 다시 붙어서 같은 tail로
# 이어 보면 된다. 그냥 `sudo bash bootstrap_instance.sh`로 돌려도 되지만, 그 경우
# 연결이 끊기면 apt가 중간에 죽어 dpkg가 깨진 상태로 남을 수 있다.

if [ "$(id -u)" -ne 0 ]; then
  echo "root로 실행한다: sudo bash $0" >&2
  exit 1
fi

ARCH=$(uname -m)
echo "arch: $ARCH"

# ── apt 환경 ──────────────────────────────────────────────────────────────
# NEEDRESTART_SUSPEND: Ubuntu 22.04+는 패키지 설치 후 needrestart가 영향받는
#   서비스를 자동 재시작한다. 여기에 ssh·amazon-ssm-agent가 걸리면 지금 붙어 있는
#   세션이 그대로 끊긴다(2026-09-15에 실제로 겪음). 재시작이 필요하면 배포 창에서
#   직접 하거나 인스턴스를 재부팅한다 — 설치 도중에 끊기는 것보다 낫다.
# DEBIAN_FRONTEND: 설정 파일 충돌 프롬프트로 멈추지 않게 한다.
# Lock::Timeout: 부팅 직후 unattended-upgrades가 잡고 있는 락을 기다린다
#   (기본값은 즉시 실패라, 띄우자마자 돌리면 그냥 죽는다).
export NEEDRESTART_SUSPEND=1
export DEBIAN_FRONTEND=noninteractive
APT_OPTS=(-y -o DPkg::Lock::Timeout=300)

# ── docker ───────────────────────────────────────────────────────────────
# Ubuntu 기본 저장소의 docker.io로 충분하다 — 이 배포는 pull/run/login만 쓰고
# compose plugin을 쓰지 않는다. 최신 엔진이 필요해지면 그때 공식 docker-ce 저장소를 붙인다.
apt update "${APT_OPTS[@]}"
apt install "${APT_OPTS[@]}" docker.io unzip curl

systemctl enable --now docker

# ssh로 들어와 직접 docker를 칠 때용. 재로그인해야 반영된다.
# CodeDeploy 훅은 runas: root라 이 그룹과 무관하게 동작한다.
usermod -aG docker ubuntu || true

# ── aws CLI v2 ───────────────────────────────────────────────────────────
# apt의 awscli 패키지는 v1이라 쓰지 않는다. `ecr get-login-password`가 v2 전제다.
if ! command -v aws >/dev/null 2>&1; then
  case "$ARCH" in
    aarch64) AWS_ZIP=awscli-exe-linux-aarch64.zip ;;
    x86_64)  AWS_ZIP=awscli-exe-linux-x86_64.zip ;;
    *) echo "모르는 아키텍처: $ARCH" >&2; exit 1 ;;
  esac
  curl -fsSL "https://awscli.amazonaws.com/$AWS_ZIP" -o /tmp/awscliv2.zip
  unzip -q /tmp/awscliv2.zip -d /tmp
  /tmp/aws/install --bin-dir /usr/local/bin --install-dir /usr/local/aws-cli
  rm -rf /tmp/awscliv2.zip /tmp/aws
fi

# ── 확인 ─────────────────────────────────────────────────────────────────
echo "── 설치 확인 ─────────────────────────────"
docker --version
aws --version

# 인스턴스 역할(lovebug-ec2-fastapi)이 붙어 있는지. Unable to locate credentials가
# 나오면 인스턴스 프로파일이 안 붙은 것이라 배포는 어차피 실패한다.
aws sts get-caller-identity

# ECR 로그인까지 실제로 통과하는지. denied가 나오면 인스턴스 역할의 ECR 권한
# (lovebug/fastapi 리포 ARN) 문제로 넘어간다.
echo "── ECR 로그인 확인 ───────────────────────"
aws ecr get-login-password --region ap-northeast-2 \
  | docker login --username AWS --password-stdin 416121583617.dkr.ecr.ap-northeast-2.amazonaws.com

# needrestart를 껐으므로 재시작이 밀린 서비스가 있을 수 있다. 목록만 알려주고
# 여기서 건드리지는 않는다 — 이 스크립트가 세션을 끊지 않는 것이 우선이다.
if command -v needrestart >/dev/null 2>&1; then
  echo "── 재시작이 밀린 서비스(있다면) ──────────"
  needrestart -b -r l 2>/dev/null | grep -i '^NEEDRESTART-SVC' || echo "없음"
fi

echo "완료 — 이제 GitHub Actions에서 ubidict-py Deploy를 재실행한다."
