import { useEffect, useState } from 'react';

import authAPI from '../../api/auth';
import { useToast } from '../../hooks/useToast';
import modal from '../../styles/modal.module.css';
import styles from '../../styles/auth.module.css';

export default function PasswordChangeDialog({ onClose }) {
  const showToast = useToast();
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const onKey = (e) => {
      if (e.key === 'Escape') onClose?.();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  const onSubmit = async (e) => {
    e.preventDefault();
    if (next.length < 8) {
      setError('새 비밀번호는 8자 이상이어야 합니다.');
      return;
    }
    if (next !== confirm) {
      setError('새 비밀번호가 서로 일치하지 않습니다.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      await authAPI.changePassword(current, next);
      // 서버 변경 즉시 기존 로그인 유지(remember-me) 쿠키는 전부 무효화된다(세션은 유지).
      showToast('비밀번호가 변경되었습니다.', 'success');
      onClose?.();
    } catch (err) {
      setError(err.message); // 서버 메시지(현재 비밀번호 불일치 등) 그대로 표시
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={modal.overlay} onClick={onClose}>
      <div
        className={modal.dialog}
        role="dialog"
        aria-modal="true"
        aria-label="비밀번호 변경"
        onClick={(e) => e.stopPropagation()}
      >
        <form className={styles.form} onSubmit={onSubmit}>
          <div>
            <label className="form-label" htmlFor="pw-current">현재 비밀번호</label>
            <input
              id="pw-current"
              type="password"
              className="form-input"
              value={current}
              onChange={(e) => setCurrent(e.target.value)}
              autoComplete="current-password"
              required
              autoFocus
            />
          </div>
          <div>
            <label className="form-label" htmlFor="pw-new">새 비밀번호</label>
            <input
              id="pw-new"
              type="password"
              className="form-input"
              value={next}
              onChange={(e) => setNext(e.target.value)}
              autoComplete="new-password"
              placeholder="8~72자"
              required
              minLength={8}
              maxLength={72}
            />
          </div>
          <div>
            <label className="form-label" htmlFor="pw-confirm">새 비밀번호 확인</label>
            <input
              id="pw-confirm"
              type="password"
              className="form-input"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              autoComplete="new-password"
              required
              minLength={8}
              maxLength={72}
            />
          </div>
          {error && <p className={styles.error}>{error}</p>}
          <div className={modal.actions}>
            <button type="button" className="btn btn-ghost" onClick={onClose}>
              취소
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? '변경 중…' : '변경'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
