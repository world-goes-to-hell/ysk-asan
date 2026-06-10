/**
 * 쿠키 문자열에서 Spring 의 XSRF-TOKEN 값을 추출한다.
 * 테스트 가능하도록 cookieString 을 인자로 받는다(기본값은 document.cookie).
 */
export function getCsrfToken(
  cookieString = typeof document !== 'undefined' ? document.cookie : ''
) {
  const m = cookieString.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return m ? decodeURIComponent(m[1]) : null;
}
