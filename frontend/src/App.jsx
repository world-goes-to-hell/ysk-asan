import { Navigate, Outlet, Route, Routes } from 'react-router-dom';

import AdminUsersPage from './components/admin/AdminUsersPage';
import AuthPage from './components/auth/AuthPage';
import ContactsPage from './components/contacts/ContactsPage';
import AppLayout from './components/layout/AppLayout';
import DashboardPage from './components/dashboard/DashboardPage';
import { useAuth } from './contexts/AuthContext';

function RequireAuth() {
  const { currentUser, loading } = useAuth();
  if (loading) return null; // 세션 확인 중 로그인 화면 깜빡임 방지
  if (!currentUser) return <Navigate to="/login" replace />;
  return <Outlet />;
}

// 관리자 전용 라우트 가드(백엔드 hasRole 과 이중 방어 — UI 접근 차단용).
function RequireAdmin() {
  const { currentUser } = useAuth();
  if (currentUser?.role !== 'ADMIN') return <Navigate to="/" replace />;
  return <Outlet />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<AuthPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="contacts" element={<ContactsPage />} />
          <Route element={<RequireAdmin />}>
            <Route path="admin/users" element={<AdminUsersPage />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
