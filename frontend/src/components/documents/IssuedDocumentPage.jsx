import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import documentsAPI from '../../api/documents';
import { findTemplate } from '../../documents/templates';
import auth from '../../styles/auth.module.css';
import styles from '../../styles/documents.module.css';

/**
 * 발급자 전용 뷰어(로그인 + 본인 발급 문서). 비밀번호 없이 열람하고 직인 찍힌 공문을 인쇄한다.
 * AppLayout 밖 전체 화면 — 인쇄 시 공문 용지만 출력된다.
 */
export default function IssuedDocumentPage() {
  const { token } = useParams();
  const navigate = useNavigate();
  const [doc, setDoc] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    documentsAPI
      .issuerView(token)
      .then((res) =>
        setDoc({
          templateId: res.templateId,
          fields: res.fields,
          seal: res.sealImageBase64
            ? { base64: res.sealImageBase64, contentType: res.sealContentType }
            : null,
        })
      )
      .catch((err) => setError(err.message));
  }, [token]);

  if (error) {
    return (
      <div className={styles.viewerPage}>
        <p className={auth.error}>{error}</p>
        <button type="button" className="btn btn-ghost" onClick={() => navigate('/documents')}>
          목록으로
        </button>
      </div>
    );
  }
  if (!doc) {
    return <div className={styles.viewerPage}>불러오는 중…</div>;
  }

  const template = findTemplate(doc.templateId);
  if (!template) {
    return <div className={styles.viewerPage}>알 수 없는 양식입니다.</div>;
  }

  return (
    <div className={styles.viewerPage}>
      <div className={styles.viewerActions}>
        <button type="button" className="btn btn-primary" onClick={() => window.print()}>
          인쇄
        </button>
        <button type="button" className="btn btn-ghost" onClick={() => navigate('/documents')}>
          목록으로
        </button>
      </div>
      {!doc.seal && <p className={styles.pendingNotice}>아직 직인이 날인되지 않았습니다.</p>}

      <template.View fields={doc.fields} seal={doc.seal} />
    </div>
  );
}
