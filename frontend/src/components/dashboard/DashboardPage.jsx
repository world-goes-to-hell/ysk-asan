import { Link } from 'react-router-dom';

import styles from '../../styles/dashboard.module.css';

const FEATURES = [
  {
    title: '부서별 정리',
    desc: '입력한 부서로 탭이 자동 생성되어 조직 구조대로 연락처를 모아 봅니다.',
    color: 'var(--accent)',
    bg: 'var(--accent-soft)',
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
           strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12 2 2 7l10 5 10-5-10-5Z" />
        <path d="m2 17 10 5 10-5" />
        <path d="m2 12 10 5 10-5" />
      </svg>
    ),
  },
  {
    title: '빠른 검색',
    desc: '이름·이메일로 즉시 필터링하고, 부서를 가로질러 필요한 사람을 찾습니다.',
    color: 'var(--accent-2)',
    bg: 'var(--accent-2-soft)',
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
           strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="11" cy="11" r="7" />
        <path d="m21 21-4.3-4.3" />
      </svg>
    ),
  },
  {
    title: '이메일 일괄 복사',
    desc: '여러 명을 선택해 수신처 목록을 한 번에 복사, 메일 작성을 간편하게.',
    color: 'var(--accent)',
    bg: 'var(--accent-soft)',
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
           strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="3" y="5" width="18" height="14" rx="2" />
        <path d="m3 7 9 6 9-6" />
      </svg>
    ),
  },
];

export default function DashboardPage() {
  return (
    <div className={styles.page}>
      <section className={styles.hero}>
        {/* 서울아산병원 로고 모티프(블루·그린이 감싸는 곡선)를 추상화한 배경 — 로고 자체는 사용하지 않음 */}
        <svg
          className={styles.heroArt}
          viewBox="0 0 560 560"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          aria-hidden="true"
        >
          <defs>
            <linearGradient id="asanBlue" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0" stopColor="#1a8fc9" />
              <stop offset="1" stopColor="#005a90" />
            </linearGradient>
            <linearGradient id="asanGreen" x1="0" y1="1" x2="1" y2="0">
              <stop offset="0" stopColor="#8ccb4a" />
              <stop offset="1" stopColor="#4f9b23" />
            </linearGradient>
            <filter id="asanSoft" x="-30%" y="-30%" width="160%" height="160%">
              <feGaussianBlur stdDeviation="34" />
            </filter>
          </defs>
          <g filter="url(#asanSoft)" opacity="0.5">
            <circle cx="340" cy="200" r="150" fill="url(#asanBlue)" />
            <circle cx="250" cy="375" r="140" fill="url(#asanGreen)" />
          </g>
          <g transform="rotate(-18 280 280)">
            <path d="M 152 280 A 128 128 0 0 1 408 280" stroke="url(#asanBlue)"
                  strokeWidth="22" fill="none" strokeLinecap="round" opacity="0.92" />
            <path d="M 408 280 A 128 128 0 0 1 152 280" stroke="url(#asanGreen)"
                  strokeWidth="22" fill="none" strokeLinecap="round" opacity="0.92" />
          </g>
          <circle cx="280" cy="280" r="54" fill="#ffffff" opacity="0.6" />
        </svg>

        <div className={styles.heroContent}>
          <span className={styles.kicker}>ASAN · 수신처 관리</span>
          <h2 className={styles.heroTitle}>
            필요한 사람에게,<br />한 번에 닿도록
          </h2>
          <p className={styles.heroDesc}>
            부서별 연락처를 한곳에 모아 검색하고, 선택한 인원의 이메일을 한 번에 복사하세요.
          </p>
          <Link to="/contacts" className="btn btn-primary">
            연락처 관리 시작
          </Link>
        </div>
      </section>

      <div className={styles.features}>
        {FEATURES.map((f) => (
          <article key={f.title} className={styles.featureCard}>
            <span className={styles.featureIcon} style={{ color: f.color, background: f.bg }}>
              {f.icon}
            </span>
            <h3 className={styles.featureTitle}>{f.title}</h3>
            <p className={styles.featureDesc}>{f.desc}</p>
          </article>
        ))}
      </div>
    </div>
  );
}
