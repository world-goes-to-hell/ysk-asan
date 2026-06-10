import { useState } from 'react';

import { useAuth } from '../../contexts/AuthContext';
import { useToast } from '../../hooks/useToast';
import styles from '../../styles/auth.module.css';

export default function RegisterForm({ onSuccess }) {
  const { register } = useAuth();
  const showToast = useToast();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    if (username.trim().length < 3) {
      setError('사용자명은 3자 이상이어야 합니다.');
      return;
    }
    if (password.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      await register(username.trim(), password);
      showToast('회원가입이 완료되었습니다. 로그인해 주세요.', 'success');
      onSuccess?.();
    } catch (err) {
      // 서버 메시지(중복 username 409, 검증 400 등)를 그대로 노출한다.
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className={styles.form} onSubmit={onSubmit}>
      <div>
        <label className="form-label" htmlFor="reg-username">사용자명</label>
        <input
          id="reg-username"
          className="form-input"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoComplete="username"
          placeholder="3~30자"
          autoFocus
        />
      </div>
      <div>
        <label className="form-label" htmlFor="reg-password">비밀번호</label>
        <input
          id="reg-password"
          type="password"
          className="form-input"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="new-password"
          placeholder="8~72자"
        />
      </div>
      {error && <p className={styles.error}>{error}</p>}
      <button type="submit" className="btn btn-primary btn-full" disabled={submitting}>
        {submitting ? '처리 중…' : '회원가입'}
      </button>
    </form>
  );
}
