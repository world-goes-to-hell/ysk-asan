import { useNavigate } from 'react-router-dom';

import { TEMPLATES } from '../../documents/templates';
import styles from '../../styles/documents.module.css';

/** 1차: 공문 양식 갤러리. 썸네일은 실제 양식의 축소 렌더(예시 값)라 항상 실양식과 일치한다. */
export default function DocumentsPage() {
  const navigate = useNavigate();

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
    </div>
  );
}
