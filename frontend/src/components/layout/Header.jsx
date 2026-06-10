import { useNavigate } from 'react-router-dom';

import { useAuth } from '../../contexts/AuthContext';
import styles from '../../styles/layout.module.css';

export default function Header() {
  const { currentUser, logout } = useAuth();
  const navigate = useNavigate();

  const onLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <header className={styles.header}>
      <div className={styles.brand}>
        <span className={styles.brandMark} aria-hidden="true">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <path d="M12 2.5 A9.5 9.5 0 0 1 12 21.5" stroke="#5fa828" strokeWidth="3.4" strokeLinecap="round" />
            <path d="M12 21.5 A9.5 9.5 0 0 1 12 2.5" stroke="#0067a8" strokeWidth="3.4" strokeLinecap="round" />
          </svg>
        </span>
        수신처 관리
      </div>
      <div className={styles.headerRight}>
        {currentUser && <span className={styles.user}>{currentUser.username}</span>}
        <button type="button" className="btn btn-ghost btn-sm" onClick={onLogout}>
          로그아웃
        </button>
      </div>
    </header>
  );
}
