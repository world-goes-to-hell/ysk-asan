import styles from '../../styles/contacts.module.css';

export default function DepartmentTabs({ departments, active, onSelect }) {
  // null = 전체. 서버가 정렬해 주므로 재정렬하지 않는다.
  const tabs = [null, ...departments];

  return (
    <div className={styles.tabs} role="tablist">
      {tabs.map((dept) => (
        <button
          key={dept ?? '__all__'}
          type="button"
          role="tab"
          aria-selected={active === dept}
          className={`${styles.tab} ${active === dept ? styles.tabActive : ''}`}
          onClick={() => onSelect(dept)}
        >
          {dept ?? '전체'}
        </button>
      ))}
    </div>
  );
}
