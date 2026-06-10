import { describe, it, expect } from 'vitest';

import { departmentSuggestions, emailSuggestions } from '../utils/suggestions';

describe('departmentSuggestions', () => {
  const depts = ['영업팀', '개발팀', '인사팀'];

  it('부분 문자열을 포함하는 부서를 추천한다', () => {
    expect(departmentSuggestions('영', depts)).toEqual(['영업팀']);
  });

  it('빈 입력이면 전체 부서를 반환한다', () => {
    expect(departmentSuggestions('', depts)).toEqual(depts);
  });

  it('정확히 일치하는 값은 제외한다(이미 다 입력함)', () => {
    expect(departmentSuggestions('영업팀', depts)).toEqual([]);
  });

  it('중복 부서는 한 번만', () => {
    expect(departmentSuggestions('', ['영업팀', '영업팀'])).toEqual(['영업팀']);
  });
});

describe('emailSuggestions', () => {
  const emails = ['hong@asan.kr', 'kim@amc.seoul.kr'];

  it('@ 없으면 기존 이메일 prefix 매칭', () => {
    expect(emailSuggestions('hong', emails)).toEqual(['hong@asan.kr']);
  });

  it('@ 직후 빈 도메인은 기존+흔한 도메인 후보', () => {
    const r = emailSuggestions('lee@', emails);
    expect(r).toContain('lee@asan.kr');
    expect(r).toContain('lee@gmail.com');
  });

  it('도메인 prefix 매칭', () => {
    expect(emailSuggestions('lee@as', emails)).toContain('lee@asan.kr');
  });

  it('. 입력 시 자주 쓰는 TLD 완성형 제안', () => {
    const r = emailSuggestions('lee@asan.', emails);
    expect(r).toContain('lee@asan.kr');
    expect(r).toContain('lee@asan.com');
    expect(r).toContain('lee@asan.go.kr');
  });

  it('부분 TLD는 필터된다(.k → .kr 만)', () => {
    const r = emailSuggestions('lee@asan.k', emails);
    expect(r).toContain('lee@asan.kr');
    expect(r).not.toContain('lee@asan.com');
  });

  it('빈 입력이면 빈 배열', () => {
    expect(emailSuggestions('', emails)).toEqual([]);
  });
});
