import { useEffect, useMemo, useState } from 'react';

import { useContacts } from '../../hooks/useContacts';
import { useToast } from '../../hooks/useToast';
import { buildRecipients } from '../../utils/recipients';
import ConfirmDialog from '../common/ConfirmDialog';
import ContactAddForm from './ContactAddForm';
import ContactTable from './ContactTable';
import DepartmentTabs from './DepartmentTabs';
import SelectionBar from './SelectionBar';
import styles from '../../styles/contacts.module.css';

export default function ContactsPage() {
  const [qInput, setQInput] = useState('');
  const [debouncedQ, setDebouncedQ] = useState('');
  const [pendingDelete, setPendingDelete] = useState(null); // 삭제 대기 id 배열
  const showToast = useToast();

  // 검색 디바운스 300ms (IME 안전)
  useEffect(() => {
    const t = setTimeout(() => setDebouncedQ(qInput.trim()), 300);
    return () => clearTimeout(t);
  }, [qInput]);

  const {
    activeDept,
    setActiveDept,
    visible,
    departments,
    contactsById,
    selectedIds,
    setSelectedIds,
    loading,
    addContact,
    updateContact,
    deleteByIds,
  } = useContacts(debouncedQ);

  const visibleIds = useMemo(() => visible.map((c) => c.id), [visible]);

  // 부서 탭 전환: 검색어는 초기화하되 선택(수신처 누적)은 유지.
  const onSelectDept = (dept) => {
    setActiveDept(dept);
    setQInput('');
  };

  const onAdd = async (data) => {
    const dup = [...contactsById.values()].some((c) => c.email === data.email);
    try {
      await addContact(data);
      showToast(
        dup ? '추가했습니다. (동일 이메일이 이미 있습니다)' : '연락처를 추가했습니다.',
        dup ? 'default' : 'success'
      );
    } catch (e) {
      showToast(e.message, 'error');
      throw e; // 폼이 입력을 비우지 않도록 전파
    }
  };

  const onUpdate = async (id, data) => {
    try {
      await updateContact(id, data);
      showToast('수정했습니다.', 'success');
    } catch (e) {
      showToast(e.message, 'error');
      throw e; // 행이 편집 모드를 유지하도록 전파
    }
  };

  const onCopyEmails = async () => {
    const text = buildRecipients(contactsById, selectedIds);
    if (!text) {
      showToast('복사할 이메일이 없습니다.', 'error');
      return;
    }
    try {
      await navigator.clipboard.writeText(text);
      showToast(`${text.split(';').length}명의 이메일을 복사했습니다.`, 'success');
    } catch (_) {
      showToast('클립보드 복사에 실패했습니다. 브라우저 권한을 확인하세요.', 'error');
    }
  };

  const onConfirmDelete = async () => {
    const ids = pendingDelete;
    setPendingDelete(null);
    try {
      const deleted = await deleteByIds(ids);
      showToast(`${deleted}건을 삭제했습니다.`, 'success');
    } catch (e) {
      showToast(e.message, 'error');
    }
  };

  return (
    <div className={styles.page}>
      <div className={styles.headerRow}>
        <h2 className={styles.heading}>연락처 관리</h2>
        <input
          className={`form-input ${styles.search}`}
          placeholder="이름·이메일 검색"
          value={qInput}
          onChange={(e) => setQInput(e.target.value)}
          aria-label="이름 또는 이메일 검색"
        />
      </div>

      <ContactAddForm onAdd={onAdd} />

      <DepartmentTabs departments={departments} active={activeDept} onSelect={onSelectDept} />

      {selectedIds.size > 0 && (
        <SelectionBar
          count={selectedIds.size}
          onCopy={onCopyEmails}
          onClear={() => setSelectedIds(new Set())}
          onDelete={() => setPendingDelete([...selectedIds])}
        />
      )}

      <ContactTable
        contacts={visible}
        loading={loading}
        selectedIds={selectedIds}
        setSelectedIds={setSelectedIds}
        visibleIds={visibleIds}
        onUpdate={onUpdate}
        onRequestDelete={(id) => setPendingDelete([id])}
      />

      {pendingDelete && (
        <ConfirmDialog
          message={`선택한 ${pendingDelete.length}건의 연락처를 삭제하시겠습니까?`}
          confirmLabel="삭제"
          onConfirm={onConfirmDelete}
          onCancel={() => setPendingDelete(null)}
        />
      )}
    </div>
  );
}
