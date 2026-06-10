import styles from '../../styles/contacts.module.css';

export default function SelectionBar({ count, onCopy, onClear, onDelete }) {
  return (
    <div className={styles.selectionBar}>
      <span className={styles.selectionCount}>{count}명 선택됨</span>
      <div className={styles.selectionActions}>
        <button type="button" className="btn btn-ghost btn-sm" onClick={onClear}>
          선택 해제
        </button>
        <button type="button" className="btn btn-ghost btn-sm" onClick={onCopy}>
          이메일 복사
        </button>
        <button type="button" className="btn btn-danger btn-sm" onClick={onDelete}>
          삭제
        </button>
      </div>
    </div>
  );
}
