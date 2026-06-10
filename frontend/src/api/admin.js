import { apiFetch } from './client';

// 회원 관리 API — ADMIN 전용(백엔드 hasRole 강제, 비관리자 403).
const adminAPI = {
  listUsers: () => apiFetch('/api/admin/users'),
  approve: (id) => apiFetch(`/api/admin/users/${id}/approve`, { method: 'POST' }),
  unlock: (id) => apiFetch(`/api/admin/users/${id}/unlock`, { method: 'POST' }),
  changeRole: (id, role) =>
    apiFetch(`/api/admin/users/${id}/role`, {
      method: 'PATCH',
      body: JSON.stringify({ role }),
    }),
};

export default adminAPI;
