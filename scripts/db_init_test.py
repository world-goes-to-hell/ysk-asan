"""
통합 테스트용 스키마(ysk_asan_test) 준비 + ysk 계정 권한 확인.
application-local.yml 에서 접속 정보를 읽어 터널(localhost:3306)로 붙는다.
"""
import os
import re
import sys

import pymysql

try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:  # noqa: BLE001
    pass

text = open("src/main/resources/application-local.yml", encoding="utf-8").read()


def cfg(field):
    m = re.search(rf"^\s*{field}:\s*(.+?)\s*$", text, re.MULTILINE)
    raw = m.group(1).strip()
    mm = re.match(r"\$\{([^:}]+):?([^}]*)\}", raw)
    return os.environ.get(mm.group(1), mm.group(2)) if mm else raw


user = cfg("username")
pw = cfg("password")

try:
    conn = pymysql.connect(host="127.0.0.1", port=3306, user=user, password=pw, connect_timeout=8)
except Exception as e:  # noqa: BLE001
    print(f"[FAIL] 연결 실패(터널 확인): {type(e).__name__}: {e}")
    sys.exit(1)

with conn.cursor() as cur:
    cur.execute("SELECT VERSION()")
    print("[OK] 연결됨, MariaDB", cur.fetchone()[0])

    cur.execute("SHOW GRANTS")
    print("ysk 권한:")
    for r in cur.fetchall():
        print("   ", r[0])

    try:
        cur.execute(
            "CREATE DATABASE IF NOT EXISTS ysk_asan_test "
            "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
        )
        print("[OK] ysk_asan_test 스키마 준비 완료")
        schema_ready = True
    except Exception as e:  # noqa: BLE001
        print(f"[FAIL] 스키마 생성 권한 없음: {e}")
        schema_ready = False

    cur.execute("SHOW DATABASES")
    dbs = [r[0] for r in cur.fetchall()]
    print("접근 가능한 DB:", dbs)

conn.close()
print("RESULT:", "READY" if schema_ready and "ysk_asan_test" in dbs else "NEED_ROOT")
