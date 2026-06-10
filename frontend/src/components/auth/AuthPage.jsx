import { useState } from 'react';
import { Navigate } from 'react-router-dom';

import { useAuth } from '../../contexts/AuthContext';
import LoginForm from './LoginForm';
import RegisterForm from './RegisterForm';
import styles from '../../styles/auth.module.css';

export default function AuthPage() {
  const { currentUser, loading } = useAuth();
  const [tab, setTab] = useState('login');

  if (loading) return null;
  if (currentUser) return <Navigate to="/" replace />;

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <div className={styles.brand}>
          <h1 className={styles.title}>수신처 관리</h1>
          <p className={styles.subtitle}>사내 연락처를 한 곳에서</p>
        </div>

        <div className={styles.tabs} role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={tab === 'login'}
            className={`${styles.tab} ${tab === 'login' ? styles.tabActive : ''}`}
            onClick={() => setTab('login')}
          >
            로그인
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={tab === 'register'}
            className={`${styles.tab} ${tab === 'register' ? styles.tabActive : ''}`}
            onClick={() => setTab('register')}
          >
            회원가입
          </button>
        </div>

        {tab === 'login' ? (
          <LoginForm />
        ) : (
          <RegisterForm onSuccess={() => setTab('login')} />
        )}
      </div>
    </div>
  );
}
