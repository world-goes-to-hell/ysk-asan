import { getCsrfToken } from '../utils/csrf';

/**
 * fetch 래퍼. 쿠키 동봉(credentials:'include'), 상태 변경 메서드에 X-XSRF-TOKEN 헤더 첨부,
 * 비-OK 응답은 서버의 {message}/{error} 로 Error 를 던진다.
 * /api/auth/* 가 아닌 경로에서 401 을 받으면 'session-expired' 이벤트를 발행해 AuthContext 가 처리한다.
 */
export async function apiFetch(url, options = {}) {
  const isFormData = options.body instanceof FormData;
  const headers = isFormData
    ? { ...(options.headers || {}) }
    : { 'Content-Type': 'application/json', ...(options.headers || {}) };

  const method = (options.method || 'GET').toUpperCase();
  if (method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS') {
    const token = getCsrfToken();
    if (token) headers['X-XSRF-TOKEN'] = token;
  }

  const response = await fetch(url, { ...options, headers, credentials: 'include' });

  if (!response.ok) {
    let message = `요청 실패: ${response.status}`;
    try {
      const body = await response.json();
      if (body && (body.message || body.error)) {
        message = body.message || body.error;
      }
    } catch (_) {
      /* 응답 바디 없음/비JSON — 기본 메시지 유지 */
    }

    if (response.status === 401 && !url.startsWith('/api/auth/')) {
      window.dispatchEvent(new CustomEvent('session-expired'));
    }

    const err = new Error(message);
    err.status = response.status;
    throw err;
  }

  const text = await response.text();
  return text ? JSON.parse(text) : null;
}
