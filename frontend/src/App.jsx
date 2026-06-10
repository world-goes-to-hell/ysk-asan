import { Navigate, Outlet, Route, Routes } from 'react-router-dom';

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

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<AuthPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="contacts" element={<ContactsPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
