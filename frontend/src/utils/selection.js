// 선택 상태(Set<id>)의 불변 연산. 부서/검색으로 화면이 바뀌어도 선택은 누적 유지되며
// (이메일 수신처 모으기), 마스터 체크박스는 "현재 보이는 행"만 대상으로 한다.

/** id 를 토글한 새 Set 을 반환한다(원본 불변). */
export function toggle(set, id) {
  const next = new Set(set);
  if (next.has(id)) next.delete(id);
  else next.add(id);
  return next;
}

/** 보이는 행 전체를 선택에 추가(union). */
export function selectVisible(set, visibleIds) {
  return new Set([...set, ...visibleIds]);
}

/** 보이는 행만 선택에서 제거(difference) — 다른 부서의 선택은 보존. */
export function deselectVisible(set, visibleIds) {
  const drop = new Set(visibleIds);
  return new Set([...set].filter((id) => !drop.has(id)));
}

/** 보이는 행이 모두 선택됐는가(마스터 체크박스 checked). 빈 목록은 false. */
export function allVisibleSelected(set, visibleIds) {
  return visibleIds.length > 0 && visibleIds.every((id) => set.has(id));
}

/** 일부만 선택됐는가(마스터 체크박스 indeterminate). */
export function someVisibleSelected(set, visibleIds) {
  return visibleIds.some((id) => set.has(id)) && !allVisibleSelected(set, visibleIds);
}
