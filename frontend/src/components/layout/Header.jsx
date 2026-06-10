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
      <div className={styles.brand}>수신처 관리</div>
      <div className={styles.headerRight}>
        {currentUser && <span className={styles.user}>{currentUser.username}</span>}
        <button type="button" className="btn btn-ghost btn-sm" onClick={onLogout}>
          로그아웃
        </button>
      </div>
    </header>
  );
}
