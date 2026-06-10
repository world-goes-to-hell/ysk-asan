import { Navigate, Outlet, Route, Routes } from 'react-router-dom';

import AuthPage from './components/auth/AuthPage';
import AppLayout from './components/layout/AppLayout';
import DashboardPage from './components/dashboard/DashboardPage';
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
        <Route element={<AppLayout />}>
          <Route index element={<DashboardPage />} />
          {/* 연락처 화면은 다음 Task 에서 ContactsPage 로 교체 */}
          <Route
            path="contacts"
            element={<div>연락처 관리 (다음 단계에서 구현)</div>}
          />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
