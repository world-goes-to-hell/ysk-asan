import { describe, it, expect } from 'vitest';

import { getCsrfToken } from '../utils/csrf';

describe('getCsrfToken', () => {
  it('단일 쿠키에서 토큰을 추출한다', () => {
    expect(getCsrfToken('XSRF-TOKEN=abc123')).toBe('abc123');
  });

  it('여러 쿠키 중에서 XSRF-TOKEN 만 추출한다', () => {
    expect(getCsrfToken('JSESSIONID=xxx; XSRF-TOKEN=tok-456; foo=bar')).toBe('tok-456');
  });

  it('URL 인코딩된 값을 디코딩한다', () => {
    expect(getCsrfToken('XSRF-TOKEN=a%2Bb%3D')).toBe('a+b=');
  });

  it('토큰이 없으면 null 을 반환한다', () => {
    expect(getCsrfToken('JSESSIONID=xxx')).toBeNull();
  });

  it('빈 문자열이면 null 을 반환한다', () => {
    expect(getCsrfToken('')).toBeNull();
  });

  it('유사 이름 쿠키(X-XSRF-TOKEN)에 오매칭하지 않는다', () => {
    expect(getCsrfToken('MY-XSRF-TOKEN=nope')).toBeNull();
  });
});
