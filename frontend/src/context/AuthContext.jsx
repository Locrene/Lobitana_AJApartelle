import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { authService } from '../services/authService';

// Auth state lives only here. The user object comes from the server (/auth/me), never from local storage.
const AuthContext = createContext(null);

export const DASHBOARD_BY_ROLE = {
  GUEST: '/guest',
  STAFF: '/staff',
  OWNER: '/owner',
};

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    try {
      setUser(await authService.me());
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { refresh(); }, [refresh]);

  const login = async (email, password) => {
    const u = await authService.login(email, password);
    setUser(u);
    return u;
  };

  const logout = async () => {
    try { await authService.logout(); } finally { setUser(null); }
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
