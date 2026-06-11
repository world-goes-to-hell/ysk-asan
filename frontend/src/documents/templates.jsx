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

/**
 * 선금 청구 공문 — 실제 원내 HWP 양식(학술연구용역 과제 선금 청구) 구조 기반.
 * 인사말·"- 아 래 -"·발신명의(서울아산병원장)는 양식 고정 문구, 나머지는 입력.
 */
function AdvancePaymentView({ fields = {}, seal = null }) {
  return (
    <div className={styles.sheet}>
      <div className={styles.orgHeader}>서 울 아 산 병 원</div>
      <div className={styles.apContact}>
        05505 서울특별시 송파구 올림픽로43길 88 TEL{fields.tel} FAX{fields.fax}
        {' '}담당자 : {fields.manager}
      </div>
      <hr className={styles.rule} />
      <table className={styles.metaTable}>
        <tbody>
          <tr><th>문서번호</th><td>{fields.docNumber}</td></tr>
          <tr><th>발송일자</th><td>{fields.sendDate}</td></tr>
          <tr><th>수신</th><td>{fields.receiver}</td></tr>
          <tr><th>참조</th><td>{fields.reference}</td></tr>
          <tr><th>제목</th><td>{fields.title}</td></tr>
        </tbody>
      </table>

      <p className={styles.body} style={{ minHeight: 'auto', marginTop: 26 }}>
        1. 귀 기관의 무궁한 번영을 기원합니다.{'\n'}
        2. 학술연구용역과제 계약 체결관련 선금 요청 서류를 별첨과 같이 제출하오니
        협조하여 주시기 바랍니다.
      </p>

      <p className={styles.apBelow}>- 아&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;래 -</p>

      <table className={styles.apTable}>
        <thead>
          <tr>
            <th>연구책임자</th>
            <th>연구과제명</th>
            <th>당해 연구비</th>
            <th>당해 연구기간</th>
            <th>비고</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>{fields.pi}</td>
            <td>{fields.projectName}</td>
            <td>{fields.budget}</td>
            <td>{fields.period}</td>
            <td>{fields.note}</td>
          </tr>
        </tbody>
      </table>

      <p className={styles.apAttachment}>첨&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;부 : {fields.attachment}</p>

      <div className={styles.signer}>
        서 울 아 산 병 원 장 <SealArea seal={seal} />
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
    id: 'advance-payment',
    title: '선금 청구 공문',
    summary: '학술연구용역 과제 선금 청구 서류 제출 공문',
    fields: [
      { key: 'manager', label: '담당자', placeholder: '홍길동 hong@amc.seoul.kr' },
      { key: 'tel', label: 'TEL', placeholder: '(02)3010-0000' },
      { key: 'fax', label: 'FAX', placeholder: '(02)2045-0000' },
      { key: 'docNumber', label: '문서번호', placeholder: '서울아산 (연구기획팀) 제 2026 – 000000 호' },
      { key: 'sendDate', label: '발송일자', placeholder: '2026년 06월 11일' },
      { key: 'receiver', label: '수신', placeholder: '질병관리청 OOO연구원 OOO과' },
      { key: 'reference', label: '참조', placeholder: '(없으면 비워두세요)' },
      { key: 'title', label: '제목', placeholder: '학술연구용역 과제 선금 청구를 위한 서류 제출의 건(홍길동)' },
      { key: 'pi', label: '연구책임자', placeholder: '홍길동 (서울아산병원)' },
      { key: 'projectName', label: '연구과제명', placeholder: '[학술] OOO 연구' },
      { key: 'budget', label: '당해 연구비', placeholder: '100,000,000원 (부가세 포함)' },
      { key: 'period', label: '당해 연구기간', placeholder: '2026. 01. 01. ~ 2026. 12. 31.' },
      { key: 'note', label: '비고', placeholder: '선금 70% 70,000,000원 (부가세 포함)' },
      { key: 'attachment', label: '첨부', placeholder: '1. 학술연구용역 과제 선금 관련서류 일체. 끝.' },
    ],
    sample: {
      manager: '홍길동 hong@amc.seoul.kr',
      tel: '(02)3010-0000',
      fax: '(02)2045-0000',
      docNumber: '서울아산 (연구기획팀) 제 2026 – 000000 호',
      sendDate: '2026년 06월 11일',
      receiver: '질병관리청 OOO연구원 OOO과',
      reference: '',
      title: '학술연구용역 과제 선금 청구를 위한 서류 제출의 건',
      pi: '홍길동 (서울아산병원)',
      projectName: '[학술] OOO 연구과제',
      budget: '100,000,000원 (부가세 포함)',
      period: '2026. 01. 01. ~ 2026. 12. 31.',
      note: '선금 70% 70,000,000원',
      attachment: '1. 학술연구용역 과제 선금 관련서류 일체. 끝.',
    },
    View: AdvancePaymentView,
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
