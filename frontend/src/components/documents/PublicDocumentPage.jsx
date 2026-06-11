import { useRef, useState } from 'react';
import { useParams } from 'react-router-dom';

import documentsAPI from '../../api/documents';
import { findTemplate } from '../../documents/templates';
import modal from '../../styles/modal.module.css';
import auth from '../../styles/auth.module.css';
import styles from '../../styles/documents.module.css';

/**
 * 6~8차: 공개 공문 뷰어(/d/:token — 로그인 불필요).
 * 직인은 잘못 올릴 수 있으므로 선택 즉시 저장하지 않는다:
 * 선택 → 화면 미리보기만 → [직인 다시 선택]으로 교체 가능 → [저장]을 눌러야 서버에 확정되고
 * 발급자가 볼 수 있게 된다. 저장 후에도 다시 선택→저장으로 교체할 수 있다.
 */
export default function PublicDocumentPage() {
  const { token } = useParams();
  const fileInputRef = useRef(null);

  const [password, setPassword] = useState('');
  const [doc, setDoc] = useState(null); // null = 게이트
  const [pending, setPending] = useState(null); // 미저장 직인 {file, base64, contentType}
  const [notice, setNotice] = useState('');
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

  // 파일 선택 = 로컬 미리보기만(서버 전송 X). 다시 선택하면 교체.
  const onSealFile = (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      setPending({ file, base64: String(reader.result).split(',')[1], contentType: file.type });
      setNotice('');
      setError('');
    };
    reader.readAsDataURL(file);
  };

  // 저장 확정 — 이때부터 발급자가 볼 수 있다.
  const onSave = async () => {
    setBusy(true);
    setError('');
    try {
      await documentsAPI.attachSeal(token, password, pending.file);
      await load(password);
      setPending(null);
      setNotice('직인이 저장되었습니다. 발급자가 확인할 수 있습니다.');
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

  const template = findTemplate(doc.templateId);
  if (!template) {
    return <div className={styles.viewerPage}>알 수 없는 양식입니다.</div>;
  }

  // 미저장 직인이 있으면 그것을 우선 미리보기(저장된 직인 교체 시나리오 포함).
  const displaySeal = pending
    ? { base64: pending.base64, contentType: pending.contentType }
    : doc.seal;

  return (
    <div className={styles.viewerPage}>
      <div className={styles.viewerActions}>
        <button type="button" className="btn btn-primary"
                onClick={() => fileInputRef.current?.click()} disabled={busy}>
          {displaySeal ? '직인 다시 선택' : '직인 삽입'}
        </button>
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
      {pending && <p className={styles.pendingNotice}>미리보기 상태입니다 — 아래 [저장]을 눌러야 직인이 확정됩니다.</p>}
      {notice && <p className={styles.savedNotice}>{notice}</p>}
      {error && <p className={auth.error}>{error}</p>}

      <template.View fields={doc.fields} seal={displaySeal} />

      {pending && (
        <div className={styles.saveBar}>
          <button type="button" className="btn btn-ghost" onClick={() => setPending(null)} disabled={busy}>
            취소
          </button>
          <button type="button" className="btn btn-primary" onClick={onSave} disabled={busy}>
            {busy ? '저장 중…' : '저장'}
          </button>
        </div>
      )}
    </div>
  );
}
