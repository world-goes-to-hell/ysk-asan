#!/usr/bin/env bash
# ============================================================
# 로컬 개발용 SSH 터널: 개발서버 MariaDB -> 로컬 3306  (bash)
# ------------------------------------------------------------
# 사용법:
#   1) cp scripts/db-tunnel.example.sh scripts/db-tunnel.sh  (db-tunnel.sh 는 .gitignore)
#   2) 아래 값을 실제 개발서버 정보로 변경
#   3) 실행:  bash scripts/db-tunnel.sh
#   4) 이 창을 열어둔 채로 WAS(local profile) 실행 -> localhost:3306 이 원격 DB
# ============================================================
set -euo pipefail

SSH_HOST="dev-server.example.com"   # 개발서버 IP/도메인
SSH_USER="your-ssh-user"            # SSH 사용자명
SSH_PORT=22                          # SSH 포트
LOCAL_PORT=3306                      # 로컬에서 WAS 가 붙을 포트
REMOTE_DB_HOST="127.0.0.1"           # 서버 입장에서 본 MariaDB 주소
REMOTE_DB_PORT=3306                  # MariaDB 포트

echo "SSH 터널 시작: localhost:${LOCAL_PORT} -> ${REMOTE_DB_HOST}:${REMOTE_DB_PORT} (via ${SSH_USER}@${SSH_HOST})"

# 개인키 사용 시 -i 옵션 추가:  ssh -i ~/.ssh/your_key -N -L ...
ssh -N -o ConnectTimeout=10 -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -L "${LOCAL_PORT}:${REMOTE_DB_HOST}:${REMOTE_DB_PORT}" -p "${SSH_PORT}" "${SSH_USER}@${SSH_HOST}"
