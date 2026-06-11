import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import documentsAPI from '../../api/documents';
import { TEMPLATES, findTemplate } from '../../documents/templates';
import styles from '../../styles/documents.module.css';

/** 1차: 공문 양식 갤러리 + 내가 발급한 공문 목록(직인 상태·열람/인쇄 진입). */
export default function DocumentsPage() {
  const navigate = useNavigate();
  const [issued, setIssued] = useState([]);

  useEffect(() => {
    documentsAPI.mine().then(setIssued).catch(() => setIssued([]));
  }, []);

  return (
    <div className={styles.page}>
      <div className={styles.headerRow}>
        <h2 className={styles.heading}>공문 관리</h2>
        <span className={styles.hint}>양식을 선택해 내용을 입력하고 직인 URL을 발급하세요.</span>
      </div>

      <div className={styles.gallery}>
        {TEMPLATES.map((template) => (
          <button
            key={template.id}
            type="button"
            className={styles.card}
            onClick={() => navigate(`/documents/${template.id}`)}
          >
            <div className={styles.thumbWrap} aria-hidden="true">
              <div className={styles.thumbScale}>
                <template.View fields={template.sample} />
              </div>
            </div>
            <div className={styles.cardBody}>
              <div className={styles.cardTitle}>{template.title}</div>
              <div className={styles.cardSummary}>{template.summary}</div>
            </div>
          </button>
        ))}
      </div>

      <h3 className={styles.sectionTitle}>발급한 공문</h3>
      {issued.length === 0 ? (
        <p className={styles.emptyIssued}>발급한 공문이 없습니다.</p>
      ) : (
        <div className={styles.issuedWrap}>
          <table className={styles.issuedTable}>
            <thead>
              <tr>
                <th>양식</th>
                <th>발급일</th>
                <th>직인</th>
                <th aria-label="작업" />
              </tr>
            </thead>
            <tbody>
              {issued.map((d) => (
                <tr key={d.token}>
                  <td>{findTemplate(d.templateId)?.title || d.templateId}</td>
                  <td>{d.createdAt?.slice(0, 10)}</td>
                  <td>
                    <span className={d.sealed ? styles.badgeSealed : styles.badgePending}>
                      {d.sealed ? '날인 완료' : '날인 대기'}
                    </span>
                  </td>
                  <td className={styles.issuedAction}>
                    <button type="button" className="btn btn-ghost btn-sm"
                            onClick={() => navigate(`/documents/issued/${d.token}`)}>
                      보기·인쇄
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
