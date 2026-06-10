import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { useAuth } from '../../contexts/AuthContext';
import styles from '../../styles/auth.module.css';

export default function LoginForm() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    if (!username.trim() || !password) {
      setError('사용자명과 비밀번호를 입력하세요.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      await login(username.trim(), password, rememberMe);
      navigate('/', { replace: true });
    } catch (err) {
      setError(
        err.status === 401
          ? '사용자명 또는 비밀번호가 올바르지 않습니다.'
          : err.message
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className={styles.form} onSubmit={onSubmit}>
      <div>
        <label className="form-label" htmlFor="login-username">사용자명</label>
        <input
          id="login-username"
          className="form-input"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoComplete="username"
          autoFocus
        />
      </div>
      <div>
        <label className="form-label" htmlFor="login-password">비밀번호</label>
        <input
          id="login-password"
          type="password"
          className="form-input"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
        />
      </div>
      <label className={styles.rememberRow}>
        <input
          type="checkbox"
          checked={rememberMe}
          onChange={(e) => setRememberMe(e.target.checked)}
        />
        로그인 상태 유지
      </label>
      {error && <p className={styles.error}>{error}</p>}
      <button type="submit" className="btn btn-primary btn-full" disabled={submitting}>
        {submitting ? '로그인 중…' : '로그인'}
      </button>
    </form>
  );
}
