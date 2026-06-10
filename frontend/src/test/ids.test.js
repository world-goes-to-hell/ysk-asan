import { describe, it, expect } from 'vitest';

import { buildIdsQuery } from '../utils/ids';

describe('buildIdsQuery', () => {
  it('repeated 파라미터로 직렬화한다', () => {
    expect(buildIdsQuery([1, 2, 3])).toBe('ids=1&ids=2&ids=3');
  });

  it('단일 id', () => {
    expect(buildIdsQuery([7])).toBe('ids=7');
  });

  it('빈 배열은 빈 문자열', () => {
    expect(buildIdsQuery([])).toBe('');
  });
});
