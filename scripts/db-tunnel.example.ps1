# ============================================================
# 로컬 개발용 SSH 터널 템플릿 (Windows PowerShell)
# ------------------------------------------------------------
# !! 이 파일(.example)은 git 에 추적됩니다. 실제 값을 여기 적지 마세요.
# 사용법:
#   1) 이 파일을 scripts/db-tunnel.ps1 로 복사 (db-tunnel.ps1 은 .gitignore 처리됨)
#        copy scripts\db-tunnel.example.ps1 scripts\db-tunnel.ps1
#   2) db-tunnel.ps1 에서 아래 값을 실제 개발서버 정보로 변경
#   3) 실행:  .\scripts\db-tunnel.ps1
#   4) 이 창을 열어둔 채로 WAS(local profile) 실행 -> localhost:3306 이 원격 DB
# ============================================================

$SshHost      = "dev-server.example.com"   # 개발서버 IP/도메인
$SshUser      = "your-ssh-user"            # SSH 사용자명
$SshPort      = 22                          # SSH 포트
$LocalPort    = 3306                        # 로컬에서 WAS 가 붙을 포트
$RemoteDbHost = "127.0.0.1"                 # 서버 입장에서 본 MariaDB 주소
$RemoteDbPort = 3306                        # MariaDB 포트

Write-Host "SSH 터널 시작: localhost:$LocalPort -> $($RemoteDbHost):$RemoteDbPort (via $SshUser@$SshHost)"

# 개인키 사용 시 -i 옵션 추가:  ssh -i $env:USERPROFILE\.ssh\your_key -N -L ...
ssh -N -o ConnectTimeout=10 -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -L "${LocalPort}:${RemoteDbHost}:${RemoteDbPort}" -p $SshPort "$SshUser@$SshHost"
