import { createContext, useCallback, useContext, useEffect, useState } from 'react';

import authAPI from '../api/auth';
import { useToast } from '../hooks/useToast';

const AuthContext = createContext(null);

export function useAuth() {
  return useContext(AuthContext);
}

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const showToast = useToast();

  // 마운트 시 현재 세션 확인. 미로그인(401)은 정상 흐름이므로 조용히 무시한다.
  useEffect(() => {
    authAPI
      .me()
      .then((user) => setCurrentUser(user))
      .catch(() => setCurrentUser(null))
      .finally(() => setLoading(false));
  }, []);

  // apiFetch 가 보내는 세션 만료(401) 신호를 받아 사용자 상태를 비운다.
  useEffect(() => {
    const onExpired = () => {
      setCurrentUser(null);
      showToast('세션이 만료되었습니다. 다시 로그인해 주세요.', 'error');
    };
    window.addEventListener('session-expired', onExpired);
    return () => window.removeEventListener('session-expired', onExpired);
  }, [showToast]);

  const login = useCallback(async (username, password) => {
    const user = await authAPI.login(username, password);
    setCurrentUser(user);
    return user;
  }, []);

  // 백엔드 register 는 세션을 만들지 않는다 → 가입 후 로그인 필요(자동 로그인 안 함).
  const register = useCallback((username, password) => {
    return authAPI.register(username, password);
  }, []);

  const logout = useCallback(async () => {
    try {
      await authAPI.logout();
    } catch (_) {
      /* 이미 만료된 세션 등은 무시하고 로컬 상태만 비운다 */
    }
    setCurrentUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ currentUser, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
