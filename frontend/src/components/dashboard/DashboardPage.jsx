import { Link } from 'react-router-dom';

import styles from '../../styles/dashboard.module.css';

export default function DashboardPage() {
  return (
    <div className={styles.page}>
      <h2 className={styles.heading}>대시보드</h2>
      <div className={styles.placeholder}>
        <p className={styles.placeholderTitle}>준비 중입니다</p>
        <p className={styles.placeholderDesc}>
          현재 버전에서는 연락처 관리 기능을 제공합니다.
        </p>
        <Link to="/contacts" className="btn btn-primary">
          연락처 관리로 이동
        </Link>
      </div>
    </div>
  );
}
