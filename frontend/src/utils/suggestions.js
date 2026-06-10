// 입력 자동완성 추천 로직(순수 함수). 부서/이메일 + 이메일 TLD 제안.

const TLDS = ['.com', '.kr', '.co.kr', '.go.kr', '.net', '.org', '.ac.kr', '.or.kr'];
const COMMON_DOMAINS = ['gmail.com', 'naver.com', 'daum.net', 'kakao.com', 'nate.com'];

const uniq = (arr) => [...new Set(arr)];

/** 기존 부서명 중 입력값을 포함하는 후보(정확히 같은 값은 제외). 빈 입력이면 전체. */
export function departmentSuggestions(value, departments = [], limit = 7) {
  const q = (value || '').trim().toLowerCase();
  const pool = uniq(departments.filter(Boolean));
  if (!q) return pool.slice(0, limit);
  return pool
    .filter((d) => d.toLowerCase().includes(q) && d.toLowerCase() !== q)
    .slice(0, limit);
}

/**
 * 이메일 자동완성:
 * - '@' 없으면: 기존 이메일 중 입력값으로 시작하는 후보.
 * - '@' 있으면: 도메인 부분을 기존 도메인 + 흔한 도메인으로 보완하고,
 *   도메인에 '.'이 들어오면 자주 쓰는 TLD(.kr/.com/.go.kr 등) 완성형을 제안.
 */
export function emailSuggestions(value, knownEmails = [], limit = 7) {
  const v = (value || '').trim();
  if (!v) return [];
  const emails = uniq(knownEmails.filter(Boolean));
  const at = v.indexOf('@');

  if (at === -1) {
    const lower = v.toLowerCase();
    return emails
      .filter((e) => e.toLowerCase().startsWith(lower) && e.toLowerCase() !== lower)
      .slice(0, limit);
  }

  const local = v.slice(0, at);
  const domain = v.slice(at + 1);
  const dlow = domain.toLowerCase();
  const knownDomains = uniq(emails.map((e) => e.split('@')[1]).filter(Boolean));
  const domainPool = uniq([...knownDomains, ...COMMON_DOMAINS]);
  const out = [];

  // 도메인 후보(기존 + 흔한). '@' 직후 빈 도메인이면 전체 제시.
  const matchedDomains = domain
    ? domainPool.filter((d) => d.toLowerCase().startsWith(dlow) && d.toLowerCase() !== dlow)
    : domainPool;
  matchedDomains.forEach((d) => out.push(`${local}@${d}`));

  // 도메인에 '.'이 있으면 TLD 완성형 제안(부분 TLD로 필터).
  const lastDot = domain.lastIndexOf('.');
  if (lastDot !== -1) {
    const base = domain.slice(0, lastDot);
    const tldPartial = domain.slice(lastDot).toLowerCase();
    TLDS.filter((t) => t.startsWith(tldPartial) && t !== tldPartial).forEach((t) => {
      const cand = `${local}@${base}${t}`;
      if (!out.some((o) => o.toLowerCase() === cand.toLowerCase())) out.push(cand);
    });
  }

  return uniq(out).slice(0, limit);
}
