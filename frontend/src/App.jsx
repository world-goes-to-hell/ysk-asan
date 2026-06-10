import { Navigate, Outlet, Route, Routes } from 'react-router-dom';

import AuthPage from './components/auth/AuthPage';
import { useAuth } from './contexts/AuthContext';

function RequireAuth() {
  const { currentUser, loading } = useAuth();
  if (loading) return null; // 세션 확인 중 로그인 화면 깜빡임 방지
  if (!currentUser) return <Navigate to="/login" replace />;
  return <Outlet />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<AuthPage />} />
      <Route element={<RequireAuth />}>
        {/* 레이아웃/대시보드/연락처는 후속 Task 에서 채운다. */}
        <Route
          path="/"
          element={<div style={{ padding: 'var(--space-6)' }}>로그인되었습니다. (레이아웃은 다음 단계)</div>}
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
