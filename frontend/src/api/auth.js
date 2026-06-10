import { apiFetch } from './client';

const authAPI = {
  me: () => apiFetch('/api/auth/me'),
  login: (username, password, rememberMe = false) =>
    apiFetch('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password, rememberMe }),
    }),
  register: (username, password) =>
    apiFetch('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),
  logout: () => apiFetch('/api/auth/logout', { method: 'POST' }),
};

export default authAPI;
