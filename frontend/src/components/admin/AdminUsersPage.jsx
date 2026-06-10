import { useCallback, useEffect, useState } from 'react';

import adminAPI from '../../api/admin';
import { useAuth } from '../../contexts/AuthContext';
import { useToast } from '../../hooks/useToast';
import ConfirmDialog from '../common/ConfirmDialog';
import styles from '../../styles/admin.module.css';

function StatusBadge({ user }) {
  if (user.locked) {
    return (
      <span className={`${styles.badge} ${styles.badgeLocked}`}>
        잠김 · 실패 {user.failedLoginAttempts}회
      </span>
    );
  }
  if (!user.approved) {
    return <span className={`${styles.badge} ${styles.badgePending}`}>승인 대기</span>;
  }
  return <span className={`${styles.badge} ${styles.badgeActive}`}>정상</span>;
}

export default function AdminUsersPage() {
  const { currentUser } = useAuth();
  const showToast = useToast();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [roleTarget, setRoleTarget] = useState(null); // 권한 변경 확인 대상

  const load = useCallback(async () => {
    try {
      setUsers(await adminAPI.listUsers());
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    load();
  }, [load]);

  // 액션 공통 처리: 서버 응답(변경된 회원)으로 해당 행만 교체(불변 갱신).
  const run = async (action, successMessage) => {
    try {
      const updated = await action();
      setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
      showToast(successMessage, 'success');
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  const onApprove = (user) =>
    run(() => adminAPI.approve(user.id), `${user.username} 님을 승인했습니다.`);

  const onUnlock = (user) =>
    run(() => adminAPI.unlock(user.id), `${user.username} 님의 잠금을 해제했습니다.`);

  const onConfirmRoleChange = () => {
    const { user, nextRole } = roleTarget;
    setRoleTarget(null);
    run(
      () => adminAPI.changeRole(user.id, nextRole),
      `${user.username} 님의 권한을 ${nextRole === 'ADMIN' ? '관리자' : '일반'}로 변경했습니다.`
    );
  };

  return (
    <div className={styles.page}>
      <div className={styles.headerRow}>
        <h1 className={styles.heading}>회원 관리</h1>
        <span className={styles.hint}>승인 대기 회원은 로그인할 수 없습니다.</span>
      </div>

      <div className={styles.tableWrap}>
        {loading ? (
          <p className={styles.empty}>불러오는 중…</p>
        ) : users.length === 0 ? (
          <p className={styles.empty}>회원이 없습니다.</p>
        ) : (
          <table className={styles.table}>
            <thead>
              <tr>
                <th>사용자명</th>
                <th>권한</th>
                <th>상태</th>
                <th>가입일</th>
                <th className={styles.actionCol}>관리</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => {
                const isMe = user.username === currentUser?.username;
                return (
                  <tr key={user.id}>
                    <td>
                      {user.username}
                      {isMe && <span className={styles.meTag}>(나)</span>}
                    </td>
                    <td className={user.role === 'ADMIN' ? styles.roleAdmin : undefined}>
                      {user.role === 'ADMIN' ? '관리자' : '일반'}
                    </td>
                    <td><StatusBadge user={user} /></td>
                    <td>{user.createdAt?.slice(0, 10)}</td>
                    <td className={styles.actionCol}>
                      <div className={styles.rowActions}>
                        {!user.approved && (
                          <button type="button" className="btn btn-primary btn-sm"
                                  onClick={() => onApprove(user)}>
                            승인
                          </button>
                        )}
                        {user.locked && (
                          <button type="button" className="btn btn-ghost btn-sm"
                                  onClick={() => onUnlock(user)}>
                            잠금 해제
                          </button>
                        )}
                        {!isMe && (
                          <button type="button" className="btn btn-ghost btn-sm"
                                  onClick={() => setRoleTarget({
                                    user,
                                    nextRole: user.role === 'ADMIN' ? 'USER' : 'ADMIN',
                                  })}>
                            {user.role === 'ADMIN' ? '일반으로' : '관리자로'}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      {roleTarget && (
        <ConfirmDialog
          message={`${roleTarget.user.username} 님의 권한을 ${
            roleTarget.nextRole === 'ADMIN' ? '관리자' : '일반'
          }(으)로 변경할까요?`}
          confirmLabel="변경"
          danger={false}
          onConfirm={onConfirmRoleChange}
          onCancel={() => setRoleTarget(null)}
        />
      )}
    </div>
  );
}
