/**
 * 일괄삭제용 쿼리스트링을 만든다.
 * 백엔드는 @RequestParam List<Long> ids 이므로 repeated 파라미터로 직렬화한다.
 * [1, 2, 3] -> "ids=1&ids=2&ids=3"
 */
export function buildIdsQuery(ids) {
  return ids.map((id) => 'ids=' + encodeURIComponent(id)).join('&');
}
