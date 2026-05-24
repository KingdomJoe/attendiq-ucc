import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { api, type AuthResponse, type MeResponse, type UserRole } from '../api/client';

interface AuthState {
  token: string | null;
  role: UserRole | null;
  user: MeResponse | null;
  loading: boolean;
  login: (identifier: string, password: string, role: UserRole) => Promise<void>;
  registerStudent: (payload: Record<string, string>) => Promise<void>;
  registerLecturer: (payload: Record<string, string>) => Promise<void>;
  logout: () => void;
  refreshMe: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

function persistAuth(res: AuthResponse) {
  localStorage.setItem('token', res.token);
  localStorage.setItem('role', res.role);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('token'));
  const [role, setRole] = useState<UserRole | null>(
    () => (localStorage.getItem('role') as UserRole | null) ?? null
  );
  const [user, setUser] = useState<MeResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const refreshMe = useCallback(async () => {
    if (!localStorage.getItem('token')) {
      setUser(null);
      return;
    }
    const { data } = await api.get<MeResponse>('/auth/me');
    setUser(data);
    setRole(data.role);
  }, []);

  useEffect(() => {
    (async () => {
      try {
        if (token) await refreshMe();
      } catch {
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        setToken(null);
        setRole(null);
      } finally {
        setLoading(false);
      }
    })();
  }, [token, refreshMe]);

  const login = useCallback(async (identifier: string, password: string, r: UserRole) => {
    const { data } = await api.post<AuthResponse>('/auth/login', {
      identifier,
      password,
      role: r,
    });
    persistAuth(data);
    setToken(data.token);
    setRole(data.role);
    const me = await api.get<MeResponse>('/auth/me');
    setUser(me.data);
  }, []);

  const registerStudent = useCallback(async (payload: Record<string, string>) => {
    const { data } = await api.post<AuthResponse>('/auth/student/register', payload);
    persistAuth(data);
    setToken(data.token);
    setRole(data.role);
    const me = await api.get<MeResponse>('/auth/me');
    setUser(me.data);
  }, []);

  const registerLecturer = useCallback(async (payload: Record<string, string>) => {
    const { data } = await api.post<AuthResponse>('/auth/lecturer/register', payload);
    persistAuth(data);
    setToken(data.token);
    setRole(data.role);
    const me = await api.get<MeResponse>('/auth/me');
    setUser(me.data);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    setToken(null);
    setRole(null);
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({
      token,
      role,
      user,
      loading,
      login,
      registerStudent,
      registerLecturer,
      logout,
      refreshMe,
    }),
    [token, role, user, loading, login, registerStudent, registerLecturer, logout, refreshMe]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
