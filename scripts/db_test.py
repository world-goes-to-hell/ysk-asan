"""
DB 연결 테스트 — application-local.yml 의 설정 그대로 localhost(SSH 터널) 로 접속해
MariaDB 버전 / 현재 DB / 계정 / 테이블을 확인한다.
비밀번호는 코드에 박지 않고 application-local.yml(.gitignore) 에서 읽는다.

사용:  먼저 scripts/db-tunnel.ps1 로 터널을 연 뒤 →  python scripts/db_test.py
"""
import os
import re
import sys

import pymysql

# Windows cp949 콘솔에서 한글/기호 출력이 깨지거나 죽지 않도록 UTF-8 강제
try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:  # noqa: BLE001
    pass

YML = "src/main/resources/application-local.yml"

try:
    text = open(YML, encoding="utf-8").read()
except FileNotFoundError:
    print(f"❌ {YML} 가 없습니다. application-local.yml.example 을 복사해 만드세요.")
    sys.exit(1)


def line_value(field):
    m = re.search(rf"^\s*{field}:\s*(.+?)\s*$", text, re.MULTILINE)
    return m.group(1).strip() if m else None


def resolve(raw):
    """${ENV:default} -> 환경변수 우선, 없으면 default. 평문이면 그대로."""
    if raw is None:
        return None
    m = re.match(r"\$\{([^:}]+):?([^}]*)\}", raw)
    if m:
        return os.environ.get(m.group(1), m.group(2))
    return raw


url = line_value("url") or ""
m = re.search(r"//([^:/]+):(\d+)/(\w+)", url)
if not m:
    print(f"❌ datasource url 파싱 실패: {url!r}")
    sys.exit(1)
host, port, db = m.group(1), int(m.group(2)), m.group(3)
if host == "localhost":
    host = "127.0.0.1"  # 터널은 127.0.0.1 에 바인딩됨 (IPv6 localhost 회피)

user = resolve(line_value("username"))
pw = resolve(line_value("password"))

print(f"접속 시도: {user}@{host}:{port}/{db}")
try:
    conn = pymysql.connect(
        host=host, port=port, user=user, password=pw, database=db, connect_timeout=8
    )
except Exception as e:  # noqa: BLE001
    print(f"[FAIL] 연결 실패: {type(e).__name__}: {e}")
    print("   - 터널이 열려 있는지(scripts/db-tunnel.ps1), 포트/계정/비번/DB명을 확인하세요.")
    sys.exit(1)

try:
    with conn.cursor() as cur:
        cur.execute("SELECT VERSION()")
        print("MariaDB 버전 :", cur.fetchone()[0])
        cur.execute("SELECT DATABASE(), CURRENT_USER()")
        cur_db, cur_user = cur.fetchone()
        print("현재 DB/계정 :", cur_db, "/", cur_user)
        cur.execute("SHOW TABLES")
        tables = [t[0] for t in cur.fetchall()]
        print(f"테이블({len(tables)}):", tables[:30])
    print("✅ 연결 성공")
finally:
    conn.close()
