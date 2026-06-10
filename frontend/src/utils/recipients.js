/**
 * 선택된 연락처의 이메일을 ';' 로 결합한다(메일 수신처용).
 * 현재 화면에 없는(다른 부서) 선택도 캐시(contactsById)에서 해석하므로 부서를 가로질러 모은 선택도 복사된다.
 * 캐시에 없거나 이메일이 없는 id 는 건너뛴다.
 */
export function buildRecipients(contactsById, selectedIds) {
  return [...selectedIds]
    .map((id) => contactsById.get(id)?.email)
    .filter(Boolean)
    .join(';');
}
