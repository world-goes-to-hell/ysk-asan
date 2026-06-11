import { apiFetch } from './client';

const documentsAPI = {
  // 발급(로그인 전용) → { token }
  issue: (templateId, fields, password) =>
    apiFetch('/api/documents', {
      method: 'POST',
      body: JSON.stringify({ templateId, fields, password }),
    }),
  // 공개 열람(토큰+비밀번호) → { templateId, fields, sealImageBase64?, sealContentType? }
  view: (token, password) =>
    apiFetch(`/api/documents/${token}/view`, {
      method: 'POST',
      body: JSON.stringify({ password }),
    }),
  // 직인 업로드(공개, 비밀번호 동봉) — 재업로드 시 교체
  attachSeal: (token, password, file) => {
    const form = new FormData();
    form.append('password', password);
    form.append('file', file);
    return apiFetch(`/api/documents/${token}/seal`, { method: 'POST', body: form });
  },
  // 발급자 전용(로그인)
  mine: () => apiFetch('/api/documents/mine'),
  issuerView: (token) => apiFetch(`/api/documents/${token}/issuer-view`),
  // 입력값 자동완성 이력 — {필드키: [값(최근 사용순)]}
  fieldHistory: (templateId) =>
    apiFetch(`/api/documents/field-history?templateId=${encodeURIComponent(templateId)}`),
};

export default documentsAPI;
