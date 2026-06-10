import { useEffect, useRef } from 'react';

import {
  allVisibleSelected,
  deselectVisible,
  selectVisible,
  someVisibleSelected,
  toggle,
} from '../../utils/selection';
import ContactRow from './ContactRow';
import styles from '../../styles/contacts.module.css';

export default function ContactTable({
  contacts,
  loading,
  selectedIds,
  setSelectedIds,
  visibleIds,
  onUpdate,
  onRequestDelete,
}) {
  const masterRef = useRef(null);
  const allSelected = allVisibleSelected(selectedIds, visibleIds);
  const someSelected = someVisibleSelected(selectedIds, visibleIds);

  useEffect(() => {
    if (masterRef.current) masterRef.current.indeterminate = someSelected;
  }, [someSelected]);

  const onMaster = (e) => {
    setSelectedIds(
      e.target.checked
        ? selectVisible(selectedIds, visibleIds)
        : deselectVisible(selectedIds, visibleIds)
    );
  };

  if (loading) {
    return <div className={styles.empty}>불러오는 중…</div>;
  }
  if (contacts.length === 0) {
    return <div className={styles.empty}>표시할 연락처가 없습니다.</div>;
  }

  return (
    <div className={styles.tableWrap}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th className={styles.checkCol}>
              <input
                ref={masterRef}
                type="checkbox"
                checked={allSelected}
                onChange={onMaster}
                aria-label="보이는 행 전체 선택"
              />
            </th>
            <th>부서</th>
            <th>이름</th>
            <th>이메일</th>
            <th className={styles.actionCol} aria-label="작업" />
          </tr>
        </thead>
        <tbody>
          {contacts.map((c) => (
            <ContactRow
              key={c.id}
              contact={c}
              selected={selectedIds.has(c.id)}
              onToggle={() => setSelectedIds(toggle(selectedIds, c.id))}
              onUpdate={onUpdate}
              onRequestDelete={onRequestDelete}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
}
