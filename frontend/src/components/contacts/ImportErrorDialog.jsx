import { useEffect } from 'react';

import modal from '../../styles/modal.module.css';

const MAX_SHOWN = 10;

/** CSV 가져오기 실패(all-or-nothing) 시 행별 오류 목록을 보여준다. */
export default function ImportErrorDialog({ message, errors = [], onClose }) {
  useEffect(() => {
    const onKey = (e) => {
      if (e.key === 'Escape') onClose?.();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  const shown = errors.slice(0, MAX_SHOWN);
  const hidden = errors.length - shown.length;

  return (
    <div className={modal.overlay} onClick={onClose}>
      <div
        className={modal.dialog}
        role="dialog"
        aria-modal="true"
        aria-label="CSV 가져오기 오류"
        onClick={(e) => e.stopPropagation()}
      >
        <p className={modal.message}>{message}</p>
        {shown.length > 0 && (
          <ul style={{ margin: '0 0 16px', paddingLeft: 18, fontSize: '0.85rem', lineHeight: 1.7 }}>
            {shown.map((err) => (
              <li key={err.row}>
                {err.row}행: {err.message}
              </li>
            ))}
            {hidden > 0 && <li>… 외 {hidden}건</li>}
          </ul>
        )}
        <div className={modal.actions}>
          <button type="button" className="btn btn-primary" onClick={onClose} autoFocus>
            확인
          </button>
        </div>
      </div>
    </div>
  );
}
