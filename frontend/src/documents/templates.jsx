import styles from '../styles/documents.module.css';

/**
 * 공문 양식 레지스트리(하드코딩). 양식마다 입력 항목이 달라 여기에 직접 정의한다.
 * - fields: 작성 폼 자동 생성 메타({key, label, multiline?, placeholder?})
 * - sample: 갤러리 썸네일용 예시 값
 * - View: 공문 렌더 컴포넌트 — 썸네일/작성 미리보기/공개 뷰어가 모두 공유한다.
 *   props: { fields, seal } — seal = { base64, contentType } | null. (인) 영역에 오버레이.
 *
 * 새 양식 추가 = 이 배열에 항목 1개 추가(백엔드 OfficialDocumentService.KNOWN_TEMPLATES 동기화).
 */

// 서버도 화이트리스트 검증하지만, data URI 에 들어가는 값이라 프론트에서 한 번 더 방어.
const ALLOWED_SEAL_TYPES = ['image/png', 'image/jpeg'];

/** (인) 표기 + 업로드된 직인 오버레이. */
function SealArea({ seal }) {
  const contentType =
    seal && ALLOWED_SEAL_TYPES.includes(seal.contentType) ? seal.contentType : 'image/png';
  return (
    <span className={styles.sealArea}>
      (인)
      {seal && (
        <img
          className={styles.sealImg}
          src={`data:${contentType};base64,${seal.base64}`}
          alt="직인"
        />
      )}
    </span>
  );
}

function GeneralOfficialView({ fields = {}, seal = null }) {
  return (
    <div className={styles.sheet}>
      <div className={styles.orgHeader}>재단법인 아산사회복지재단</div>
      <div className={styles.orgSub}>OOO병원</div>
      <hr className={styles.rule} />
      <table className={styles.metaTable}>
        <tbody>
          <tr>
            <th>문서번호</th>
            <td>{fields.docNumber}</td>
            <th>시행일자</th>
            <td>{fields.date}</td>
          </tr>
          <tr>
            <th>수신</th>
            <td colSpan={3}>{fields.receiver}</td>
          </tr>
          <tr>
            <th>참조</th>
            <td colSpan={3}>{fields.reference}</td>
          </tr>
        </tbody>
      </table>
      <p className={styles.docTitle}>제목: {fields.title}</p>
      <p className={styles.body}>{fields.body}</p>
      <p className={styles.closing}>끝.</p>
      <div className={styles.signer}>
        {fields.signer} <SealArea seal={seal} />
      </div>
    </div>
  );
}

function EmploymentCertView({ fields = {}, seal = null }) {
  return (
    <div className={styles.sheet}>
      <h1 className={styles.certTitle}>재 직 증 명 서</h1>
      <table className={styles.certTable}>
        <tbody>
          <tr><th>성명</th><td>{fields.name}</td></tr>
          <tr><th>부서</th><td>{fields.department}</td></tr>
          <tr><th>직위</th><td>{fields.position}</td></tr>
          <tr><th>재직기간</th><td>{fields.period}</td></tr>
          <tr><th>용도</th><td>{fields.purpose}</td></tr>
        </tbody>
      </table>
      <p className={styles.certStatement}>위와 같이 재직하고 있음을 증명합니다.</p>
      <p className={styles.certDate}>{fields.issueDate}</p>
      <div className={styles.signer}>
        {fields.signer} <SealArea seal={seal} />
      </div>
    </div>
  );
}

export const TEMPLATES = [
  {
    id: 'general-official',
    title: '일반 공문',
    summary: '수신처에 발송하는 표준 공문 양식',
    fields: [
      { key: 'docNumber', label: '문서번호', placeholder: '아산-2026-001' },
      { key: 'date', label: '시행일자', placeholder: '2026. 6. 10.' },
      { key: 'receiver', label: '수신', placeholder: '수신처 (예: OO기관장)' },
      { key: 'reference', label: '참조', placeholder: '담당 부서/담당자' },
      { key: 'title', label: '제목', placeholder: '공문 제목' },
      { key: 'body', label: '본문', multiline: true, placeholder: '1. 귀 기관의 무궁한 발전을 기원합니다.\n2. ...' },
      { key: 'signer', label: '발신명의', placeholder: 'OOO병원장' },
    ],
    sample: {
      docNumber: '아산-2026-001',
      date: '2026. 6. 10.',
      receiver: '수신처 각위',
      reference: '행정팀',
      title: '업무 협조 요청',
      body: '1. 귀 기관의 무궁한 발전을 기원합니다.\n2. 아래와 같이 협조를 요청하오니 조치하여 주시기 바랍니다.',
      signer: 'OOO병원장',
    },
    View: GeneralOfficialView,
  },
  {
    id: 'employment-cert',
    title: '재직증명서',
    summary: '재직 사실 확인용 증명서 양식',
    fields: [
      { key: 'name', label: '성명', placeholder: '홍길동' },
      { key: 'department', label: '부서', placeholder: '영상의학과' },
      { key: 'position', label: '직위', placeholder: '주임' },
      { key: 'period', label: '재직기간', placeholder: '2020. 1. 1. ~ 현재' },
      { key: 'purpose', label: '용도', placeholder: '관공서 제출용' },
      { key: 'issueDate', label: '발급일', placeholder: '2026. 6. 10.' },
      { key: 'signer', label: '발급명의', placeholder: 'OOO병원장' },
    ],
    sample: {
      name: '홍길동',
      department: '영상의학과',
      position: '주임',
      period: '2020. 1. 1. ~ 현재',
      purpose: '관공서 제출용',
      issueDate: '2026. 6. 10.',
      signer: 'OOO병원장',
    },
    View: EmploymentCertView,
  },
];

export function findTemplate(id) {
  return TEMPLATES.find((t) => t.id === id) || null;
}
