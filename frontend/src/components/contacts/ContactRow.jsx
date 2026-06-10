import { useState } from 'react';

import styles from '../../styles/contacts.module.css';

const fmtDate = (iso) => (iso ? iso.slice(0, 10) : '');

/** "수정자 · 날짜" 요약. 변경 이력 도입 전 데이터(작성자 null)는 '-'. */
function auditSummary(contact) {
  const who = contact.updatedBy || contact.createdBy;
  if (!who) return '-';
  return `${who} · ${fmtDate(contact.updatedAt)}`;
}

function auditTooltip(contact) {
  const created = `등록: ${contact.createdBy || '알 수 없음'} (${fmtDate(contact.createdAt)})`;
  const updated = `수정: ${contact.updatedBy || '알 수 없음'} (${fmtDate(contact.updatedAt)})`;
  return `${created}\n${updated}`;
}

export default function ContactRow({ contact, selected, onToggle, onUpdate, onRequestDelete }) {
  const [editing, setEditing] = useState(false);
  const [department, setDepartment] = useState(contact.department);
  const [name, setName] = useState(contact.name);
  const [email, setEmail] = useState(contact.email);
  const [saving, setSaving] = useState(false);

  const startEdit = () => {
    setDepartment(contact.department);
    setName(contact.name);
    setEmail(contact.email);
    setEditing(true);
  };

  const save = async () => {
    if (!department.trim() || !name.trim() || !email.trim()) return;
    setSaving(true);
    try {
      await onUpdate(contact.id, {
        department: department.trim(),
        name: name.trim(),
        email: email.trim(),
      });
      setEditing(false);
    } catch (_) {
      // 토스트는 상위에서 처리, 편집 모드 유지
    } finally {
      setSaving(false);
    }
  };

  if (editing) {
    return (
      <tr className={styles.rowEditing}>
        <td className={styles.checkCol} />
        <td>
          <input className="form-input" value={department} onChange={(e) => setDepartment(e.target.value)} aria-label="부서 수정" />
        </td>
        <td>
          <input className="form-input" value={name} onChange={(e) => setName(e.target.value)} aria-label="이름 수정" />
        </td>
        <td>
          <input className="form-input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} aria-label="이메일 수정" />
        </td>
        <td className={styles.auditCol} />
        <td className={styles.actionCol}>
          <div className={styles.rowActions}>
            <button type="button" className="btn btn-primary btn-sm" onClick={save} disabled={saving}>
              저장
            </button>
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => setEditing(false)}>
              취소
            </button>
          </div>
        </td>
      </tr>
    );
  }

  return (
    <tr className={selected ? styles.rowSelected : undefined}>
      <td className={styles.checkCol}>
        <input type="checkbox" checked={selected} onChange={onToggle} aria-label={`${contact.name} 선택`} />
      </td>
      <td>{contact.department}</td>
      <td>{contact.name}</td>
      <td className={styles.emailCell}>{contact.email}</td>
      <td className={styles.auditCol} title={auditTooltip(contact)}>
        {auditSummary(contact)}
      </td>
      <td className={styles.actionCol}>
        <div className={styles.rowActions}>
          <button type="button" className="btn btn-ghost btn-sm" onClick={startEdit}>
            수정
          </button>
          <button
            type="button"
            className={`btn btn-ghost btn-sm ${styles.deleteBtn}`}
            onClick={() => onRequestDelete(contact.id)}
          >
            삭제
          </button>
        </div>
      </td>
    </tr>
  );
}
