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
  // 직인 업로드(공개, 비밀번호 동봉)
  attachSeal: (token, password, file) => {
    const form = new FormData();
    form.append('password', password);
    form.append('file', file);
    return apiFetch(`/api/documents/${token}/seal`, { method: 'POST', body: form });
  },
};

export default documentsAPI;
