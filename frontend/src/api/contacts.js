import { apiFetch } from './client';
import { buildIdsQuery } from '../utils/ids';

const contactsAPI = {
  list: (department, q) => {
    const params = new URLSearchParams();
    if (department) params.set('department', department);
    if (q) params.set('q', q);
    const qs = params.toString();
    return apiFetch('/api/contacts' + (qs ? '?' + qs : ''));
  },
  departments: () => apiFetch('/api/contacts/departments'),
  create: (data) =>
    apiFetch('/api/contacts', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) =>
    apiFetch('/api/contacts/' + id, { method: 'PUT', body: JSON.stringify(data) }),
  remove: (ids) =>
    apiFetch('/api/contacts?' + buildIdsQuery(ids), { method: 'DELETE' }),
};

export default contactsAPI;
