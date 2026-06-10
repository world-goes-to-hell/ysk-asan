import { describe, it, expect } from 'vitest';

import { buildRecipients } from '../utils/recipients';

const cache = new Map([
  [1, { id: 1, email: 'a@ex.com' }],
  [2, { id: 2, email: 'b@ex.com' }],
  [3, { id: 3, email: 'c@ex.com' }],
]);

describe('buildRecipients', () => {
  it('선택된 이메일을 ; 로 결합한다', () => {
    expect(buildRecipients(cache, new Set([1, 2]))).toBe('a@ex.com;b@ex.com');
  });

  it('캐시에 없는 id 는 건너뛴다', () => {
    expect(buildRecipients(cache, new Set([1, 999]))).toBe('a@ex.com');
  });

  it('빈 선택이면 빈 문자열', () => {
    expect(buildRecipients(cache, new Set())).toBe('');
  });

  it('부서를 가로지른 선택도 캐시에서 모두 해석한다', () => {
    expect(buildRecipients(cache, new Set([3, 1])).split(';').sort()).toEqual([
      'a@ex.com',
      'c@ex.com',
    ]);
  });
});
