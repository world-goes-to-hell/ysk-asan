import { describe, it, expect } from 'vitest';

import {
  toggle,
  selectVisible,
  deselectVisible,
  allVisibleSelected,
  someVisibleSelected,
} from '../utils/selection';

describe('toggle', () => {
  it('없는 id 는 추가한다', () => {
    expect([...toggle(new Set([1]), 2)]).toEqual([1, 2]);
  });
  it('있는 id 는 제거한다', () => {
    expect([...toggle(new Set([1, 2]), 2)]).toEqual([1]);
  });
  it('원본 Set 을 변경하지 않는다(불변)', () => {
    const orig = new Set([1]);
    toggle(orig, 2);
    expect([...orig]).toEqual([1]);
  });
});

describe('selectVisible', () => {
  it('보이는 id 를 합집합으로 추가한다', () => {
    expect([...selectVisible(new Set([1]), [2, 3])].sort()).toEqual([1, 2, 3]);
  });
  it('중복은 합쳐진다', () => {
    expect([...selectVisible(new Set([1, 2]), [2, 3])].sort()).toEqual([1, 2, 3]);
  });
});

describe('deselectVisible', () => {
  it('보이는 id 만 제거하고 다른 선택은 유지한다', () => {
    // 99 는 다른 부서 선택 → 보존되어야 함
    expect([...deselectVisible(new Set([1, 2, 99]), [1, 2])]).toEqual([99]);
  });
});

describe('allVisibleSelected', () => {
  it('보이는 행이 모두 선택되면 true', () => {
    expect(allVisibleSelected(new Set([1, 2, 99]), [1, 2])).toBe(true);
  });
  it('일부만 선택되면 false', () => {
    expect(allVisibleSelected(new Set([1]), [1, 2])).toBe(false);
  });
  it('빈 목록이면 false', () => {
    expect(allVisibleSelected(new Set([1]), [])).toBe(false);
  });
});

describe('someVisibleSelected', () => {
  it('일부만 선택되면 true(indeterminate)', () => {
    expect(someVisibleSelected(new Set([1]), [1, 2])).toBe(true);
  });
  it('전부 선택이면 false(allVisible 과 배타)', () => {
    expect(someVisibleSelected(new Set([1, 2]), [1, 2])).toBe(false);
  });
  it('아무것도 선택 안 하면 false', () => {
    expect(someVisibleSelected(new Set(), [1, 2])).toBe(false);
  });
});
