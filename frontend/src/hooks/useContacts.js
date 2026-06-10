import { useCallback, useEffect, useState } from 'react';

import contactsAPI from '../api/contacts';
import { useToast } from './useToast';

/**
 * 연락처 목록·부서·선택 상태와 CRUD 를 관리한다.
 * - 변이 후에는 목록+부서를 함께 refetch(낙관적 업데이트 X) → 부서 탭 자동 등장/소멸이 자연히 반영.
 * - contactsById 캐시는 부서를 가로지른 선택의 이메일 해석에 쓰인다.
 * @param q 디바운스된 검색어(상위에서 전달)
 */
export function useContacts(q) {
  const [activeDept, setActiveDept] = useState(null); // null = 전체
  const [visible, setVisible] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [contactsById, setContactsById] = useState(() => new Map());
  const [selectedIds, setSelectedIds] = useState(() => new Set());
  const [loading, setLoading] = useState(false);
  const showToast = useToast();

  const load = useCallback(
    async (dept, query) => {
      setLoading(true);
      try {
        const [rows, depts] = await Promise.all([
          contactsAPI.list(dept, query),
          contactsAPI.departments(),
        ]);
        setDepartments(depts);
        // 활성 부서가 사라졌으면(마지막 연락처 삭제 등) 전체로 폴백 → effect 가 재조회.
        if (dept && !depts.includes(dept)) {
          setActiveDept(null);
          return;
        }
        setVisible(rows);
        setContactsById((prev) => {
          const next = new Map(prev);
          rows.forEach((c) => next.set(c.id, c));
          return next;
        });
      } catch (e) {
        showToast(e.message, 'error');
      } finally {
        setLoading(false);
      }
    },
    [showToast]
  );

  useEffect(() => {
    load(activeDept, q);
  }, [activeDept, q, load]);

  const addContact = useCallback(
    async (data) => {
      await contactsAPI.create(data);
      await load(activeDept, q);
    },
    [activeDept, q, load]
  );

  const updateContact = useCallback(
    async (id, data) => {
      await contactsAPI.update(id, data);
      await load(activeDept, q);
    },
    [activeDept, q, load]
  );

  const deleteByIds = useCallback(
    async (ids) => {
      if (!ids.length) return 0;
      const res = await contactsAPI.remove(ids);
      // 삭제한 id 만 선택/캐시에서 제거(다른 선택은 유지).
      setSelectedIds((prev) => {
        const next = new Set(prev);
        ids.forEach((id) => next.delete(id));
        return next;
      });
      setContactsById((prev) => {
        const next = new Map(prev);
        ids.forEach((id) => next.delete(id));
        return next;
      });
      await load(activeDept, q);
      return res?.deleted ?? 0;
    },
    [activeDept, q, load]
  );

  return {
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
  };
}
