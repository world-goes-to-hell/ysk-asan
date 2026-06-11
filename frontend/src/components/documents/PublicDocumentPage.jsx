import { useRef, useState } from 'react';
import { useParams } from 'react-router-dom';

import documentsAPI from '../../api/documents';
import { findTemplate } from '../../documents/templates';
import modal from '../../styles/modal.module.css';
import auth from '../../styles/auth.module.css';
import styles from '../../styles/documents.module.css';

/**
 * 6~8차: 공개 공문 뷰어(/d/:token — 로그인 불필요).
 * 비밀번호 게이트 통과 후 공문을 렌더하고, 직인 이미지를 업로드하면 (인) 영역에 합성된다.
 * 비밀번호는 서버에 상태를 만들지 않으므로 메모리에 들고 열람/직인 요청마다 동봉한다.
 */
export default function PublicDocumentPage() {
  const { token } = useParams();
  const fileInputRef = useRef(null);

  const [password, setPassword] = useState('');
  const [doc, setDoc] = useState(null); // null = 게이트, {templateId, fields, seal} = 열람
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const load = async (pw) => {
    const res = await documentsAPI.view(token, pw);
    setDoc({
      templateId: res.templateId,
      fields: res.fields,
      seal: res.sealImageBase64
        ? { base64: res.sealImageBase64, contentType: res.sealContentType }
        : null,
    });
  };

  const onSubmitPassword = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await load(password);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const onSealFile = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    setBusy(true);
    setError('');
    try {
      await documentsAPI.attachSeal(token, password, file);
      await load(password); // 날인 결과 재조회 → (인) 위에 직인 표시
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  // 6차: 비밀번호 게이트
  if (!doc) {
    return (
      <div className={styles.viewerPage}>
        <div className={modal.dialog} role="dialog" aria-label="공문 열람 비밀번호">
          <form className={auth.form} onSubmit={onSubmitPassword}>
            <p className={modal.message}>
              공문 열람 비밀번호를 입력하세요.{'\n'}(발급자에게 전달받은 비밀번호)
            </p>
            <input
              type="password"
              className="form-input"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              aria-label="열람 비밀번호"
              required
              autoFocus
            />
            {error && <p className={auth.error}>{error}</p>}
            <button type="submit" className="btn btn-primary btn-full" disabled={busy}>
              {busy ? '확인 중…' : '열람'}
            </button>
          </form>
        </div>
      </div>
    );
  }

  // 7~8차: 공문 + 직인
  const template = findTemplate(doc.templateId);
  if (!template) {
    return <div className={styles.viewerPage}>알 수 없는 양식입니다.</div>;
  }

  return (
    <div className={styles.viewerPage}>
      <div className={styles.viewerActions}>
        {!doc.seal && (
          <button type="button" className="btn btn-primary"
                  onClick={() => fileInputRef.current?.click()} disabled={busy}>
            {busy ? '처리 중…' : '직인 삽입'}
          </button>
        )}
        <button type="button" className="btn btn-ghost" onClick={() => window.print()}>
          인쇄
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/png,image/jpeg"
          style={{ display: 'none' }}
          onChange={onSealFile}
          aria-label="직인 이미지 선택"
        />
      </div>
      {error && <p className={auth.error}>{error}</p>}

      <template.View fields={doc.fields} seal={doc.seal} />
    </div>
  );
}
