import { useState } from 'react';
import { Navigate, useParams } from 'react-router-dom';

import documentsAPI from '../../api/documents';
import { findTemplate } from '../../documents/templates';
import { useToast } from '../../hooks/useToast';
import modal from '../../styles/modal.module.css';
import auth from '../../styles/auth.module.css';
import styles from '../../styles/documents.module.css';

/**
 * 2~5차: 양식 작성(좌 폼 / 우 실시간 미리보기) → 직인 URL 발급(비밀번호 설정) → URL 복사.
 */
export default function DocumentComposePage() {
  const { templateId } = useParams();
  const template = findTemplate(templateId);
  const showToast = useToast();

  const [fields, setFields] = useState({});
  const [dialog, setDialog] = useState(null); // null | 'password' | {url}
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (!template) return <Navigate to="/documents" replace />;

  const setField = (key, value) => setFields((prev) => ({ ...prev, [key]: value }));

  const openIssueDialog = () => {
    setPassword('');
    setConfirm('');
    setError('');
    setDialog('password');
  };

  const onIssue = async (e) => {
    e.preventDefault();
    if (password.length < 4) {
      setError('열람 비밀번호는 4자 이상이어야 합니다.');
      return;
    }
    if (password !== confirm) {
      setError('비밀번호가 서로 일치하지 않습니다.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      const { token } = await documentsAPI.issue(template.id, fields, password);
      setDialog({ url: `${window.location.origin}/d/${token}` });
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const copyUrl = async () => {
    try {
      await navigator.clipboard.writeText(dialog.url);
      showToast('URL을 복사했습니다. 비밀번호는 별도로 전달하세요.', 'success');
    } catch (_) {
      showToast('클립보드 복사에 실패했습니다. 직접 선택해 복사하세요.', 'error');
    }
  };

  return (
    <div className={styles.page}>
      <div className={styles.headerRow}>
        <h2 className={styles.heading}>{template.title}</h2>
        <span className={styles.hint}>입력 내용은 우측 미리보기에 즉시 반영됩니다.</span>
      </div>

      <div className={styles.composeLayout}>
        <div className={styles.formPanel}>
          {template.fields.map((f) => (
            <div key={f.key}>
              <label className="form-label" htmlFor={`doc-${f.key}`}>{f.label}</label>
              {f.multiline ? (
                <textarea
                  id={`doc-${f.key}`}
                  className="form-input"
                  rows={8}
                  placeholder={f.placeholder}
                  value={fields[f.key] || ''}
                  onChange={(e) => setField(f.key, e.target.value)}
                />
              ) : (
                <input
                  id={`doc-${f.key}`}
                  type="text"
                  className="form-input"
                  placeholder={f.placeholder}
                  value={fields[f.key] || ''}
                  onChange={(e) => setField(f.key, e.target.value)}
                />
              )}
            </div>
          ))}
          <button type="button" className="btn btn-primary btn-full" onClick={openIssueDialog}>
            직인 URL 발급
          </button>
        </div>

        <div className={styles.previewPanel}>
          <template.View fields={fields} />
        </div>
      </div>

      {dialog === 'password' && (
        <div className={modal.overlay} onClick={() => setDialog(null)}>
          <div className={modal.dialog} role="dialog" aria-modal="true" aria-label="열람 비밀번호 설정"
               onClick={(e) => e.stopPropagation()}>
            <form className={auth.form} onSubmit={onIssue}>
              <p className={modal.message}>
                URL 열람용 비밀번호를 설정하세요.{'\n'}직인 담당자에게 URL과 함께 전달됩니다.
              </p>
              <div>
                <label className="form-label" htmlFor="doc-pw">열람 비밀번호</label>
                <input id="doc-pw" type="password" className="form-input" value={password}
                       onChange={(e) => setPassword(e.target.value)} minLength={4} maxLength={72}
                       required autoFocus />
              </div>
              <div>
                <label className="form-label" htmlFor="doc-pw2">비밀번호 확인</label>
                <input id="doc-pw2" type="password" className="form-input" value={confirm}
                       onChange={(e) => setConfirm(e.target.value)} minLength={4} maxLength={72}
                       required />
              </div>
              {error && <p className={auth.error}>{error}</p>}
              <div className={modal.actions}>
                <button type="button" className="btn btn-ghost" onClick={() => setDialog(null)}>
                  취소
                </button>
                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? '발급 중…' : '발급'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {dialog && dialog.url && (
        <div className={modal.overlay} onClick={() => setDialog(null)}>
          <div className={modal.dialog} role="dialog" aria-modal="true" aria-label="직인 URL 발급 완료"
               onClick={(e) => e.stopPropagation()}>
            <p className={modal.message}>
              직인 URL이 발급되었습니다.{'\n'}URL과 비밀번호를 직인 담당자에게 전달하세요.
            </p>
            <code className={styles.issuedUrl}>{dialog.url}</code>
            <div className={modal.actions}>
              <button type="button" className="btn btn-ghost" onClick={() => setDialog(null)}>
                닫기
              </button>
              <button type="button" className="btn btn-primary" onClick={copyUrl}>
                URL 복사
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
